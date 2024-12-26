package org.onap.usecaseui.llmadaptation.service.impl;

import com.alibaba.fastjson2.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.onap.usecaseui.llmadaptation.bean.KnowledgeBase;
import org.onap.usecaseui.llmadaptation.bean.MaaSPlatform;
import org.onap.usecaseui.llmadaptation.bean.ResultHeader;
import org.onap.usecaseui.llmadaptation.bean.ServiceResult;
import org.onap.usecaseui.llmadaptation.constant.ServerConstant;
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
    private ServerConstant serverConstant;

    @Override
    public Mono<ServiceResult> createDataset(Flux<FilePart> fileParts, String metaData) {
        KnowledgeBase knowledgeBase = JSONObject.parseObject(metaData, KnowledgeBase.class);
        List<KnowledgeBase> knowledgeBaseRecords = datasetMapper.getKnowledgeBaseRecords();
        if (!CollectionUtils.isEmpty(knowledgeBaseRecords)) {
            List<KnowledgeBase> collect = knowledgeBaseRecords.stream().filter(base -> base.getKnowledgeBaseName().equals(knowledgeBase.getKnowledgeBaseName())).toList();
            if (!collect.isEmpty()) {
                return Mono.just(new ServiceResult(new ResultHeader(200, "name exists"), knowledgeBaseRecords));
            }
        }
        MaaSPlatform maaSPlatformById = maaSPlatformMapper.getMaaSPlatformById(knowledgeBase.getMaaSPlatformId());
        String maaSType = maaSPlatformById.getMaaSType();
        String fastGptType = serverConstant.getFastGptType();
        if (fastGptType.equals(maaSType)) {
            return fastGptDatasetService.createDataset(fileParts, metaData);
        }
        return biShengDatasetService.createDataset(fileParts, metaData);
    }

    @Override
    public Mono<ServiceResult> removeDataset(String knowledgeBaseId) {
        KnowledgeBase knowledgeBaseRecordById = datasetMapper.getKnowledgeBaseRecordById(knowledgeBaseId);
        MaaSPlatform maaSPlatformById = maaSPlatformMapper.getMaaSPlatformById(knowledgeBaseRecordById.getMaaSPlatformId());
        String maaSType = maaSPlatformById.getMaaSType();
        String fastGptType = serverConstant.getFastGptType();
        if (fastGptType.equals(maaSType)) {
            return fastGptDatasetService.removeDataset(knowledgeBaseId);
        }
        return biShengDatasetService.removeDataset(knowledgeBaseId);
    }

    @Override
    public ServiceResult getDataSetRecord() {
        List<KnowledgeBase> knowledgeBaseRecords = datasetMapper.getKnowledgeBaseRecords();
        if (CollectionUtils.isEmpty(knowledgeBaseRecords)) {
            return new ServiceResult(new ResultHeader(500, "get datasets failed"), knowledgeBaseRecords);
        }

        knowledgeBaseRecords.forEach(knowledgeBase -> {
            List<String> fileNamesByKnowledgeBaseId = datasetMapper.getFileNamesByKnowledgeBaseId(knowledgeBase.getKnowledgeBaseId());
            knowledgeBase.setFilesName(fileNamesByKnowledgeBaseId);
        });
        return new ServiceResult(new ResultHeader(200, "success"), knowledgeBaseRecords);
    }

    @Override
    public ServiceResult geDatasetById(String knowledgeBaseId) {
        KnowledgeBase knowledgeBase = datasetMapper.getKnowledgeBaseRecordById(knowledgeBaseId);
        if (knowledgeBase == null) {
            return new ServiceResult(new ResultHeader(500, "get dataset failed"));
        }
        List<String> fileNamesByKnowledgeBaseId = datasetMapper.getFileNamesByKnowledgeBaseId(knowledgeBase.getKnowledgeBaseId());
        knowledgeBase.setFilesName(fileNamesByKnowledgeBaseId);

        return new ServiceResult(new ResultHeader(200, "success"), knowledgeBase);
    }

    @Override
    public Mono<ServiceResult> editDataset(KnowledgeBase knowledgeBase) {
        KnowledgeBase knowledgeBaseRecordById = datasetMapper.getKnowledgeBaseRecordById(knowledgeBase.getKnowledgeBaseId());
        MaaSPlatform maaSPlatformById = maaSPlatformMapper.getMaaSPlatformById(knowledgeBaseRecordById.getMaaSPlatformId());
        String maaSType = maaSPlatformById.getMaaSType();
        String fastGptType = serverConstant.getFastGptType();
        if (fastGptType.equals(maaSType)) {
            return fastGptDatasetService.editDataset(knowledgeBase);
        }
        return biShengDatasetService.editDataset(knowledgeBase);
    }
}
