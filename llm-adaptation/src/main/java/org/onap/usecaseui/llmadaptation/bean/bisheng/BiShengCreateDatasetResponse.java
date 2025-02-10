package org.onap.usecaseui.llmadaptation.bean.bisheng;

import com.alibaba.fastjson2.JSONObject;
import lombok.Data;

@Data
public class BiShengCreateDatasetResponse extends ResponseStatus {
    private JSONObject data;

    @Override
    public String toString() {
        return "BiShengCreateDatasetResponse{" +
                "data=" + data +
                "} " + super.toString();
    }
}
