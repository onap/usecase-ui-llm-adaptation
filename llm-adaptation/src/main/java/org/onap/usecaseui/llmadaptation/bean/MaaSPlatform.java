package org.onap.usecaseui.llmadaptation.bean;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.util.List;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MaaSPlatform {
    private String maaSPlatformId;

    private String maaSPlatformName;

    private String operatorId;

    private String operatorName;

    private String maaSType;

    private List<ModelInformation> modelList;

}
