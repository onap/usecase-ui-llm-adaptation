package org.onap.usecaseui.llmadaptation.service;

import com.alibaba.fastjson2.JSONObject;
import org.onap.usecaseui.llmadaptation.bean.Application;
import org.onap.usecaseui.llmadaptation.bean.ChatResponse;
import org.onap.usecaseui.llmadaptation.bean.ServiceResult;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface FastGptApplicationService {
    Mono<ServiceResult> createApplication(Application application, String serverIp);

    Flux<ChatResponse> chat(JSONObject question, String serverIp);

    Mono<ServiceResult> removeApplication(String applicationId,  String serverIp);

    Mono<ServiceResult> editApplication(Application application,  String serverIp);
}
