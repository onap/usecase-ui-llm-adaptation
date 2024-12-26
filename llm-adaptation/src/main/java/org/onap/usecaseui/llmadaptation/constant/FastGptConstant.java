package org.onap.usecaseui.llmadaptation.constant;

public class FastGptConstant {
    public static final String COOKIE_VALUE = "fastgpt_token=eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJ1c2VySWQiOiI2NzFmNTQ2MGM4Zjc3YTFjMGYzZTUyYmEiLCJ0ZWFtSWQiOiI2NzFmNTQ2MGM4Zjc3YTFjMGYzZTUyYzAiLCJ0bWJJZCI6IjY3MWY1NDYwYzhmNzdhMWMwZjNlNTJjMiIsImlzUm9vdCI6dHJ1ZSwiZXhwIjoxNzM1NTIzMzY0LCJpYXQiOjE3MzQ5MTg1NjR9.GPomC4qSnz3ADnEIo4sgl8jROaCsomEh9J6kfVAZuBQ";

    public static final String CREATE_DATASET_URL = "/api/core/dataset/create";

    public static final String UPLOAD_FILE_URL = "/api/common/file/upload";

    public static final String CRATE_COLLECTION_URL = "/api/core/dataset/collection/create/fileId";

    public static final String UPDATE_DATASET_URL = "/api/core/dataset/update";

    public static final String DELETE_DATASET_URL = "/api/core/dataset/delete?id=";

    public static final String CREATE_APPLICATION = "/api/core/app/create";

    public static final String UPDATE_APPLICATION = "/api/core/app/update?appId=";

    public static final String PUBLISH_APPLICATION = "/api/core/app/version/publish?appId=";

    public static final String CREATE_APP_PARAM_FILE_URL = "classpath:Param/createApplication.json";

    public static final String PUBLISH_APP_PARAM_FILE_URL = "classpath:Param/publishApplication.json";

    public static final String APPLICATION_CHAT_URL = "/api/v1/chat/completions";

    public static final String DELETE_APPLICATION = "/api/core/app/del?appId=";
}
