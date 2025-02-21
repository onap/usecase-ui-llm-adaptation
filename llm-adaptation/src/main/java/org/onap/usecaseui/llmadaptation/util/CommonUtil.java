package org.onap.usecaseui.llmadaptation.util;

import org.onap.usecaseui.llmadaptation.bean.ChatResponse;
import org.onap.usecaseui.llmadaptation.bean.ResultHeader;
import reactor.core.publisher.Flux;

public class CommonUtil {

    public static Flux<ChatResponse> chatFailed() {
        ChatResponse result = new ChatResponse();
        result.setReference("");
        result.setFinished("error");
        ResultHeader resultHeader = new ResultHeader(500, "failure");
        result.setResult_header(resultHeader);
        result.setAnswer("Network Error");
        return Flux.just(result);
    }
}
