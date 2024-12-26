package org.onap.usecaseui.llmadaptation.service.impl;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.onap.usecaseui.llmadaptation.bean.KnowledgeBase;
import org.onap.usecaseui.llmadaptation.bean.ResultHeader;
import org.onap.usecaseui.llmadaptation.bean.ServiceResult;
import org.onap.usecaseui.llmadaptation.bean.bisheng.BiShengCreateDatasetResponse;
import org.onap.usecaseui.llmadaptation.bean.bisheng.ProcessFileResponse;
import org.onap.usecaseui.llmadaptation.constant.BiShengConstant;
import org.onap.usecaseui.llmadaptation.constant.CommonConstant;
import org.onap.usecaseui.llmadaptation.constant.ServerConstant;
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

import java.util.UUID;

import static org.springframework.http.MediaType.APPLICATION_JSON;

@Slf4j
@Service
public class BiShengDatasetServiceImpl implements BiShengDatasetService {

    @Autowired
    private WebClient webClient;

    @Autowired
    private DatasetMapper datasetMapper;

    @Autowired
    private ServerConstant serverConstant;

    @Override
    public Mono<ServiceResult> createDataset(Flux<FilePart> fileParts, String metaData) {
        KnowledgeBase knowledgeBase = JSONObject.parseObject(metaData, KnowledgeBase.class);
        knowledgeBase.setUpdateTime(TimeUtil.getNowTime());
        JSONObject createParam = new JSONObject();
        createParam.put("description", knowledgeBase.getKnowledgeBaseDescription());
        createParam.put("model", serverConstant.getBiShengModel());
        createParam.put("name", knowledgeBase.getKnowledgeBaseName());
        return webClient.post()
                .uri(serverConstant.getBiShengServer() + BiShengConstant.CREATE_DATASET_URL)
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
                    return fileParts.flatMap(filePart -> processFile(filePart, knowledgeBaseId))
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

    private Mono<Void> processFile(FilePart filePart, int knowledgeBaseId) {
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
                .uri(serverConstant.getBiShengServer() + BiShengConstant.UPLOAD_FILE_URL)
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
                            .uri(serverConstant.getBiShengServer() + BiShengConstant.PROCESS_FILE_URL)
                            .contentType(APPLICATION_JSON)
                            .header(CommonConstant.COOKIE, BiShengConstant.COOKIE_VALUE)
                            .bodyValue(processParam)
                            .retrieve()
                            .bodyToMono(ProcessFileResponse.class).flatMap(lastResponse -> {
                                if (lastResponse.getStatus_code() == 200) {
                                    String fileId = UUID.randomUUID().toString();
                                    datasetMapper.insertFileName(fileId, filename, String.valueOf(knowledgeBaseId));
                                }
                                return Mono.empty();
                            });
                });
    }

    @Override
    public Mono<ServiceResult> removeDataset(String knowledgeBaseId) {
        return webClient.delete()
                .uri(serverConstant.getBiShengServer() + BiShengConstant.DATASET_V2_URL + knowledgeBaseId)
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
    public Mono<ServiceResult> editDataset(KnowledgeBase knowledgeBase) {
        KnowledgeBase knowledgeBaseRecordById = datasetMapper.getKnowledgeBaseRecordById(knowledgeBase.getKnowledgeBaseId());
        if (knowledgeBaseRecordById == null) {
            return Mono.just(new ServiceResult(new ResultHeader(500, "dataset is not exist")));
        }
        JSONObject updateParam = new JSONObject();
        updateParam.put("knowledge_id", knowledgeBase.getKnowledgeBaseId());
        updateParam.put("name", knowledgeBase.getKnowledgeBaseName());
        updateParam.put("description", knowledgeBase.getKnowledgeBaseDescription());
        updateParam.put("model", serverConstant.getBiShengModel());

        return webClient.put()
                .uri(serverConstant.getBiShengServer() + BiShengConstant.DATASET_V2_URL)
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
}
