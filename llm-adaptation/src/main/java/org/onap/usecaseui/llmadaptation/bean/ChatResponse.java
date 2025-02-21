package org.onap.usecaseui.llmadaptation.bean;

import lombok.Data;

@Data
public class ChatResponse {
    private ResultHeader result_header;

    private String finished;

    private String answer;

    private String reference;
}
