package org.onap.usecaseui.llmadaptation.service.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.onap.usecaseui.llmadaptation.bean.*;
import org.onap.usecaseui.llmadaptation.bean.fastgpt.dataset.CreateCollectionParam;
import org.onap.usecaseui.llmadaptation.bean.fastgpt.dataset.CreateDataSetParam;
import org.onap.usecaseui.llmadaptation.bean.fastgpt.dataset.CreateDataSetResponse;
import org.onap.usecaseui.llmadaptation.constant.CommonConstant;
import org.onap.usecaseui.llmadaptation.constant.FastGptConstant;
import org.onap.usecaseui.llmadaptation.mapper.DatasetMapper;
import org.onap.usecaseui.llmadaptation.service.FastGptDatasetService;
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
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.springframework.http.MediaType.APPLICATION_JSON;

@Slf4j
@Service
public class FastGptDatasetServiceImpl implements FastGptDatasetService {
    @Autowired
    private DatasetMapper datasetMapper;

    @Autowired
    private WebClient webClient;

    @Override
    public Mono<ServiceResult> createDataset(Flux<FilePart> fileParts, String metaData, MaaSPlatform maaSPlatform) {
        KnowledgeBase knowledgeBase = JSONObject.parseObject(metaData, KnowledgeBase.class);
        knowledgeBase.setUpdateTime(TimeUtil.getNowTime());
        CreateDataSetParam dataSetParam = new CreateDataSetParam();
        dataSetParam.setAgentModel(maaSPlatform.getVectorModel());
        dataSetParam.setType("dataset");
        dataSetParam.setIntro(knowledgeBase.getKnowledgeBaseDescription());
        dataSetParam.setName(knowledgeBase.getKnowledgeBaseName());
        return webClient.post()
                .uri(maaSPlatform.getServerIp() + FastGptConstant.CREATE_DATASET_URL)
                .contentType(APPLICATION_JSON)
                .header(CommonConstant.COOKIE, FastGptConstant.COOKIE_VALUE)
                .bodyValue(dataSetParam)
                .retrieve()
                .bodyToMono(CreateDataSetResponse.class)
                .flatMap(response -> {
                    if (response.getCode() == 200) {
                        String knowledgeBaseId = String.valueOf(response.getData());
                        return fileParts
                                .flatMap(filePart -> uploadFile(filePart, knowledgeBaseId, maaSPlatform.getServerIp()))
                                .then(Mono.defer(() -> {
                                    knowledgeBase.setKnowledgeBaseId(knowledgeBaseId);
                                    datasetMapper.insertKnowledgeBaseRecord(knowledgeBase);
                                    return handleFileId(knowledgeBaseId, maaSPlatform.getServerIp());
                                }))
                                .onErrorResume(e -> {
                                    log.error("Error occurred during file upload: {}", e.getMessage());
                                    return Mono.just(new ServiceResult(new ResultHeader(500, "file upload failed")));
                                });
                    } else {
                        return Mono.just(new ServiceResult(new ResultHeader(500, response.getMessage())));
                    }
                })
                .onErrorResume(e -> {
                    log.error("Error occurred while creating dataset: {}", e.getMessage());
                    return Mono.just(new ServiceResult(new ResultHeader(500, "create failed")));
                });
    }

