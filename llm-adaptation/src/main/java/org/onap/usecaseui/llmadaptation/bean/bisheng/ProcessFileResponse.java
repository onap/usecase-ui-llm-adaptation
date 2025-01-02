package org.onap.usecaseui.llmadaptation.bean.bisheng;

import com.alibaba.fastjson2.JSONObject;
import lombok.Data;

import java.util.List;


@Data
public class ProcessFileResponse extends ResponseStatus{
    private List<JSONObject> data;
}
