package org.onap.usecaseui.llmadaptation.bean.fastgpt.application;

import com.alibaba.fastjson2.JSONObject;
import lombok.Data;

import java.util.List;

@Data
public class ChatParam {
    private String appId;

    private String chatId;

    private boolean detail;

    private boolean stream;

    private String responseChatItemId;

    private JSONObject variables;

    private List<Message> messages;
}
