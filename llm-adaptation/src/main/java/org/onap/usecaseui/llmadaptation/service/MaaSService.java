package org.onap.usecaseui.llmadaptation.service;

import org.onap.usecaseui.llmadaptation.bean.Operator;
import reactor.core.publisher.Mono;

import java.util.List;

public interface MaaSService {
    List<Operator> getAllMaaSPlatform();

    Mono<String> loginFastGpt();
}
