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
}
