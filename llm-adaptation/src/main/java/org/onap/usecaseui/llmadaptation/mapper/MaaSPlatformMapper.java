package org.onap.usecaseui.llmadaptation.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.onap.usecaseui.llmadaptation.bean.MaaSPlatform;
import org.onap.usecaseui.llmadaptation.bean.ModelInformation;

import java.util.List;

@Mapper
public interface MaaSPlatformMapper {

    int insertMaaSPlatform(@Param(value = "maaSPlatform") MaaSPlatform maaSPlatform);

    int insertModel(@Param(value = "maaSPlatformId") String maaSPlatformId,
                    @Param(value = "modelInformationList") List<ModelInformation> modelInformationList);

    List<MaaSPlatform> getMaaSPlatforms();

    List<ModelInformation> getModelList(@Param(value = "maaSPlatformId") String maaSPlatformId);

    int insertCookie(@Param(value = "name") String name,@Param(value = "cookie") String cookie);

    String getCookie(@Param(value = "name") String name);
}
