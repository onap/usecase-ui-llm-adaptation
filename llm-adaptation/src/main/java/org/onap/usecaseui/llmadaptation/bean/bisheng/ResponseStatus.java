package org.onap.usecaseui.llmadaptation.bean.bisheng;

import lombok.Data;

@Data
public class ResponseStatus {
    private int status_code;

    private String status_message;

    private String detail;
}
