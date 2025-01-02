package org.onap.usecaseui.llmadaptation.service.impl;

import com.alibaba.fastjson2.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.onap.usecaseui.llmadaptation.bean.*;
import org.onap.usecaseui.llmadaptation.constant.ServerConstant;
import org.onap.usecaseui.llmadaptation.mapper.ApplicationMapper;
import org.onap.usecaseui.llmadaptation.mapper.DatasetMapper;
import org.onap.usecaseui.llmadaptation.mapper.MaaSPlatformMapper;
import org.onap.usecaseui.llmadaptation.service.ApplicationService;
import org.onap.usecaseui.llmadaptation.service.BiShengApplicationService;
import org.onap.usecaseui.llmadaptation.service.FastGptApplicationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

@Slf4j
@Service
public class ApplicationServiceImpl implements ApplicationService {
    @Autowired
    private ApplicationMapper applicationMapper;

    @Autowired
    private DatasetMapper datasetMapper;

    @Autowired
    private FastGptApplicationService fastGptApplicationService;

    @Autowired
    private BiShengApplicationService biShengApplicationService;

    @Autowired
    private MaaSPlatformMapper maaSPlatformMapper;

    @Autowired
    private ServerConstant serverConstant;

    @Override
    public Mono<ServiceResult> createApplication(Application application) {
        List<Application> applications = applicationMapper.getAllApplication();
        if (!CollectionUtils.isEmpty(applications)) {
            List<Application> collect = applications.stream().filter(app -> app.getApplicationName().equals(application.getApplicationName())).toList();
            if (!collect.isEmpty()) {
                return Mono.just(new ServiceResult(new ResultHeader(500, "name exists"), applications));
            }
        }
        MaaSPlatform maaSPlatformById = maaSPlatformMapper.getMaaSPlatformById(application.getMaaSPlatformId());
        if (maaSPlatformById == null) {
            return Mono.just(new ServiceResult(new ResultHeader(500, "maas is not exist")));
        }
        String maaSType = maaSPlatformById.getMaaSType();
        String fastGptType = serverConstant.getFastGptType();
        if (fastGptType.equals(maaSType)) {
            return fastGptApplicationService.createApplication(application);
        }
        return biShengApplicationService.createApplication(application);
    }

    @Override
    public Mono<ServiceResult> removeApplication(String applicationId) {
        String maaSType = getMaaSType(applicationId);
        String fastGptType = serverConstant.getFastGptType();
        if (fastGptType.equals(maaSType)) {
            return fastGptApplicationService.removeApplication(applicationId);
        }
        return biShengApplicationService.removeApplication(applicationId);
    }

    @Override
    public Flux<String> chat(JSONObject question) {
        String applicationId = question.getString("applicationId");
        String maaSType = getMaaSType(applicationId);
        String fastGptType = serverConstant.getFastGptType();
        if (fastGptType.equals(maaSType)) {
            return fastGptApplicationService.chat(question);
        }
        return biShengApplicationService.chat(question);
    }

    @Override
    public ServiceResult getApplications() {
        List<Application> allApplication = applicationMapper.getAllApplication();
        if (CollectionUtils.isEmpty(allApplication)) {
            return new ServiceResult(new ResultHeader(500, "no application"), allApplication);
        }
        allApplication.forEach(application -> {
            KnowledgeBase knowledgeBaseRecordById = datasetMapper.getKnowledgeBaseRecordById(application.getKnowledgeBaseId());
            if (knowledgeBaseRecordById != null) {
                application.setOperatorId(knowledgeBaseRecordById.getOperatorId());
                application.setOperatorName(knowledgeBaseRecordById.getOperatorName());
                application.setMaaSPlatformId(knowledgeBaseRecordById.getMaaSPlatformId());
                application.setMaaSPlatformName(knowledgeBaseRecordById.getMaaSPlatformName());
                application.setKnowledgeBaseName(knowledgeBaseRecordById.getKnowledgeBaseName());
            }
        });
        return new ServiceResult(new ResultHeader(200, "success"), allApplication);
    }

    @Override
    public ServiceResult getApplicationById(String applicationId) {
        Application application = applicationMapper.getApplicationById(applicationId);
        if (application == null) {
            return new ServiceResult(new ResultHeader(500, "no application"), application);
        }
        KnowledgeBase knowledgeBaseRecordById = datasetMapper.getKnowledgeBaseRecordById(application.getKnowledgeBaseId());
        application.setOperatorId(knowledgeBaseRecordById.getOperatorId());
        application.setOperatorName(knowledgeBaseRecordById.getOperatorName());
        application.setMaaSPlatformId(knowledgeBaseRecordById.getMaaSPlatformId());
        application.setMaaSPlatformName(knowledgeBaseRecordById.getMaaSPlatformName());
        application.setKnowledgeBaseName(knowledgeBaseRecordById.getKnowledgeBaseName());
        return new ServiceResult(new ResultHeader(200, "success"), application);
    }

    @Override
    public Mono<ServiceResult> editApplication(Application application) {
        MaaSPlatform maaSPlatformById = maaSPlatformMapper.getMaaSPlatformById(application.getMaaSPlatformId());
        if (maaSPlatformById == null) {
            return Mono.just(new ServiceResult(new ResultHeader(500, "maas is not exist")));
        }
        String maaSType = maaSPlatformById.getMaaSType();
        String fastGptType = serverConstant.getFastGptType();
        if (fastGptType.equals(maaSType)) {
            return fastGptApplicationService.editApplication(application);
        }
        return biShengApplicationService.editApplication(application);
    }

    private String getMaaSType(String applicationId) {
        Application applicationById = applicationMapper.getApplicationById(applicationId);
        KnowledgeBase knowledgeBaseRecordById = datasetMapper.getKnowledgeBaseRecordById(applicationById.getKnowledgeBaseId());
        MaaSPlatform maaSPlatformById = maaSPlatformMapper.getMaaSPlatformById(knowledgeBaseRecordById.getMaaSPlatformId());
        return maaSPlatformById.getMaaSType();
    }
}
