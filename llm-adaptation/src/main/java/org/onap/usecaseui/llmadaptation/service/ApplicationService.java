package org.onap.usecaseui.llmadaptation.service;

import com.alibaba.fastjson2.JSONObject;
import org.onap.usecaseui.llmadaptation.bean.Application;
import org.onap.usecaseui.llmadaptation.bean.ServiceResult;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface ApplicationService {
    Mono<ServiceResult> createApplication(Application application);

    Flux<String> chat(JSONObject question);

    Mono<ServiceResult> removeApplication(String applicationId);

    ServiceResult getApplications();

    ServiceResult getApplicationById(String applicationId);

    Mono<ServiceResult> editApplication(Application application);
}
