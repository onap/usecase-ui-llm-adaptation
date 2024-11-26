package org.onap.usecaseui.llmadaptation.bean;

import lombok.Data;

import java.util.List;

@Data
public class KnowledgeBase {
    private String knowledgeBaseId;

    private String knowledgeBaseName;

    private String knowledgeBaseDescription;

    private String operatorId;

    private String operatorName;

    private String maaSPlatformId;

    private String maaSPlatformName;

    private String updateTime;

    private List<String> filesName;
}
