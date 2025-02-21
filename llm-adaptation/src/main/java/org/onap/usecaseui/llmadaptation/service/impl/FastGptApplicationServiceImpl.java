package org.onap.usecaseui.llmadaptation.service.impl;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.onap.usecaseui.llmadaptation.bean.Application;
import org.onap.usecaseui.llmadaptation.bean.ChatResponse;
import org.onap.usecaseui.llmadaptation.bean.ResultHeader;
import org.onap.usecaseui.llmadaptation.bean.ServiceResult;
import org.onap.usecaseui.llmadaptation.bean.fastgpt.dataset.CreateDataSetResponse;
import org.onap.usecaseui.llmadaptation.bean.fastgpt.application.*;
import org.onap.usecaseui.llmadaptation.constant.CommonConstant;
import org.onap.usecaseui.llmadaptation.constant.FastGptConstant;
import org.onap.usecaseui.llmadaptation.mapper.ApplicationMapper;
import org.onap.usecaseui.llmadaptation.service.FastGptApplicationService;
import org.onap.usecaseui.llmadaptation.util.CommonUtil;
import org.onap.usecaseui.llmadaptation.util.TimeUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.springframework.http.MediaType.APPLICATION_JSON;

@Slf4j
@Service
public class FastGptApplicationServiceImpl implements FastGptApplicationService {
    @Autowired
    private ResourceLoader resourceLoader;

    @Autowired
    private ApplicationMapper applicationMapper;

    @Autowired
    private WebClient webClient;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public Mono<ServiceResult> createApplication(Application application,  String serverIp) {
        try (InputStream inputStream = resourceLoader.getResource(FastGptConstant.CREATE_APP_PARAM_FILE_URL).getInputStream()) {
            CreateApplicationParam createApplicationParam = objectMapper.readValue(inputStream, CreateApplicationParam.class);
            createApplicationParam.setName(application.getApplicationName());

            return createApplication(createApplicationParam, application, serverIp)
                    .onErrorResume(e -> {
                        log.error("Error occurred while creating application: {}", e.getMessage());
                        return Mono.just(new ServiceResult(new ResultHeader(500, "Application creation failed")));
                    });

        } catch (IOException e) {
            log.error("Error occurred while reading input file: {}", e.getMessage());
            return Mono.just(new ServiceResult(new ResultHeader(500, "Failed to read input file")));
        }
    }

    private Mono<ServiceResult> createApplication(CreateApplicationParam createApplicationParam, Application application,  String serverIp) {
        return webClient.post()
                .uri(serverIp + FastGptConstant.CREATE_APPLICATION)
                .contentType(APPLICATION_JSON)
                .header(CommonConstant.COOKIE, FastGptConstant.COOKIE_VALUE)
                .bodyValue(createApplicationParam)
                .retrieve()
                .bodyToMono(CreateDataSetResponse.class)
                .flatMap(response -> {
                    if (response.getCode() == 200) {
                        return handleApplicationResponse(String.valueOf(response.getData()), application, serverIp);
                    }
                    return Mono.just(new ServiceResult(new ResultHeader(500, response.getStatusText())));
                });
    }

    private Mono<ServiceResult> handleApplicationResponse(String dataId, Application application,  String serverIp) {
        application.setApplicationId(dataId);
        String url = serverIp + FastGptConstant.UPDATE_APPLICATION + dataId;
        UpdateApplicationParam updateApplicationParam = new UpdateApplicationParam();
        updateApplicationParam.setAvatar("/imgs/app/avatar/simple.svg");
        updateApplicationParam.setDefaultPermission(0);
        updateApplicationParam.setName(application.getApplicationName());
        updateApplicationParam.setIntro(application.getApplicationDescription());

        return webClient.put()
                .uri(url)
                .contentType(APPLICATION_JSON)
                .header(CommonConstant.COOKIE, FastGptConstant.COOKIE_VALUE)
                .bodyValue(updateApplicationParam)
                .retrieve()
                .bodyToMono(CreateDataSetResponse.class)
                .flatMap(response -> {
                    if (response.getCode() == 200) {
                        return publishApplication(application, dataId, serverIp);
                    } else if (response.getCode() == 502000) {
                        return Mono.just(new ServiceResult(new ResultHeader(404, "The resource does not exist,please delete")));
                    } else {
                        return Mono.just(new ServiceResult(new ResultHeader(500, response.getStatusText())));
                    }
                });
    }

    private Mono<ServiceResult> publishApplication(Application application, String data,  String serverIp) {
        try (InputStream inputStream = resourceLoader.getResource(FastGptConstant.PUBLISH_APP_PARAM_FILE_URL).getInputStream()) {
            PublishApplicationParam publishApplicationParam = objectMapper.readValue(inputStream, PublishApplicationParam.class);
            publishApplicationParam.setVersionName(TimeUtil.getNowTime());
            publishApplicationParam.getChatConfig().setWelcomeText(application.getOpeningRemarks());
            setApplicationParameters(application, publishApplicationParam);
            String publishUrl = serverIp + FastGptConstant.PUBLISH_APPLICATION + data;

            return webClient.post()
                    .uri(publishUrl)
                    .contentType(APPLICATION_JSON)
                    .header(CommonConstant.COOKIE, FastGptConstant.COOKIE_VALUE)
                    .bodyValue(publishApplicationParam)
                    .retrieve()
                    .bodyToMono(CreateDataSetResponse.class)
                    .flatMap(response -> {
                        if (response.getCode() == 200) {
                            applicationMapper.insertApplication(application);
                            return Mono.just(new ServiceResult(new ResultHeader(200, "Application created successfully")));
                        }
                        return Mono.just(new ServiceResult(new ResultHeader(500, response.getStatusText())));
                    });
        } catch (IOException e) {
            log.error("Error occurred while reading publish parameters: {}", e.getMessage());
            return Mono.just(new ServiceResult(new ResultHeader(500, "Failed to read publish parameters")));
        }
    }

