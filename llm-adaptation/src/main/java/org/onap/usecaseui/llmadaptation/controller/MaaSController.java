package org.onap.usecaseui.llmadaptation.controller;

import lombok.extern.slf4j.Slf4j;
import org.onap.usecaseui.llmadaptation.bean.*;
import org.onap.usecaseui.llmadaptation.mapper.MaaSPlatformMapper;
import org.onap.usecaseui.llmadaptation.service.MaaSService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/usecaseui-llm-adaptation/v1/")
public class MaaSController {

    @Autowired
    private MaaSPlatformMapper maaSPlatformMapper;

    @Autowired
    private MaaSService maaSService;

    @GetMapping(value = {"/operator/maas/getAll"}, produces = MediaType.APPLICATION_JSON_VALUE)
    public ServiceResult getKnowledgeBaseRecord() {
        List<Operator> allMaaSPlatform = maaSService.getAllMaaSPlatform();
        return new ServiceResult(new ResultHeader(200, "success"), allMaaSPlatform);
    }

    @PostMapping(value = "/maas/register", produces = MediaType.APPLICATION_JSON_VALUE)
    public ServiceResult registerMaaSPlatform(@RequestBody MaaSPlatform maaSPlatform) {
        maaSPlatformMapper.insertMaaSPlatform(maaSPlatform);
        maaSPlatformMapper.insertModel(maaSPlatform.getMaaSPlatformId(), maaSPlatform.getModelList());
        return new ServiceResult(new ResultHeader(200, "success"));
    }


    @GetMapping(value = "/maas/login")
    public Mono<String> get() {
        return maaSService.loginFastGpt();
    }
}
