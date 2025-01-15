package org.onap.usecaseui.llmadaptation.service.impl;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.onap.usecaseui.llmadaptation.bean.*;
import org.onap.usecaseui.llmadaptation.bean.bisheng.BiShengCreateDatasetResponse;
import org.onap.usecaseui.llmadaptation.bean.bisheng.ProcessFileResponse;
import org.onap.usecaseui.llmadaptation.constant.BiShengConstant;
import org.onap.usecaseui.llmadaptation.constant.CommonConstant;
import org.onap.usecaseui.llmadaptation.mapper.DatasetMapper;
import org.onap.usecaseui.llmadaptation.service.BiShengDatasetService;
import org.onap.usecaseui.llmadaptation.util.TimeUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.ContentDisposition;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

import static org.springframework.http.MediaType.APPLICATION_JSON;

@Slf4j
@Service
public class BiShengDatasetServiceImpl implements BiShengDatasetService {

    @Autowired
    private WebClient webClient;

    @Autowired
    private DatasetMapper datasetMapper;


    @Override
    public Mono<ServiceResult> createDataset(Flux<FilePart> fileParts, String metaData, MaaSPlatform maaSPlatform) {

        KnowledgeBase knowledgeBase = JSONObject.parseObject(metaData, KnowledgeBase.class);
        knowledgeBase.setUpdateTime(TimeUtil.getNowTime());
        JSONObject createParam = new JSONObject();
        createParam.put("description", knowledgeBase.getKnowledgeBaseDescription());
        createParam.put("model", maaSPlatform.getVectorModel());
        createParam.put("name", knowledgeBase.getKnowledgeBaseName());
        return webClient.post()
                .uri(maaSPlatform.getServerIp() + BiShengConstant.CREATE_DATASET_URL)
                .contentType(APPLICATION_JSON)
                .header(CommonConstant.COOKIE, BiShengConstant.COOKIE_VALUE)
                .bodyValue(createParam)
                .retrieve()
                .bodyToMono(BiShengCreateDatasetResponse.class)
                .flatMap(response -> {
                    if (response.getStatus_code() != 200) {
                        return Mono.just(new ServiceResult(new ResultHeader(500, response.getStatus_message())));
                    }
                    int knowledgeBaseId = response.getData().getIntValue("id");
                    return fileParts.flatMap(filePart -> processFile(filePart, knowledgeBaseId, maaSPlatform.getServerIp()))
                            .then(Mono.defer(() -> {
                                knowledgeBase.setKnowledgeBaseId(String.valueOf(knowledgeBaseId));
                                datasetMapper.insertKnowledgeBaseRecord(knowledgeBase);
                                return Mono.just(new ServiceResult(new ResultHeader(200, "create success")));
                            })).onErrorResume(e -> {
                                log.error("Error occurred during file upload: {}", e.getMessage());
                                return Mono.just(new ServiceResult(new ResultHeader(500, "file upload failed")));
                            });
                }).onErrorResume(e -> {
                    log.error("Error occurred while creating dataset: {}", e.getMessage());
                    return Mono.just(new ServiceResult(new ResultHeader(500, "create failed")));
                });
    }

    private Mono<Void> processFile(FilePart filePart, int knowledgeBaseId, String serverIp) {
        String filename = filePart.filename();
        Flux<DataBuffer> content = filePart.content();
        MultipartBodyBuilder builder = new MultipartBodyBuilder();
        builder.asyncPart("file", content, DataBuffer.class)
                .headers(headers -> {
                    ContentDisposition contentDisposition = ContentDisposition
                            .builder("form-data")
                            .name("file")
                            .filename(filename)
                            .build();
                    headers.setContentDisposition(contentDisposition);
                    headers.setContentType(MediaType.TEXT_PLAIN);
                });
        return webClient.post()
                .uri(serverIp + BiShengConstant.UPLOAD_FILE_URL)
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .header(CommonConstant.COOKIE, BiShengConstant.COOKIE_VALUE)
                .body(BodyInserters.fromMultipartData(builder.build()))
                .retrieve()
                .bodyToMono(BiShengCreateDatasetResponse.class)
                .flatMap(response -> {
                    if (response.getStatus_code() != 200) {
                        log.error("response is {}", response);
                        return Mono.empty();
                    }
                    String filePath = response.getData().getString("file_path");
                    JSONObject processParam = new JSONObject();
                    processParam.put("knowledge_id", knowledgeBaseId);
                    JSONArray jsonArray = new JSONArray();
                    JSONObject jsonObject = new JSONObject();
                    jsonObject.put("file_path", filePath);
                    jsonArray.add(jsonObject);
                    processParam.put("file_list", jsonArray);
                    return webClient.post()
                            .uri(serverIp + BiShengConstant.PROCESS_FILE_URL)
                            .contentType(APPLICATION_JSON)
                            .header(CommonConstant.COOKIE, BiShengConstant.COOKIE_VALUE)
                            .bodyValue(processParam)
                            .retrieve()
                            .bodyToMono(ProcessFileResponse.class).flatMap(lastResponse -> {
                                if (lastResponse.getStatus_code() == 200) {
                                    JSONObject data = lastResponse.getData().get(0);
                                    int fileId = data.getIntValue("id");
                                    File file = new File(String.valueOf(fileId), filename);
                                    datasetMapper.insertFileName(List.of(file), String.valueOf(knowledgeBaseId));
                                }
                                return Mono.empty();
                            });
                });
    }

