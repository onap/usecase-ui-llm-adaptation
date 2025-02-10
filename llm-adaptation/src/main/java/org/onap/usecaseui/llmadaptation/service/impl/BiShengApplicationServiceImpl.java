package org.onap.usecaseui.llmadaptation.service.impl;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import io.netty.util.internal.StringUtil;
import lombok.extern.slf4j.Slf4j;
import org.onap.usecaseui.llmadaptation.bean.Application;
import org.onap.usecaseui.llmadaptation.bean.ResultHeader;
import org.onap.usecaseui.llmadaptation.bean.ServiceResult;
import org.onap.usecaseui.llmadaptation.bean.bisheng.BiShengCreateDatasetResponse;
import org.onap.usecaseui.llmadaptation.constant.BiShengConstant;
import org.onap.usecaseui.llmadaptation.constant.CommonConstant;
import org.onap.usecaseui.llmadaptation.mapper.ApplicationMapper;
import org.onap.usecaseui.llmadaptation.service.BiShengApplicationService;
import org.onap.usecaseui.llmadaptation.util.TimeUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;

import static org.springframework.http.MediaType.APPLICATION_JSON;

@Slf4j
@Service
public class BiShengApplicationServiceImpl implements BiShengApplicationService {
    @Autowired
    private ApplicationMapper applicationMapper;

    @Autowired
    private WebClient webClient;

    @Override
    public Mono<ServiceResult> createApplication(Application application, String serverIp) {
        JSONObject createParam = new JSONObject();
        createParam.put("logo", "");
        createParam.put("name", application.getApplicationName());
        createParam.put("prompt", application.getPrompt());
        return webClient.post()
                .uri(serverIp + BiShengConstant.APPLICATION_URL)
                .contentType(APPLICATION_JSON)
                .header(CommonConstant.COOKIE, BiShengConstant.COOKIE_VALUE)
                .bodyValue(createParam)
                .retrieve()
                .bodyToMono(BiShengCreateDatasetResponse.class)
                .flatMap(createResponse -> {
                    JSONObject data = createResponse.getData();
                    if (data == null) {
                        return Mono.just(new ServiceResult(new ResultHeader(createResponse.getStatus_code(), createResponse.getStatus_message())));
                    }
                    String applicationId = data.getString("id");
                    String applicationDescription = application.getApplicationDescription();
                    if (!StringUtil.isNullOrEmpty(applicationDescription)) {
                        data.put("desc", applicationDescription);
                    }
                    data.put("model_name", application.getLargeModelId());
                    data.put("temperature", application.getTemperature() / 10);
                    List<Integer> list = new ArrayList<>();
                    list.add(Integer.valueOf(application.getKnowledgeBaseId()));
                    data.put("knowledge_list", list);
                    data.put("guide_word", application.getOpeningRemarks());
                    data.put("update_time", TimeUtil.getNowTime());
                    return webClient.put()
                            .uri(serverIp + BiShengConstant.APPLICATION_URL)
                            .contentType(APPLICATION_JSON)
                            .header(CommonConstant.COOKIE, BiShengConstant.COOKIE_VALUE)
                            .bodyValue(data)
                            .retrieve()
                            .bodyToMono(BiShengCreateDatasetResponse.class)
                            .flatMap(updateResponse -> {
                                if (updateResponse.getStatus_code() == 200) {
                                    application.setApplicationId(applicationId);
                                    applicationMapper.insertApplication(application);
                                    return Mono.just(new ServiceResult(new ResultHeader(200, "Application created successfully")));
                                }
                                log.error("error is {}",updateResponse.getStatus_message());
                                return Mono.just(new ServiceResult(new ResultHeader(updateResponse.getStatus_code(), "Application created failed")));
                            });
                }).onErrorResume(e -> {
                    log.error("Error occurred while creating application: {}", e.getMessage());
                    return Mono.just(new ServiceResult(new ResultHeader(500, "Application created failed")));
                });
    }

