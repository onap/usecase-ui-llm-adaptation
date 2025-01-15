package org.onap.usecaseui.llmadaptation.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.onap.usecaseui.llmadaptation.bean.File;
import org.onap.usecaseui.llmadaptation.bean.KnowledgeBase;

import java.util.List;

@Mapper
public interface DatasetMapper {
    int insertKnowledgeBaseRecord(@Param(value = "knowledgeBase") KnowledgeBase knowledgeBase);

    int insertFileName(@Param(value = "files") List<File> files, @Param(value = "knowledgeBaseId") String knowledgeBaseId);

    List<KnowledgeBase> getKnowledgeBaseRecords();

    List<File> getFileNamesByKnowledgeBaseId(@Param(value = "knowledgeBaseId") String knowledgeBaseId);

    KnowledgeBase getKnowledgeBaseRecordById(@Param(value = "knowledgeBaseId") String knowledgeBaseId);

    int deleteKnowledgeBaseByUuid(@Param(value = "knowledgeBaseId") String knowledgeBaseId);

    List<KnowledgeBase> getKnowledgeBaseByMaaSId(@Param(value = "maaSPlatformId") String maaSPlatformId);

    int deleteFileById(@Param(value = "knowledgeBaseId") String knowledgeBaseId);

    int updateKnowledgeBase(@Param(value = "knowledgeBase") KnowledgeBase knowledgeBase);

    int deleteFileByFileId(@Param(value = "fileId") String fileId);

    String getKnowledgeIdByFileId(@Param(value = "fileId") String fileId);
}
