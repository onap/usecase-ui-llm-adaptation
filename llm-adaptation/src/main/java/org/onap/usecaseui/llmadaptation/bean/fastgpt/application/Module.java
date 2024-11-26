package org.onap.usecaseui.llmadaptation.bean.fastgpt.application;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.util.List;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Module {
    private String nodeId;

    private String name;

    private String intro;

    private String avatar;

    private String flowNodeType;

    private boolean showStatus;

    private Position position;

    private String version;

    private List<Input> inputs;

    private List<Output> outputs;
}
