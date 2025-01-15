package org.onap.usecaseui.llmadaptation.service;

import org.onap.usecaseui.llmadaptation.bean.KnowledgeBase;
import org.onap.usecaseui.llmadaptation.bean.MaaSPlatform;
import org.onap.usecaseui.llmadaptation.bean.ServiceResult;
import org.springframework.http.codec.multipart.FilePart;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface BiShengDatasetService {
    Mono<ServiceResult> createDataset(Flux<FilePart> fileParts, String metaData, MaaSPlatform maaSPlatform);

    Mono<ServiceResult> removeDataset(String knowledgeBaseId,  String serverIp);

    Mono<ServiceResult> editDataset(KnowledgeBase knowledgeBase, MaaSPlatform maaSPlatform);

    Mono<ServiceResult> uploadFiles(Flux<FilePart> fileParts, String knowledgeBaseId,  String serverIp);

    Mono<ServiceResult> deleteFile(String fileId, String serverIp);
}
