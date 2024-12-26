package org.onap.usecaseui.llmadaptation.service;

import org.onap.usecaseui.llmadaptation.bean.MaaSPlatform;
import org.onap.usecaseui.llmadaptation.bean.Operator;
import org.onap.usecaseui.llmadaptation.bean.ServiceResult;

import java.util.List;

public interface MaaSService {
    List<Operator> getAllMaaSPlatform();

    ServiceResult registerMaaSPlatform(MaaSPlatform maaSPlatform);
}
