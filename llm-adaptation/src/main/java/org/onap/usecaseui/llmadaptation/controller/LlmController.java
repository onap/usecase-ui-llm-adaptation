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

package org.onap.usecaseui.llmadaptation.controller;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.nimbusds.jose.JOSEException;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.onap.usecaseui.llmadaptation.bean.LargeModelRequestParam;
import org.onap.usecaseui.llmadaptation.util.TokenUtil;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;

@Slf4j
@RestController
public class LlmController {

    @PostMapping(value = "/getHelper")
    public String getHelp(@RequestBody String question) {
        String result = "";
        String url = "http://jiutian.hq.cmcc/largemodel/api/v1/completions?klAssisId=65e6c42ba8a3d22f0366c84d";
        String apiKey = "65e82b2fa8a3d22f03679898.kTywdU/witQJlHdwgWAI+1thI2UUWfHN";
        String token;
        try {
            token = TokenUtil.generateToken(apiKey, 200000);
        } catch (JOSEException e) {
            log.error("error is {}", e.getMessage());
            return result;
        }
        String authorization = "Bearer " + token;

        LargeModelRequestParam helpRequest = new LargeModelRequestParam();
        helpRequest.setPrompt(question);
        helpRequest.setReference(false);
        helpRequest.setStream(false);
        helpRequest.setHistory(new ArrayList<>());
        helpRequest.setTemperature(0.01);

        RequestConfig defaultRequestConfig = RequestConfig.custom()
                .setConnectTimeout(10000)
                .setConnectionRequestTimeout(10000)
                .setSocketTimeout(10000)
                .build();
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpPost httpPost = new HttpPost(url);

            httpPost.setHeader("Content-Type", "application/json");
            httpPost.setHeader("Authorization", authorization);
            httpPost.setConfig(defaultRequestConfig);

            StringEntity requestEntity = new StringEntity(JSON.toJSONString(helpRequest), ContentType.APPLICATION_JSON);
            httpPost.setEntity(requestEntity);

            HttpResponse response = httpClient.execute(httpPost);

            HttpEntity responseEntity = response.getEntity();

            if (responseEntity != null) {
                String responseString = EntityUtils.toString(responseEntity, "utf-8");
                String json = responseString.replaceAll("^data:", "");
                JSONObject jsonObject = JSON.parseObject(json);
                result = jsonObject.getString("response");
                return result;
            }
        } catch (Exception e) {
            log.error("error is {}", e.getMessage());
        }
        return result;
    }
}
