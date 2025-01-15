package org.onap.usecaseui.llmadaptation.bean.fastgpt.dataset;

import lombok.Data;

@Data
public class CreateDataSetParam {
    private String type;

    private String name;

    private String intro;

    private String agentModel;
}
