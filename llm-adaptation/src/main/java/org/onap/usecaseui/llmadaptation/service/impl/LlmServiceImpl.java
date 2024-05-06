/*
 * Copyright (C) 2024 CMCC, Inc. and others. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.onap.usecaseui.llmadaptation.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.nimbusds.jose.JOSEException;
import io.netty.channel.ChannelOption;
import lombok.extern.slf4j.Slf4j;
import org.onap.usecaseui.llmadaptation.bean.LargeModelRequestParam;
import org.onap.usecaseui.llmadaptation.service.LlmService;
import org.onap.usecaseui.llmadaptation.util.TokenUtil;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Optional;

import static org.onap.usecaseui.llmadaptation.constant.LLMConstant.RESPONSE;
import static org.onap.usecaseui.llmadaptation.constant.LLMConstant.API_KEY;
import static org.onap.usecaseui.llmadaptation.constant.LLMConstant.LARGE_MODEL_UIL;

@Slf4j
@Service
public class LlmServiceImpl implements LlmService {
    @Override
    public Flux<String> getStream(String question) {
        LargeModelRequestParam helpRequest = new LargeModelRequestParam();
        helpRequest.setPrompt(question);
        helpRequest.setReference(false);
        helpRequest.setStream(true);
        helpRequest.setHistory(new ArrayList<>());
        helpRequest.setTemperature(0.01);
        Optional<String> token = getToken();
        if (token.isEmpty()) {
            return Flux.just("Token Error");
        }
        HttpClient httpClient = HttpClient.create()
                .tcpConfiguration(tcpClient -> tcpClient
                        .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000))
                .responseTimeout(Duration.ofSeconds(10));
        WebClient webClient = WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
        return webClient.post()
                .uri(LARGE_MODEL_UIL)
                .bodyValue(helpRequest)
                .header("Authorization", "Bearer " + token.get())
                .header("Content-Type", "application/json")
                .retrieve()
                .bodyToFlux(String.class).flatMap(this::parseAndTransform)
                .onErrorResume(throwable -> {
                    log.error("An error occurred", throwable);
                    return Flux.just("Network Error");
                });
    }

    private Optional<String> getToken() {
        try {
            String token = TokenUtil.generateToken(API_KEY, 200000);
            return Optional.of(token);
        } catch (JOSEException e) {
            log.error("get token is error,error is {}", e.getMessage());
        }
        return Optional.empty();
    }

    private Flux<String> parseAndTransform(String param) {
        JSONObject jsonObject = JSON.parseObject(param);
        if (!jsonObject.containsKey(RESPONSE)) {
            return Flux.just("Response Error");
        }
        String response = jsonObject.getString(RESPONSE);
        String replace = response.replace("\n", "\\x0A");
        return Flux.just(replace);
    }
}