    private void setApplicationParameters(Application application, PublishApplicationParam publishApplicationParam) {
        publishApplicationParam.getNodes().forEach(node -> {
            if ("chatNode".equals(node.getFlowNodeType())) {
                node.getInputs().forEach(input -> {
                    switch (input.getKey()) {
                        case "temperature":
                            input.setValue(application.getTemperature());
                            break;
                        case "systemPrompt":
                            input.setValue(application.getPrompt());
                            break;
                        case "model":
                            log.info(application.getLargeModelName());
                            input.setValue(application.getLargeModelName());
                            break;
                    }
                });
            } else if ("datasetSearchNode".equals(node.getFlowNodeType())) {
                node.getInputs().forEach(input -> {
                    if ("datasets".equals(input.getKey())) {
                        JSONObject jsonObject = new JSONObject();
                        jsonObject.put("datasetId", application.getKnowledgeBaseId());
                        List<JSONObject> list = new ArrayList<>();
                        list.add(jsonObject);
                        input.setValue(list);
                    }
                });
            }
        });
    }

    public Flux<ChatResponse> chat(JSONObject question, String serverIp) {
        ChatParam chatParam = new ChatParam();
        chatParam.setAppId(question.getString("applicationId"));
        chatParam.setStream(true);
        chatParam.setDetail(true);
        chatParam.setChatId(UUID.randomUUID().toString());
        chatParam.setResponseChatItemId(UUID.randomUUID().toString());
        JSONObject time = new JSONObject();
        time.put("cTime", TimeUtil.getFormattedDateTime());
        chatParam.setVariables(time);
        Message message = new Message();
        message.setContent(question.getString("question"));
        message.setDataId(UUID.randomUUID().toString());
        message.setRole("user");
        List<Message> messages = new ArrayList<>();
        messages.add(message);
        chatParam.setMessages(messages);
        AtomicBoolean isDone = new AtomicBoolean(false);
        return webClient.post()
                .uri(serverIp + FastGptConstant.APPLICATION_CHAT_URL)
                .contentType(APPLICATION_JSON)
                .header(CommonConstant.COOKIE, FastGptConstant.COOKIE_VALUE)
                .bodyValue(chatParam)
                .retrieve()
                .bodyToFlux(String.class).flatMap(response -> parseAndTransform(response, isDone))
                .onErrorResume(throwable -> {
                    log.error("An error occurred {}", throwable.getMessage());
                    return CommonUtil.chatFailed();
                });
    }

    private Flux<ChatResponse> parseAndTransform(String param, AtomicBoolean isDone) {
        if (isDone.get()) {
            return Flux.empty();
        }
        JSONObject jsonObject = JSONObject.parseObject(param);
        if (!jsonObject.containsKey("choices")) {
            return Flux.empty();
        }
        JSONArray choices = jsonObject.getJSONArray("choices");
        JSONObject choice = choices.getJSONObject(0);
        ChatResponse response = new ChatResponse();
        response.setReference("");
        response.setFinished("stop");
        ResultHeader resultHeader = new ResultHeader(200,"success");
        response.setResult_header(resultHeader);
        if ("stop".equals(choice.getString("finish_reason"))) {
            isDone.set(true);
            response.setAnswer("[DONE]");
            return Flux.just(response);
        }
        String string = choice.getJSONObject("delta").getString("content");
        isDone.set(false);
        string = string.replace(" ", "__SPACE__");
        response.setAnswer(string);
        return Flux.just(response);
    }

    @Override
    public Mono<ServiceResult> removeApplication(String applicationId,  String serverIp) {
        String url = serverIp + FastGptConstant.DELETE_APPLICATION + applicationId;
        return webClient.delete()
                .uri(url)
                .header(CommonConstant.COOKIE, FastGptConstant.COOKIE_VALUE)
                .retrieve()
                .bodyToMono(CreateDataSetResponse.class)
                .flatMap(response -> {
                    if (response.getCode() == 200 || response.getCode() == 502000) {
                        return Mono.fromRunnable(() -> {
                            try {
                                applicationMapper.deleteApplicationById(applicationId);
                            } catch (Exception dbException) {
                                throw new RuntimeException("Database operation failed", dbException);
                            }
                        }).then(Mono.just(new ServiceResult(new ResultHeader(200, "delete success"))));
                    } else {
                        return Mono.just(new ServiceResult(new ResultHeader(500, response.getStatusText())));
                    }
                })
                .onErrorResume(e -> {
                    log.error("Error occurred while delete dataset: {}", e.getMessage());
                    return Mono.just(new ServiceResult(new ResultHeader(500, "delete failed")));
                });
    }

    @Override
    public Mono<ServiceResult> editApplication(Application application,  String serverIp) {
        return handleApplicationResponse(application.getApplicationId(), application, serverIp);
    }

}
