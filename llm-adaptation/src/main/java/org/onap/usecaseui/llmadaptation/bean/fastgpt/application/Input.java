package org.onap.usecaseui.llmadaptation.bean.fastgpt.application;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.util.List;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Input {
    private String key;

    private List<String> renderTypeList;

    private String valueType;

    private String label;

    private Object value;

    private String debugLabel;

    private String toolDescription;

    private boolean required;

    private String description;

    private String placeholder;

    private int min;

    private int max;

    private int step;

    private List<String> list;
}
