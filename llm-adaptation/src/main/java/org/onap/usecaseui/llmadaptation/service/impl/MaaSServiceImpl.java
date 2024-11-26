package org.onap.usecaseui.llmadaptation.service.impl;

import com.alibaba.fastjson2.JSONObject;
import io.netty.util.internal.StringUtil;
import lombok.extern.slf4j.Slf4j;
import org.onap.usecaseui.llmadaptation.bean.MaaSPlatform;
import org.onap.usecaseui.llmadaptation.bean.ModelInformation;
import org.onap.usecaseui.llmadaptation.bean.Operator;
import org.onap.usecaseui.llmadaptation.bean.fastgpt.CreateDataSetResponse;
import org.onap.usecaseui.llmadaptation.mapper.MaaSPlatformMapper;
import org.onap.usecaseui.llmadaptation.service.MaaSService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
public class MaaSServiceImpl implements MaaSService {
    @Autowired
    private WebClient webClient;

    @Autowired
    private MaaSPlatformMapper maaSPlatformMapper;

    public List<Operator> getAllMaaSPlatform() {
        List<Operator> operatorList = new ArrayList<>();
        List<MaaSPlatform> maaSPlatforms = maaSPlatformMapper.getMaaSPlatforms();
        Map<String, List<MaaSPlatform>> collect = maaSPlatforms.stream().collect(Collectors.groupingBy(MaaSPlatform::getOperatorId));
        collect.forEach((id, maaSPlatformList) -> {
            Operator operator = new Operator();
            operator.setOperatorId(id);
            operator.setOperatorName(maaSPlatformList.get(0).getOperatorName());
            maaSPlatformList.forEach(maaSPlatform -> {
                List<ModelInformation> modelList = maaSPlatformMapper.getModelList(maaSPlatform.getMaaSPlatformId());
                maaSPlatform.setModelList(modelList);
                maaSPlatform.setOperatorName(null);
                maaSPlatform.setOperatorId(null);
            });
            operator.setMaaSPlatformList(maaSPlatformList);
            operatorList.add(operator);
        });
        return operatorList;
    }

    public Mono<String> loginFastGpt() {
        JSONObject param = new JSONObject();
        param.put("username", "root");
        String password = sha256("1234");
        log.info(password);
        param.put("password", password);

        return webClient.post()
                .uri("http://172.22.16.126:3000/api/support/user/account/loginByPassword")
                .bodyValue(param)
                .retrieve()
                .bodyToMono(CreateDataSetResponse.class)
                .flatMap(response -> {
                    int code = response.getCode();
                    if (code == 200) {
                        Object data = response.getData();
                        JSONObject jsonObject = JSONObject.parseObject(JSONObject.toJSONString(data));
                        String token = jsonObject.getString("token");
                        maaSPlatformMapper.insertCookie("fastGpt", token);
                        return Mono.just(token);
                    } else {
                        return Mono.just(response.getMessage());
                    }
                })
                .onErrorResume(throwable -> {
                    log.error("An error occurred", throwable);
                    return Mono.just("Network Error");
                });
    }

    private String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(input.getBytes());

            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }
}
