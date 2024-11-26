package org.onap.usecaseui.llmadaptation.bean.fastgpt.application;

import lombok.Data;

import java.util.List;

@Data
public class CreateApplicationParam {
    private String parentId;

    private String avatar;

    private String name;

    private String type;

    private List<Module> modules;

    private List<Edge> edges;
}