    @Override
    public Mono<ServiceResult> removeDataset(String knowledgeBaseId, String serverIp) {
        return webClient.delete()
                .uri(serverIp + BiShengConstant.DATASET_V2_URL + knowledgeBaseId)
                .header(CommonConstant.COOKIE, BiShengConstant.COOKIE_VALUE)
                .retrieve()
                .bodyToMono(BiShengCreateDatasetResponse.class)
                .flatMap(response -> {
                    if (response.getStatus_code() == 200) {
                        return Mono.fromRunnable(() -> {
                            try {
                                datasetMapper.deleteKnowledgeBaseByUuid(knowledgeBaseId);
                                datasetMapper.deleteFileById(knowledgeBaseId);
                            } catch (Exception dbException) {
                                throw new RuntimeException("Database operation failed", dbException);
                            }
                        }).then(Mono.just(new ServiceResult(new ResultHeader(200, "delete success"))));
                    } else {
                        return Mono.just(new ServiceResult(new ResultHeader(500, response.getStatus_message())));
                    }
                })
                .onErrorResume(e -> {
                    log.error("Error occurred while delete dataset: {}", e.getMessage());
                    return Mono.just(new ServiceResult(new ResultHeader(500, "delete failed")));
                });
    }

    @Override
    public Mono<ServiceResult> editDataset(KnowledgeBase knowledgeBase, MaaSPlatform maaSPlatform) {
        KnowledgeBase knowledgeBaseRecordById = datasetMapper.getKnowledgeBaseRecordById(knowledgeBase.getKnowledgeBaseId());
        if (knowledgeBaseRecordById == null) {
            return Mono.just(new ServiceResult(new ResultHeader(500, "dataset is not exist")));
        }
        JSONObject updateParam = new JSONObject();
        updateParam.put("knowledge_id", knowledgeBase.getKnowledgeBaseId());
        updateParam.put("name", knowledgeBase.getKnowledgeBaseName());
        updateParam.put("description", knowledgeBase.getKnowledgeBaseDescription());
        updateParam.put("model", maaSPlatform.getVectorModel());

        return webClient.put()
                .uri(maaSPlatform.getServerIp() + BiShengConstant.DATASET_V2_URL)
                .contentType(APPLICATION_JSON)
                .bodyValue(updateParam)
                .retrieve()
                .bodyToMono(BiShengCreateDatasetResponse.class)
                .flatMap(response -> {
                    if (response.getStatus_code() == 200) {
                        return Mono.fromRunnable(() -> {
                            knowledgeBase.setUpdateTime(TimeUtil.getNowTime());
                            datasetMapper.updateKnowledgeBase(knowledgeBase);
                        }).then(Mono.just(new ServiceResult(new ResultHeader(200, "update success"))));
                    } else {
                        return Mono.just(new ServiceResult(new ResultHeader(500, response.getStatus_message())));
                    }
                })
                .onErrorResume(e -> {
                    log.error("Error occurred while delete dataset: {}", e.getMessage());
                    return Mono.just(new ServiceResult(new ResultHeader(500, "update failed")));
                });
    }

    @Override
    public Mono<ServiceResult> uploadFiles(Flux<FilePart> fileParts, String knowledgeBaseId, String serverIp) {
        return fileParts.flatMap(filePart -> processFile(filePart, Integer.parseInt(knowledgeBaseId), serverIp))
                .then(Mono.just(new ServiceResult(new ResultHeader(200, "upload success"))))
                .onErrorResume(e -> {
                    log.error("Error occurred during file upload: {}", e.getMessage());
                    return Mono.just(new ServiceResult(new ResultHeader(500, "file upload failed")));
                });
    }

    @Override
    public Mono<ServiceResult> deleteFile(String fileId, String serverIp) {
        return webClient.delete()
                .uri(serverIp + BiShengConstant.DELETE_FILE_URL + fileId)
                .header(CommonConstant.COOKIE, BiShengConstant.COOKIE_VALUE)
                .retrieve()
                .bodyToMono(BiShengCreateDatasetResponse.class)
                .flatMap(response -> {
                    if (response.getStatus_code() == 200) {
                        return Mono.fromRunnable(() -> datasetMapper.deleteFileByFileId(fileId)).then(Mono.just(new ServiceResult(new ResultHeader(200, "delete file success"))));
                    } else {
                        return Mono.just(new ServiceResult(new ResultHeader(response.getStatus_code(), response.getStatus_message())));
                    }
                })
                .onErrorResume(e -> {
                    log.error("Error occurred while delete dataset: {}", e.getMessage());
                    return Mono.just(new ServiceResult(new ResultHeader(500, "delete file failed")));
                });
    }
}
