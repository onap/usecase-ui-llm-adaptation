package org.onap.usecaseui.llmadaptation.controller;

import com.alibaba.fastjson2.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.onap.usecaseui.llmadaptation.bean.Application;
import org.onap.usecaseui.llmadaptation.bean.ServiceResult;
import org.onap.usecaseui.llmadaptation.service.FastGptApplicationService;
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
    private FastGptApplicationService fastGptApplicationService;

    @PostMapping(value = "/create", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ServiceResult> createApplication(@RequestBody Application application) {
        return fastGptApplicationService.createApplication(application);
    }

    @DeleteMapping(value = "/delete/{applicationId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ServiceResult> removeKnowledgeBase(@PathVariable("applicationId") String applicationId) {
        return fastGptApplicationService.removeApplication(applicationId);
    }

    @PostMapping(value = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> streamData(@RequestBody JSONObject question) {
        return fastGptApplicationService.chat(question);
    }

    @GetMapping(value = {"/query"}, produces = MediaType.APPLICATION_JSON_VALUE)
    public ServiceResult getApplications() {
        return fastGptApplicationService.getApplications();
    }

    @GetMapping(value = {"/queryById/{applicationId}"}, produces = MediaType.APPLICATION_JSON_VALUE)
    public ServiceResult getApplications(@PathVariable("applicationId") String applicationId) {
        return fastGptApplicationService.getApplicationById(applicationId);
    }
}
