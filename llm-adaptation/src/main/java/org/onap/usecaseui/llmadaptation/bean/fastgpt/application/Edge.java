package org.onap.usecaseui.llmadaptation.bean.fastgpt.application;

import lombok.Data;

@Data
public class Edge {
    private String source;

    private String target;

    private String sourceHandle;

    private String targetHandle;
}
