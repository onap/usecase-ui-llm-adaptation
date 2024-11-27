package org.onap.usecaseui.llmadaptation.service.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.onap.usecaseui.llmadaptation.bean.KnowledgeBase;
import org.onap.usecaseui.llmadaptation.bean.ResultHeader;
import org.onap.usecaseui.llmadaptation.bean.ServiceResult;
import org.onap.usecaseui.llmadaptation.bean.fastgpt.CreateCollectionParam;
import org.onap.usecaseui.llmadaptation.bean.fastgpt.CreateDataSetParam;
import org.onap.usecaseui.llmadaptation.bean.fastgpt.CreateDataSetResponse;
import org.onap.usecaseui.llmadaptation.constant.FastGptConstant;
import org.onap.usecaseui.llmadaptation.mapper.FastGptDatasetMapper;
import org.onap.usecaseui.llmadaptation.service.FastGptDatasetService;
import org.onap.usecaseui.llmadaptation.util.TimeUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.ContentDisposition;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

import static org.springframework.http.MediaType.APPLICATION_JSON;

@Slf4j
@Service
public class FastGptDatasetServiceImpl implements FastGptDatasetService {
    @Autowired
    private FastGptDatasetMapper fastGptDatasetMapper;

    @Autowired
    private WebClient webClient;

    public Mono<ServiceResult> createDataset(Flux<FilePart> fileParts, String metaData) {
        KnowledgeBase knowledgeBase = JSONObject.parseObject(metaData, KnowledgeBase.class);
        knowledgeBase.setUpdateTime(TimeUtil.getNowTime());
        CreateDataSetParam dataSetParam = new CreateDataSetParam();
        dataSetParam.setAgentModel("qwen2:7b");
        dataSetParam.setType("dataset");
        dataSetParam.setAvatar("core/dataset/commonDatasetColor");
        dataSetParam.setVectorModel("m3e");
        dataSetParam.setIntro(knowledgeBase.getKnowledgeBaseDescription());
        dataSetParam.setName(knowledgeBase.getKnowledgeBaseName());
        return webClient.post()
                .uri(FastGptConstant.CREATE_DATASET_URL)
                .contentType(APPLICATION_JSON)
                .header(FastGptConstant.COOKIE, FastGptConstant.COOKIE_VALUE)
                .bodyValue(dataSetParam)
                .retrieve()
                .bodyToMono(CreateDataSetResponse.class)
                .flatMap(response -> {
                    if (response.getCode() == 200) {
                        String knowledgeBaseId = String.valueOf(response.getData());
                        return fileParts.flatMap(filePart -> uploadFile(filePart, knowledgeBaseId))
                                .then(Mono.defer(() -> {
                                    knowledgeBase.setKnowledgeBaseId(knowledgeBaseId);
                                    fastGptDatasetMapper.insertKnowledgeBaseRecord(knowledgeBase);
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
                .uri(FastGptConstant.UPLOAD_FILE_URL)
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .header(FastGptConstant.COOKIE, FastGptConstant.COOKIE_VALUE)
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
                            .uri(FastGptConstant.CRATE_COLLECTION_URL)
                            .contentType(APPLICATION_JSON)
                            .header(FastGptConstant.COOKIE, FastGptConstant.COOKIE_VALUE)
                            .bodyValue(createCollectionParam)
                            .retrieve()
                            .bodyToMono(CreateDataSetResponse.class)
                            .flatMap(responseData -> {
                                if (responseData.getCode() == 200) {
                                    fastGptDatasetMapper.insertFileName(fileId, filename, knowledgeBaseId);
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

    public Mono<ServiceResult> removeDataset(String knowledgeBaseId) {
        String url = FastGptConstant.DELETE_DATASET_URL + knowledgeBaseId;
        return webClient.delete()
                .uri(url)
                .header(FastGptConstant.COOKIE, FastGptConstant.COOKIE_VALUE)
                .retrieve()
                .bodyToMono(CreateDataSetResponse.class)
                .flatMap(response -> {
                    if (response.getCode() == 200) {
                        return Mono.fromRunnable(() -> {
                            try {
                                fastGptDatasetMapper.deleteKnowledgeBaseByUuid(knowledgeBaseId);
                                fastGptDatasetMapper.deleteFileById(knowledgeBaseId);
                            } catch (Exception dbException) {
                                throw new RuntimeException("Database operation failed", dbException); // 抛出新异常
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

    public ServiceResult getDataSetRecord() {
        List<KnowledgeBase> knowledgeBaseRecords = fastGptDatasetMapper.getKnowledgeBaseRecords();
        if (CollectionUtils.isEmpty(knowledgeBaseRecords)) {
            return new ServiceResult(new ResultHeader(200, "get datasets failed"), knowledgeBaseRecords);
        }

        knowledgeBaseRecords.forEach(knowledgeBase -> {
            List<String> fileNamesByKnowledgeBaseId = fastGptDatasetMapper.getFileNamesByKnowledgeBaseId(knowledgeBase.getKnowledgeBaseId());
            knowledgeBase.setFilesName(fileNamesByKnowledgeBaseId);
        });
        return new ServiceResult(new ResultHeader(200, "success"), knowledgeBaseRecords);
    }

    public ServiceResult geDatasetById(String knowledgeBaseId) {
        KnowledgeBase knowledgeBase = fastGptDatasetMapper.getKnowledgeBaseRecordById(knowledgeBaseId);
        if (knowledgeBase == null) {
            return new ServiceResult(new ResultHeader(200, "get dataset failed"));
        }
        List<String> fileNamesByKnowledgeBaseId = fastGptDatasetMapper.getFileNamesByKnowledgeBaseId(knowledgeBase.getKnowledgeBaseId());
        knowledgeBase.setFilesName(fileNamesByKnowledgeBaseId);

        return new ServiceResult(new ResultHeader(200, "success"), knowledgeBase);
    }

}
