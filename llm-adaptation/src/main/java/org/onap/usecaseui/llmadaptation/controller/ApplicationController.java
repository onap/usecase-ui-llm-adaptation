package org.onap.usecaseui.llmadaptation.controller;

import com.alibaba.fastjson2.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.onap.usecaseui.llmadaptation.bean.*;
import org.onap.usecaseui.llmadaptation.service.ApplicationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
@RestController
@RequestMapping("/api/usecaseui-llm-adaptation/v1/application")
public class ApplicationController {
    @Autowired
    private ApplicationService applicationService;

    @PostMapping(value = "/create", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ServiceResult> createApplication(@RequestBody Application application) {
        return applicationService.createApplication(application);

    }

    @DeleteMapping(value = "/delete/{applicationId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ServiceResult> removeKnowledgeBase(@PathVariable("applicationId") String applicationId) {
        return applicationService.removeApplication(applicationId);
    }

    @PostMapping(value = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ChatResponse> streamData(@RequestBody JSONObject question) {
        return applicationService.chat(question);
    }

    @GetMapping(value = {"/query"}, produces = MediaType.APPLICATION_JSON_VALUE)
    public ServiceResult getApplications() {
        return applicationService.getApplications();
    }

    @GetMapping(value = {"/queryById/{applicationId}"}, produces = MediaType.APPLICATION_JSON_VALUE)
    public ServiceResult getApplications(@PathVariable("applicationId") String applicationId) {
        return applicationService.getApplicationById(applicationId);
    }

    @PostMapping(value = "/edit", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ServiceResult> editDataset(@RequestBody Application application) {
        return applicationService.editApplication(application);
    }
}
