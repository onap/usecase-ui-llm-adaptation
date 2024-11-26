package org.onap.usecaseui.llmadaptation.bean;

import lombok.Data;

@Data
public class Application {
    private String applicationId;

    private String applicationName;

    private String applicationDescription;

    private String applicationType;

    private String operatorId;

    private String operatorName;

    private String maaSPlatformId;

    private String maaSPlatformName;

    private String knowledgeBaseName;

    private String knowledgeBaseId;

    private String largeModelName;

    private String largeModelId;

    private String prompt;

    private float temperature;

    private float top_p;

    private String openingRemarks;
}
