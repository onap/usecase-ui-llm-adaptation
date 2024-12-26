package org.onap.usecaseui.llmadaptation.service;

import com.alibaba.fastjson2.JSONObject;
import org.onap.usecaseui.llmadaptation.bean.Application;
import org.onap.usecaseui.llmadaptation.bean.ServiceResult;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface BiShengApplicationService {
    Mono<ServiceResult> createApplication(Application application);

    Flux<String> chat(JSONObject question);

    Mono<ServiceResult> removeApplication(String applicationId);

    Mono<ServiceResult> editApplication(Application application);
}
