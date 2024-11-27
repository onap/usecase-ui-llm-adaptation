package org.onap.usecaseui.llmadaptation.controller;

import lombok.extern.slf4j.Slf4j;
import org.onap.usecaseui.llmadaptation.bean.KnowledgeBase;
import org.onap.usecaseui.llmadaptation.bean.ResultHeader;
import org.onap.usecaseui.llmadaptation.bean.ServiceResult;
import org.onap.usecaseui.llmadaptation.mapper.FastGptDatasetMapper;
import org.onap.usecaseui.llmadaptation.service.FastGptDatasetService;
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
    private FastGptDatasetService fastGptDatasetService;

    @Autowired
    private FastGptDatasetMapper fastGptDatasetMapper;

    @PostMapping(value = "/create", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Mono<ServiceResult> handleFileUpload(@RequestPart("files") Flux<FilePart> fileParts,
                                                @RequestPart("metaData") String metaData) {
        return fastGptDatasetService.createDataset(fileParts, metaData);
    }

    @DeleteMapping(value = "/delete/{knowledgeBaseId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ServiceResult> removeKnowledgeBase(@PathVariable("knowledgeBaseId") String knowledgeBaseId) {
        return fastGptDatasetService.removeDataset(knowledgeBaseId);
    }

    @GetMapping(value = {"/query"}, produces = MediaType.APPLICATION_JSON_VALUE)
    public ServiceResult getKnowledgeBaseRecord() {
        return fastGptDatasetService.getDataSetRecord();
    }

    @GetMapping(value = {"/queryById/{knowledgeBaseId}"}, produces = MediaType.APPLICATION_JSON_VALUE)
    public ServiceResult getKnowledgeBaseRecordById(@PathVariable("knowledgeBaseId") String knowledgeBaseId) {
        return fastGptDatasetService.geDatasetById(knowledgeBaseId);
    }

    @GetMapping(value = {"/queryByMaaSId/{maaSPlatformId}"}, produces = MediaType.APPLICATION_JSON_VALUE)
    public ServiceResult getKnowledgeBaseRecordByMaaSId(@PathVariable("maaSPlatformId") String maaSPlatformId) {
        List<KnowledgeBase> knowledgeBaseByMaaSId = fastGptDatasetMapper.getKnowledgeBaseByMaaSId(maaSPlatformId);
        return new ServiceResult(new ResultHeader(200, "success"), knowledgeBaseByMaaSId);
    }
}
