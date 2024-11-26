package org.onap.usecaseui.llmadaptation.bean.fastgpt.application;

import lombok.Data;

@Data
public class Message {
    private String content;

    private String dataId;

    private String role;
}
