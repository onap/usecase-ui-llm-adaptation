package org.onap.usecaseui.llmadaptation.bean.fastgpt.application;

import lombok.Data;

import java.util.List;

@Data
public class PublishApplicationParam {
    private List<Module> nodes;

    private List<Edge> edges;

    private ChatConfig chatConfig;

    private String type;

    private boolean isPublish;

    private String versionName;

    public boolean isIsPublish() {
        return isPublish;
    }

    public void setIsPublish(boolean publish) {
        isPublish = publish;
    }
}