    @Override
    public Flux<String> chat(JSONObject question, String serverIp) {
        JSONObject param = new JSONObject();
        param.put("model", question.getString("applicationId"));
        param.put("temperature", 0);
        param.put("stream", true);
        JSONObject message = new JSONObject();
        message.put("role", "user");
        message.put("content", question.getString("question"));
        JSONArray jsonArray = new JSONArray();
        jsonArray.add(message);
        param.put("messages", jsonArray);
        return webClient.post()
                .uri(serverIp + BiShengConstant.APPLICATION_CHAT_URL)
                .bodyValue(param)
                .retrieve()
                .bodyToFlux(String.class)
                .flatMap(response -> {
                    if ("[DONE]".equals(response)) {
                        return Flux.just(response);
                    }
                    JSONArray choices = JSONObject.parseObject(response).getJSONArray("choices");
                    String jsonString = JSONObject.toJSONString(choices.get(0));
                    JSONObject jsonObject = JSONObject.parseObject(jsonString);
                    String string = jsonObject.getJSONObject("delta").getString("content");
                    return Flux.just(string);
                })
                .onErrorResume(e -> {
                    log.error("An error occurred {}", e.getMessage());
                    return Flux.just("Network Error");
                });
    }

    @Override
    public Mono<ServiceResult> removeApplication(String applicationId, String serverIp) {
        String url = serverIp + BiShengConstant.DELETE_APPLICATION + applicationId;
        return webClient.post()
                .uri(url)
                .header(CommonConstant.COOKIE, BiShengConstant.COOKIE_VALUE)
                .retrieve()
                .bodyToMono(BiShengCreateDatasetResponse.class)
                .flatMap(response -> {
                    if (response.getStatus_code() == 200 || response.getStatus_code() == 10400) {
                        return Mono.fromRunnable(() -> {
                            try {
                                applicationMapper.deleteApplicationById(applicationId);
                            } catch (Exception dbException) {
                                throw new RuntimeException("Database operation failed", dbException);
                            }
                        }).then(Mono.just(new ServiceResult(new ResultHeader(200, "delete success"))));
                    } else {
                        return Mono.just(new ServiceResult(new ResultHeader(500, response.getStatus_message())));
                    }
                })
                .onErrorResume(e -> {
                    log.error("Error occurred while delete dataset: {}", e.getMessage());
                    return Mono.just(new ServiceResult(new ResultHeader(500, "delete failed")));
                });
    }

    @Override
    public Mono<ServiceResult> editApplication(Application application, String serverIp) {
        String url = serverIp + BiShengConstant.GET_APPLICATION_URL + application.getApplicationId();
        return webClient.get()
                .uri(url)
                .header(CommonConstant.COOKIE, BiShengConstant.COOKIE_VALUE)
                .retrieve()
                .bodyToMono(BiShengCreateDatasetResponse.class)
                .flatMap(createResponse -> {
                    JSONObject data = createResponse.getData();
                    if (data == null) {
                        return Mono.just(new ServiceResult(new ResultHeader(createResponse.getStatus_code(), createResponse.getStatus_message())));
                    }
                    data.put("desc", application.getApplicationDescription());
                    data.put("name", application.getApplicationName());
                    data.put("update_time", TimeUtil.getNowTime());
                    List<Integer> list = new ArrayList<>();
                    list.add(Integer.valueOf(application.getKnowledgeBaseId()));
                    data.put("knowledge_list", list);
                    data.put("model_name", application.getLargeModelId());
                    data.put("temperature", application.getTemperature() / 10);
                    data.put("prompt", application.getPrompt());
                    data.put("guide_word", application.getOpeningRemarks());
                    return webClient.put()
                            .uri(serverIp + BiShengConstant.APPLICATION_URL)
                            .contentType(APPLICATION_JSON)
                            .header(CommonConstant.COOKIE, BiShengConstant.COOKIE_VALUE)
                            .bodyValue(data)
                            .retrieve()
                            .bodyToMono(BiShengCreateDatasetResponse.class)
                            .flatMap(updateResponse -> {
                                if (updateResponse.getStatus_code() == 200) {
                                    applicationMapper.insertApplication(application);
                                    return Mono.just(new ServiceResult(new ResultHeader(200, "Application update successfully")));
                                }
                                log.error("error is {}", updateResponse.getStatus_message());
                                return Mono.just(new ServiceResult(new ResultHeader(updateResponse.getStatus_code(), "Application update failed")));
                            });
                }).onErrorResume(e -> {
                    log.error("Error occurred while update application: {}", e.getMessage());
                    return Mono.just(new ServiceResult(new ResultHeader(500, "Application update failed")));
                });
    }
}
