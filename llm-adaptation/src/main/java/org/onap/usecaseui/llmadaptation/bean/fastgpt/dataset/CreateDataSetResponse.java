package org.onap.usecaseui.llmadaptation.bean.fastgpt.dataset;

import lombok.Data;

@Data
public class CreateDataSetResponse {
    private int code;

    private String statusText;

    private String message;

    private Object data;
}
