package org.onap.usecaseui.llmadaptation.bean.fastgpt;

import lombok.Data;

@Data
public class CreateDataSetParam {
    private String type;

    private String avatar;

    private String name;

    private String intro;

    private String agentModel;

    private String vectorModel;
}