    private Mono<ServiceResult> handleFileId(String knowledgeBaseId, String serverIp) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("datasetId", knowledgeBaseId);
        return webClient.post()
                .uri(serverIp + FastGptConstant.GET_COLLECTION_LIST_URL)
                .header(CommonConstant.COOKIE, FastGptConstant.COOKIE_VALUE)
                .bodyValue(jsonObject)
                .retrieve()
                .bodyToMono(CreateDataSetResponse.class)
                .flatMap(response -> {
                    Object data = response.getData();
                    JSONArray jsonArray = JSONObject.parseObject(JSONObject.toJSONString(data)).getJSONArray("data");
                    Map<String, String> resultMap = IntStream.range(0, jsonArray.size())
                            .mapToObj(jsonArray::getJSONObject)
                            .collect(Collectors.toMap(
                                    obj -> obj.getString("fileId"),
                                    obj -> obj.getString("_id")
                            ));
                    List<File> fileList = datasetMapper.getFileNamesByKnowledgeBaseId(knowledgeBaseId);
                    List<File> updatedFileList = fileList.stream()
                            .map(file -> new File(
                                    resultMap.getOrDefault(file.getFileId(), file.getFileId()),
                                    file.getFileName()))
                            .toList();
                    datasetMapper.deleteFileById(knowledgeBaseId);
                    datasetMapper.insertFileName(updatedFileList, knowledgeBaseId);
                    return Mono.just(new ServiceResult(new ResultHeader(200, "create success")));
                });
    }

    private Mono<Void> uploadFile(FilePart filePart, String knowledgeBaseId, String serverIp) {
        String filename = filePart.filename();
        Flux<DataBuffer> content = filePart.content();

        MultipartBodyBuilder builder = new MultipartBodyBuilder();
        builder.part("metadata", "", APPLICATION_JSON);
        builder.part("bucketName", "dataset");
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
                .uri(serverIp + FastGptConstant.UPLOAD_FILE_URL)
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .header(CommonConstant.COOKIE, FastGptConstant.COOKIE_VALUE)
                .body(BodyInserters.fromMultipartData(builder.build()))
                .retrieve()
                .bodyToMono(CreateDataSetResponse.class)
                .flatMap(response -> {
                    if (response.getCode() != 200) {
                        log.error("response is {}", response);
                        return Mono.empty();
                    }
                    Object data = response.getData();
                    JSONObject jsonObject = JSON.parseObject(JSON.toJSONString(data));
                    String fileId = jsonObject.getString("fileId");
                    CreateCollectionParam createCollectionParam = getCreateCollectionParam(knowledgeBaseId, fileId, filename);

                    return webClient.post()
                            .uri(serverIp + FastGptConstant.CRATE_COLLECTION_URL)
                            .contentType(APPLICATION_JSON)
                            .header(CommonConstant.COOKIE, FastGptConstant.COOKIE_VALUE)
                            .bodyValue(createCollectionParam)
                            .retrieve()
                            .bodyToMono(CreateDataSetResponse.class)
                            .flatMap(responseData -> {
                                if (responseData.getCode() == 200) {
                                    File file = new File(String.valueOf(fileId), filename);
                                    datasetMapper.insertFileName(List.of(file), String.valueOf(knowledgeBaseId));
                                }
                                return Mono.empty();
                            });
                });
    }

    @NotNull
    private static CreateCollectionParam getCreateCollectionParam(String knowledgeBaseId, String fileId, String fileName) {
        CreateCollectionParam createCollectionParam = new CreateCollectionParam();
        createCollectionParam.setTrainingType("chunk");
        createCollectionParam.setDatasetId(knowledgeBaseId);
        createCollectionParam.setChunkSize(700);
        createCollectionParam.setChunkSplitter("");
        createCollectionParam.setFileId(fileId);
        createCollectionParam.setName(fileName);
        createCollectionParam.setQaPrompt("");
        return createCollectionParam;
    }

    @Override
    public Mono<ServiceResult> removeDataset(String knowledgeBaseId, String serverIp) {
        String url = serverIp + FastGptConstant.DELETE_DATASET_URL + knowledgeBaseId;
        return webClient.delete()
                .uri(url)
                .header(CommonConstant.COOKIE, FastGptConstant.COOKIE_VALUE)
                .retrieve()
                .bodyToMono(CreateDataSetResponse.class)
                .flatMap(response -> {
                    if (response.getCode() == 200) {
                        return Mono.fromRunnable(() -> {
                            try {
                                datasetMapper.deleteKnowledgeBaseByUuid(knowledgeBaseId);
                                datasetMapper.deleteFileById(knowledgeBaseId);
                            } catch (Exception dbException) {
                                throw new RuntimeException("Database operation failed", dbException);
                            }
                        }).then(Mono.just(new ServiceResult(new ResultHeader(200, "delete success"))));
                    } else {
                        return Mono.just(new ServiceResult(new ResultHeader(500, response.getStatusText())));
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
        updateParam.put("id", knowledgeBase.getKnowledgeBaseId());
        updateParam.put("name", knowledgeBase.getKnowledgeBaseName());
        updateParam.put("intro", knowledgeBase.getKnowledgeBaseDescription());
        updateParam.put("avatar", "core/dataset/commonDatasetColor");

        return webClient.put()
                .uri(maaSPlatform.getServerIp() + FastGptConstant.UPDATE_DATASET_URL)
                .contentType(APPLICATION_JSON)
                .header(CommonConstant.COOKIE, FastGptConstant.COOKIE_VALUE)
                .bodyValue(updateParam)
                .retrieve()
                .bodyToMono(CreateDataSetResponse.class)
                .flatMap(response -> {
                    if (response.getCode() == 200) {
                        return Mono.fromRunnable(() -> {
                            knowledgeBase.setUpdateTime(TimeUtil.getNowTime());
                            datasetMapper.updateKnowledgeBase(knowledgeBase);
                        }).then(Mono.just(new ServiceResult(new ResultHeader(200, "update success"))));
                    } else {
                        return Mono.just(new ServiceResult(new ResultHeader(500, response.getStatusText())));
                    }
                })
                .onErrorResume(e -> {
                    log.error("Error occurred while delete dataset: {}", e.getMessage());
                    return Mono.just(new ServiceResult(new ResultHeader(500, "update failed")));
                });
    }

    @Override
    public Mono<ServiceResult> uploadFiles(Flux<FilePart> fileParts, String knowledgeBaseId, String serverIp) {
        return fileParts.flatMap(filePart -> uploadFile(filePart, knowledgeBaseId, serverIp))
                .then(Mono.defer(() -> handleFileId(knowledgeBaseId, serverIp)))
                .onErrorResume(e -> {
                    log.error("Error occurred during file upload: {}", e.getMessage());
                    return Mono.just(new ServiceResult(new ResultHeader(500, "file upload failed")));
                });
    }

    @Override
    public Mono<ServiceResult> deleteFile(String fileId, String serverIp) {
        return webClient.delete().uri(serverIp + FastGptConstant.DELETE_FILE_URL + fileId)
                .header(CommonConstant.COOKIE, FastGptConstant.COOKIE_VALUE)
                .retrieve()
                .bodyToMono(CreateDataSetResponse.class)
                .flatMap(response -> {
                    if (response.getCode() == 200) {
                        return Mono.fromRunnable(() -> datasetMapper.deleteFileByFileId(fileId)).then(Mono.just(new ServiceResult(new ResultHeader(200, "delete file success"))));
                    } else {
                        return Mono.just(new ServiceResult(new ResultHeader(response.getCode(), response.getStatusText())));
                    }
                })
                .onErrorResume(e -> {
                    log.error("Error occurred while delete dataset: {}", e.getMessage());
                    return Mono.just(new ServiceResult(new ResultHeader(500, "delete file failed")));
                });
    }
}
