package org.onap.usecaseui.llmadaptation.service.impl;

import com.alibaba.fastjson2.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.onap.usecaseui.llmadaptation.bean.*;
import org.onap.usecaseui.llmadaptation.constant.FastGptConstant;
import org.onap.usecaseui.llmadaptation.mapper.ApplicationMapper;
import org.onap.usecaseui.llmadaptation.mapper.DatasetMapper;
import org.onap.usecaseui.llmadaptation.mapper.MaaSPlatformMapper;
import org.onap.usecaseui.llmadaptation.service.BiShengDatasetService;
import org.onap.usecaseui.llmadaptation.service.DatasetService;
import org.onap.usecaseui.llmadaptation.service.FastGptDatasetService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

@Slf4j
@Service
public class DatasetServiceImpl implements DatasetService {

    @Autowired
    private DatasetMapper datasetMapper;

    @Autowired
    private FastGptDatasetService fastGptDatasetService;

    @Autowired
    private BiShengDatasetService biShengDatasetService;

    @Autowired
    private MaaSPlatformMapper maaSPlatformMapper;

    @Autowired
    private ApplicationMapper applicationMapper;

    @Override
    public Mono<ServiceResult> createDataset(Flux<FilePart> fileParts, String metaData) {
        KnowledgeBase knowledgeBase = JSONObject.parseObject(metaData, KnowledgeBase.class);
        List<KnowledgeBase> knowledgeBaseRecords = datasetMapper.getKnowledgeBaseRecords();
        if (!CollectionUtils.isEmpty(knowledgeBaseRecords)) {
            List<KnowledgeBase> collect = knowledgeBaseRecords.stream().filter(base -> base.getKnowledgeBaseName().equals(knowledgeBase.getKnowledgeBaseName())).toList();
            if (!collect.isEmpty()) {
                return Mono.just(new ServiceResult(new ResultHeader(500, "name exists"), knowledgeBaseRecords));
            }
        }
        MaaSPlatform maaSPlatformById = maaSPlatformMapper.getMaaSPlatformById(knowledgeBase.getMaaSPlatformId());
        String maaSType = maaSPlatformById.getMaaSType();
        if (FastGptConstant.FAST_GPT.equals(maaSType)) {
            return fastGptDatasetService.createDataset(fileParts, metaData, maaSPlatformById);
        }
        return biShengDatasetService.createDataset(fileParts, metaData, maaSPlatformById);
    }

    @Override
    public Mono<ServiceResult> removeDataset(String knowledgeBaseId) {
        List<Application> applicationByDatasetId = applicationMapper.getApplicationByDatasetId(knowledgeBaseId);
        if (!CollectionUtils.isEmpty(applicationByDatasetId)) {
            return Mono.just(new ServiceResult(new ResultHeader(500, "This database is currently in use")));
        }
        MaaSPlatform maaSPlatformById = getMaaSPlatform(knowledgeBaseId);
        String maaSType = maaSPlatformById.getMaaSType();
        if (FastGptConstant.FAST_GPT.equals(maaSType)) {
            return fastGptDatasetService.removeDataset(knowledgeBaseId, maaSPlatformById.getServerIp());
        }
        return biShengDatasetService.removeDataset(knowledgeBaseId, maaSPlatformById.getServerIp());
    }

    @Override
    public ServiceResult getDataSetRecord() {
        List<KnowledgeBase> knowledgeBaseRecords = datasetMapper.getKnowledgeBaseRecords();
        if (CollectionUtils.isEmpty(knowledgeBaseRecords)) {
            return new ServiceResult(new ResultHeader(200, "no dataset"), knowledgeBaseRecords);
        }

        knowledgeBaseRecords.forEach(knowledgeBase -> {
            List<File> fileNamesByKnowledgeBaseId = datasetMapper.getFileNamesByKnowledgeBaseId(knowledgeBase.getKnowledgeBaseId());
            knowledgeBase.setFileList(fileNamesByKnowledgeBaseId);
        });
        return new ServiceResult(new ResultHeader(200, "success"), knowledgeBaseRecords);
    }

    @Override
    public ServiceResult geDatasetById(String knowledgeBaseId) {
        KnowledgeBase knowledgeBase = datasetMapper.getKnowledgeBaseRecordById(knowledgeBaseId);
        if (knowledgeBase == null) {
            return new ServiceResult(new ResultHeader(500, "get dataset failed"));
        }
        List<File> fileNamesByKnowledgeBaseId = datasetMapper.getFileNamesByKnowledgeBaseId(knowledgeBase.getKnowledgeBaseId());
        knowledgeBase.setFileList(fileNamesByKnowledgeBaseId);
        return new ServiceResult(new ResultHeader(200, "success"), knowledgeBase);
    }

    @Override
    public Mono<ServiceResult> editDataset(KnowledgeBase knowledgeBase) {
        MaaSPlatform maaSPlatformById = getMaaSPlatform(knowledgeBase.getKnowledgeBaseId());
        String maaSType = maaSPlatformById.getMaaSType();
        if (FastGptConstant.FAST_GPT.equals(maaSType)) {
            return fastGptDatasetService.editDataset(knowledgeBase, maaSPlatformById);
        }
        return biShengDatasetService.editDataset(knowledgeBase, maaSPlatformById);
    }

    @Override
    public Mono<ServiceResult> uploadFiles(Flux<FilePart> fileParts, String metaData) {
        KnowledgeBase knowledgeBase = JSONObject.parseObject(metaData, KnowledgeBase.class);
        MaaSPlatform maaSPlatform = getMaaSPlatform(knowledgeBase.getKnowledgeBaseId());
        String maaSType = maaSPlatform.getMaaSType();
        String knowledgeBaseId = knowledgeBase.getKnowledgeBaseId();
        String serverIp = maaSPlatform.getServerIp();
        if (FastGptConstant.FAST_GPT.equals(maaSType)) {
            return fastGptDatasetService.uploadFiles(fileParts,knowledgeBaseId, serverIp);
        }
        return biShengDatasetService.uploadFiles(fileParts, knowledgeBaseId, serverIp);
    }

    @Override
    public Mono<ServiceResult> deleteFile(String fileId) {
        String knowledgeId = datasetMapper.getKnowledgeIdByFileId(fileId);
        MaaSPlatform maaSPlatform = getMaaSPlatform(knowledgeId);
        String maaSType = maaSPlatform.getMaaSType();
        if (FastGptConstant.FAST_GPT.equals(maaSType)) {
            return fastGptDatasetService.deleteFile(fileId, maaSPlatform.getServerIp());
        }
        return biShengDatasetService.deleteFile(fileId, maaSPlatform.getServerIp());
    }

    private MaaSPlatform getMaaSPlatform(String knowledgeBaseId) {
        KnowledgeBase knowledgeBase = datasetMapper.getKnowledgeBaseRecordById(knowledgeBaseId);
        return maaSPlatformMapper.getMaaSPlatformById(knowledgeBase.getMaaSPlatformId());
    }
}
