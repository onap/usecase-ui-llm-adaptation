package org.onap.usecaseui.llmadaptation.controller;

import lombok.extern.slf4j.Slf4j;
import org.onap.usecaseui.llmadaptation.bean.KnowledgeBase;
import org.onap.usecaseui.llmadaptation.bean.ResultHeader;
import org.onap.usecaseui.llmadaptation.bean.ServiceResult;
import org.onap.usecaseui.llmadaptation.mapper.DatasetMapper;
import org.onap.usecaseui.llmadaptation.service.DatasetService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/usecaseui-llm-adaptation/v1/knowledgeBase")
public class DatasetController {

    @Autowired
    private DatasetMapper datasetMapper;

    @Autowired
    private DatasetService datasetService;


    @PostMapping(value = "/create", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Mono<ServiceResult> handleFileUpload(@RequestPart("files") Flux<FilePart> fileParts,
                                                @RequestPart("metaData") String metaData) {
        return datasetService.createDataset(fileParts, metaData);
    }

    @DeleteMapping(value = "/delete/{knowledgeBaseId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ServiceResult> removeKnowledgeBase(@PathVariable("knowledgeBaseId") String knowledgeBaseId) {
        return datasetService.removeDataset(knowledgeBaseId);
    }

    @GetMapping(value = {"/query"}, produces = MediaType.APPLICATION_JSON_VALUE)
    public ServiceResult getKnowledgeBaseRecord() {
        return datasetService.getDataSetRecord();
    }

    @GetMapping(value = {"/queryById/{knowledgeBaseId}"}, produces = MediaType.APPLICATION_JSON_VALUE)
    public ServiceResult getKnowledgeBaseRecordById(@PathVariable("knowledgeBaseId") String knowledgeBaseId) {
        return datasetService.geDatasetById(knowledgeBaseId);
    }

    @GetMapping(value = {"/queryByMaaSId/{maaSPlatformId}"}, produces = MediaType.APPLICATION_JSON_VALUE)
    public ServiceResult getKnowledgeBaseRecordByMaaSId(@PathVariable("maaSPlatformId") String maaSPlatformId) {
        List<KnowledgeBase> knowledgeBaseByMaaSId = datasetMapper.getKnowledgeBaseByMaaSId(maaSPlatformId);
        return new ServiceResult(new ResultHeader(200, "success"), knowledgeBaseByMaaSId);
    }

    @PostMapping(value = "/edit", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ServiceResult> editDataset(@RequestBody KnowledgeBase knowledgeBase) {
        return datasetService.editDataset(knowledgeBase);
    }

    @PostMapping(value = "/file/upload", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ServiceResult> uploadFiles(@RequestPart("files") Flux<FilePart> fileParts,
                                           @RequestPart("metaData") String metaData) {
        return datasetService.uploadFiles(fileParts, metaData);
    }

    @DeleteMapping(value = "/file/delete/{fileId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ServiceResult> deleteFile(@PathVariable("fileId") String fileId) {
        return datasetService.deleteFile(fileId);
    }
}
