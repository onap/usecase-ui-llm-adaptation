package org.onap.usecaseui.llmadaptation.service.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.onap.usecaseui.llmadaptation.bean.KnowledgeBase;
import org.onap.usecaseui.llmadaptation.bean.ResultHeader;
import org.onap.usecaseui.llmadaptation.bean.ServiceResult;
import org.onap.usecaseui.llmadaptation.bean.fastgpt.dataset.CreateCollectionParam;
import org.onap.usecaseui.llmadaptation.bean.fastgpt.dataset.CreateDataSetParam;
import org.onap.usecaseui.llmadaptation.bean.fastgpt.dataset.CreateDataSetResponse;
import org.onap.usecaseui.llmadaptation.constant.CommonConstant;
import org.onap.usecaseui.llmadaptation.constant.FastGptConstant;
import org.onap.usecaseui.llmadaptation.constant.ServerConstant;
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


import static org.springframework.http.MediaType.APPLICATION_JSON;

@Slf4j
@Service
public class FastGptDatasetServiceImpl implements FastGptDatasetService {
    @Autowired
    private DatasetMapper datasetMapper;

    @Autowired
    private WebClient webClient;

    @Autowired
    private ServerConstant serverConstant;

    @Override
    public Mono<ServiceResult> createDataset(Flux<FilePart> fileParts, String metaData) {
        KnowledgeBase knowledgeBase = JSONObject.parseObject(metaData, KnowledgeBase.class);
        knowledgeBase.setUpdateTime(TimeUtil.getNowTime());
        CreateDataSetParam dataSetParam = new CreateDataSetParam();
        dataSetParam.setAgentModel(serverConstant.getFastGptModel());
        dataSetParam.setType("dataset");
        dataSetParam.setAvatar("core/dataset/commonDatasetColor");
        dataSetParam.setVectorModel("m3e");
        dataSetParam.setIntro(knowledgeBase.getKnowledgeBaseDescription());
        dataSetParam.setName(knowledgeBase.getKnowledgeBaseName());
        return webClient.post()
                .uri(serverConstant.getFastGptServer() + FastGptConstant.CREATE_DATASET_URL)
                .contentType(APPLICATION_JSON)
                .header(CommonConstant.COOKIE, FastGptConstant.COOKIE_VALUE)
                .bodyValue(dataSetParam)
                .retrieve()
                .bodyToMono(CreateDataSetResponse.class)
                .flatMap(response -> {
                    if (response.getCode() == 200) {
                        String knowledgeBaseId = String.valueOf(response.getData());
                        return fileParts.flatMap(filePart -> uploadFile(filePart, knowledgeBaseId))
                                .then(Mono.defer(() -> {
                                    knowledgeBase.setKnowledgeBaseId(knowledgeBaseId);
                                    datasetMapper.insertKnowledgeBaseRecord(knowledgeBase);
                                    return Mono.just(new ServiceResult(new ResultHeader(200, "create success")));
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

    private Mono<Void> uploadFile(FilePart filePart, String knowledgeBaseId) {
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
                .uri(serverConstant.getFastGptServer() + FastGptConstant.UPLOAD_FILE_URL)
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
                    CreateCollectionParam createCollectionParam = getCreateCollectionParam(knowledgeBaseId, fileId);

                    return webClient.post()
                            .uri(serverConstant.getFastGptServer() + FastGptConstant.CRATE_COLLECTION_URL)
                            .contentType(APPLICATION_JSON)
                            .header(CommonConstant.COOKIE, FastGptConstant.COOKIE_VALUE)
                            .bodyValue(createCollectionParam)
                            .retrieve()
                            .bodyToMono(CreateDataSetResponse.class)
                            .flatMap(responseData -> {
                                if (responseData.getCode() == 200) {
                                    datasetMapper.insertFileName(fileId, filename, knowledgeBaseId);
                                }
                                return Mono.empty();
                            });
                });
    }

    @NotNull
    private static CreateCollectionParam getCreateCollectionParam(String knowledgeBaseId, String fileId) {
        CreateCollectionParam createCollectionParam = new CreateCollectionParam();
        createCollectionParam.setTrainingType("chunk");
        createCollectionParam.setDatasetId(knowledgeBaseId);
        createCollectionParam.setChunkSize(700);
        createCollectionParam.setChunkSplitter("");
        createCollectionParam.setFileId(fileId);
        createCollectionParam.setName("");
        createCollectionParam.setQaPrompt("");
        return createCollectionParam;
    }

    @Override
    public Mono<ServiceResult> removeDataset(String knowledgeBaseId) {
        String url = serverConstant.getFastGptServer() + FastGptConstant.DELETE_DATASET_URL + knowledgeBaseId;
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
    public Mono<ServiceResult> editDataset(KnowledgeBase knowledgeBase) {
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
                .uri(serverConstant.getFastGptServer() + FastGptConstant.UPDATE_DATASET_URL)
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
}
