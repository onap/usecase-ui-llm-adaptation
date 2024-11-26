package org.onap.usecaseui.llmadaptation.bean;

import lombok.Data;

import java.util.List;

@Data
public class Operator {
    private String operatorId;

    private String operatorName;

    private List<MaaSPlatform> maaSPlatformList;
}
