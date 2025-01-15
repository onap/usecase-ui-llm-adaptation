package org.onap.usecaseui.llmadaptation.bean;

import lombok.Data;

@Data
public class File {
    private String fileId;

    private String fileName;

    public File(String fileId, String fileName) {
        this.fileId = fileId;
        this.fileName = fileName;
    }
}
