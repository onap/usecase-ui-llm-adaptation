package org.onap.usecaseui.llmadaptation.bean.fastgpt.application;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class ChatConfig {
    private boolean questionGuide;

    private Map<String, Object> ttsConfig;

    private Map<String, Object> whisperConfig;

    private Map<String, Object> scheduledTriggerConfig;

    private Map<String, Object> chatInputGuide;

    private String instruction;

    private List<String> variables;

    private String welcomeText;
}
