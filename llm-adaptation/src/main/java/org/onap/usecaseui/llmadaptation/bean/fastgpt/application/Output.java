package org.onap.usecaseui.llmadaptation.bean.fastgpt.application;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Output {
    private String id;

    private String key;

    private String label;

    private String type;

    private String valueType;

    private String description;

    private String valueDesc;

    private boolean required;
}
