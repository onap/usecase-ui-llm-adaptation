package org.onap.usecaseui.llmadaptation.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.onap.usecaseui.llmadaptation.bean.*;
import org.onap.usecaseui.llmadaptation.mapper.MaaSPlatformMapper;
import org.onap.usecaseui.llmadaptation.service.MaaSService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
public class MaaSServiceImpl implements MaaSService {

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

    @Override
    public ServiceResult registerMaaSPlatform(MaaSPlatform maaSPlatform) {
        String maaSPlatformId = maaSPlatform.getMaaSPlatformId();
        MaaSPlatform maaSPlatformById = maaSPlatformMapper.getMaaSPlatformById(maaSPlatformId);
        if (maaSPlatformById != null) {
            return new ServiceResult(new ResultHeader(500, maaSPlatform.getMaaSPlatformName() + "already exists"));
        }
        List<ModelInformation> modelList = maaSPlatform.getModelList();
        for (ModelInformation model : modelList) {
            ModelInformation modelById = maaSPlatformMapper.getModelById(model.getModelId(), maaSPlatformId);
            if (modelById != null) {
                return new ServiceResult(new ResultHeader(500, model.getModelName() + " already exists"));
            }
        }
        maaSPlatformMapper.insertMaaSPlatform(maaSPlatform);
        maaSPlatformMapper.insertModel(maaSPlatform.getMaaSPlatformId(), maaSPlatform.getModelList());
        return new ServiceResult(new ResultHeader(200, "register success"));
    }

    @Override
    public ServiceResult deleteMaaSPlatform(String maaSPlatformId) {
        MaaSPlatform maaSPlatformById = maaSPlatformMapper.getMaaSPlatformById(maaSPlatformId);
        if (maaSPlatformById == null) {
            return new ServiceResult(new ResultHeader(500, maaSPlatformId + "does not exist"));
        }
        if (maaSPlatformMapper.deleteMaaSPlatformById(maaSPlatformId) < 1) {
            return new ServiceResult(new ResultHeader(500, " delete failed"));
        }
        if (maaSPlatformMapper.deleteModelByMaaSPlatformId(maaSPlatformId) < 1) {
            return new ServiceResult(new ResultHeader(500, " delete failed"));
        }
        return new ServiceResult(new ResultHeader(200, " delete success"));
    }
}
