package org.onap.usecaseui.llmadaptation.service.impl;

import com.alibaba.fastjson2.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.onap.usecaseui.llmadaptation.bean.*;
import org.onap.usecaseui.llmadaptation.constant.FastGptConstant;
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

    @Override
    public Mono<ServiceResult> createApplication(Application application) {
        List<Application> applications = applicationMapper.getAllApplication();
        if (!CollectionUtils.isEmpty(applications)) {
            List<Application> collect = applications.stream().filter(app -> app.getApplicationName().equals(application.getApplicationName())).toList();
            if (!collect.isEmpty()) {
                return Mono.just(new ServiceResult(new ResultHeader(500, "name exists"), applications));
            }
        }
        MaaSPlatform maaSPlatformById = getMaaSPlatFormById(application.getMaaSPlatformId());
        if (maaSPlatformById == null) {
            return Mono.just(new ServiceResult(new ResultHeader(500, "maas is not exist")));
        }
        String maaSType = maaSPlatformById.getMaaSType();
        if (FastGptConstant.FAST_GPT.equals(maaSType)) {
            return fastGptApplicationService.createApplication(application, maaSPlatformById.getServerIp());
        }
        return biShengApplicationService.createApplication(application, maaSPlatformById.getServerIp());
    }

    @Override
    public Mono<ServiceResult> removeApplication(String applicationId) {
        MaaSPlatform maaSPlatform = getMaaSPlatFormByAppId(applicationId);
        if (FastGptConstant.FAST_GPT.equals(maaSPlatform.getMaaSType())) {
            return fastGptApplicationService.removeApplication(applicationId, maaSPlatform.getServerIp());
        }
        return biShengApplicationService.removeApplication(applicationId, maaSPlatform.getServerIp());
    }

    @Override
    public Flux<ChatResponse> chat(JSONObject question) {
        String applicationId = question.getString("applicationId");
        MaaSPlatform maaSPlatform = getMaaSPlatFormByAppId(applicationId);
        if (FastGptConstant.FAST_GPT.equals(maaSPlatform.getMaaSType())) {
            return fastGptApplicationService.chat(question, maaSPlatform.getServerIp());
        }
        return biShengApplicationService.chat(question, maaSPlatform.getServerIp());
    }

    @Override
    public ServiceResult getApplications() {
        List<Application> allApplication = applicationMapper.getAllApplication();
        if (CollectionUtils.isEmpty(allApplication)) {
            return new ServiceResult(new ResultHeader(200, "no application"), allApplication);
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
        MaaSPlatform maaSPlatformById = getMaaSPlatFormById(application.getMaaSPlatformId());
        if (maaSPlatformById == null) {
            return Mono.just(new ServiceResult(new ResultHeader(500, "maas is not exist")));
        }
        String maaSType = maaSPlatformById.getMaaSType();
        if (FastGptConstant.FAST_GPT.equals(maaSType)) {
            return fastGptApplicationService.editApplication(application, maaSPlatformById.getServerIp());
        }
        return biShengApplicationService.editApplication(application, maaSPlatformById.getServerIp());
    }

    private MaaSPlatform getMaaSPlatFormByAppId(String applicationId) {
        Application applicationById = applicationMapper.getApplicationById(applicationId);
        KnowledgeBase knowledgeBaseRecordById = datasetMapper.getKnowledgeBaseRecordById(applicationById.getKnowledgeBaseId());
        return getMaaSPlatFormById(knowledgeBaseRecordById.getMaaSPlatformId());
    }

    private MaaSPlatform getMaaSPlatFormById(String maaSPlatformId) {
        return maaSPlatformMapper.getMaaSPlatformById(maaSPlatformId);
    }
}
