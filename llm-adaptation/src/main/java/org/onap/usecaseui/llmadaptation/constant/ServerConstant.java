package org.onap.usecaseui.llmadaptation.constant;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Getter
public class ServerConstant {

    @Value("${fastGpt.server}")
    private String fastGptServer;

    @Value("${biSheng.server}")
    private String biShengServer;

    @Value("${fastGpt.model}")
    private String fastGptModel;

    @Value("${biSheng.model}")
    private int biShengModel;

    @Value("${fastGpt.maaSType}")
    private String fastGptType;

    @Value("${biSheng.maaSType}")
    private String biShengType;
}
