package org.onap.usecaseui.llmadaptation.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.onap.usecaseui.llmadaptation.bean.Application;

import java.util.List;

@Mapper
public interface FastGptApplicationMapper {
    List<Application> getAllApplication();

    int insertApplication(@Param(value = "application") Application application);

    int deleteApplicationById(@Param(value = "applicationId") String applicationId);

    Application getApplicationById(@Param(value = "applicationId") String applicationId);
}
