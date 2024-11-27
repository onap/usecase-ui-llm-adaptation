package org.onap.usecaseui.llmadaptation.constant;

public class FastGptConstant {
    public static final String COOKIE = "Cookie";

    public static final String COOKIE_VALUE = "fastgpt_token=eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJ1c2VySWQiOiI2NzFmNTQ2MGM4Zjc3YTFjMGYzZTUyYmEiLCJ0ZWFtSWQiOiI2NzFmNTQ2MGM4Zjc3YTFjMGYzZTUyYzAiLCJ0bWJJZCI6IjY3MWY1NDYwYzhmNzdhMWMwZjNlNTJjMiIsImlzUm9vdCI6dHJ1ZSwiZXhwIjoxNzMzMjc0Mzc2LCJpYXQiOjE3MzI2Njk1NzZ9.NdJ_ShISQOa1f5AvGsfq8Zrh4g4e2JwtX1TZ2iCLN6I";

    public static final String CREATE_DATASET_URL = "http://172.22.16.126:3000/api/core/dataset/create";

    public static final String UPLOAD_FILE_URL = "http://172.22.16.126:3000/api/common/file/upload";

    public static final String CRATE_COLLECTION_URL = "http://172.22.16.126:3000/api/core/dataset/collection/create/fileId";

    public static final String DELETE_DATASET_URL = "http://172.22.16.126:3000/api/core/dataset/delete?id=";

    public static final String CREATE_APPLICATION = "http://172.22.16.126:3000/api/core/app/create";

    public static final String UPDATE_APPLICATION = "http://172.22.16.126:3000/api/core/app/update?appId=";

    public static final String PUBLISH_APPLICATION = "http://172.22.16.126:3000/api/core/app/version/publish?appId=";

    public static final String CREATE_APP_PARAM_FILE_URL = "classpath:Param/createApplication.json";

    public static final String PUBLISH_APP_PARAM_FILE_URL = "classpath:Param/publishApplication.json";

    public static final String APPLICATION_CHAT_URL = "http://172.22.16.126:3000/api/v1/chat/completions";

    public static final String DELETE_APPLICATION = "http://172.22.16.126:3000/api/core/app/del?appId=";
}
