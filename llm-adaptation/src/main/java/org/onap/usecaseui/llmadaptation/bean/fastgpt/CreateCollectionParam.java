package org.onap.usecaseui.llmadaptation.bean.fastgpt;

import lombok.Data;

@Data
public class CreateCollectionParam {
    private String trainingType;

    private String datasetId;

    private int chunkSize;

    private String chunkSplitter;

    private String fileId;

    private String name;

    private String qaPrompt;
}
