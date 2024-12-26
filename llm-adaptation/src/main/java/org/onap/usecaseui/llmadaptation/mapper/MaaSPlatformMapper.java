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

    MaaSPlatform getMaaSPlatformById(@Param(value = "maaSPlatformId") String maaSPlatformId);

    ModelInformation getModelById(@Param(value = "modelId") String modelId);
}
