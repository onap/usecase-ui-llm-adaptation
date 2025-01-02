package org.onap.usecaseui.llmadaptation.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.onap.usecaseui.llmadaptation.bean.KnowledgeBase;

import java.util.List;

@Mapper
public interface DatasetMapper {
    int insertKnowledgeBaseRecord(@Param(value = "knowledgeBase") KnowledgeBase knowledgeBase);

    int insertFileName(@Param(value = "fileId") String fileId,@Param(value = "fileName") String fileName,@Param(value = "knowledgeBaseId") String knowledgeBaseId);

    List<KnowledgeBase> getKnowledgeBaseRecords();

    List<String> getFileNamesByKnowledgeBaseId(@Param(value = "knowledgeBaseId") String knowledgeBaseId);

    KnowledgeBase getKnowledgeBaseRecordById(@Param(value = "knowledgeBaseId") String knowledgeBaseId);

    int deleteKnowledgeBaseByUuid(@Param(value = "knowledgeBaseId") String knowledgeBaseId);

    List<KnowledgeBase> getKnowledgeBaseByMaaSId(@Param(value = "maaSPlatformId") String maaSPlatformId);

    int deleteFileById(@Param(value = "knowledgeBaseId") String knowledgeBaseId);

    int updateKnowledgeBase(@Param(value = "knowledgeBase") KnowledgeBase knowledgeBase);
}
