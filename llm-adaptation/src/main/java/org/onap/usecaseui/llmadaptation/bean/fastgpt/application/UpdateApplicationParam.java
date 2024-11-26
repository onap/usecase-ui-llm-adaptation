package org.onap.usecaseui.llmadaptation.bean.fastgpt.application;

import lombok.Data;

@Data
public class UpdateApplicationParam {
    private String avatar;

    private int defaultPermission;

    private String intro;

    private String name;
}
