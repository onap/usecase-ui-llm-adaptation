package org.onap.usecaseui.llmadaptation.service;

import org.onap.usecaseui.llmadaptation.bean.KnowledgeBase;
import org.onap.usecaseui.llmadaptation.bean.ServiceResult;
import org.springframework.http.codec.multipart.FilePart;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface FastGptDatasetService {
    Mono<ServiceResult> createDataset(Flux<FilePart> fileParts, String metaData);

    Mono<ServiceResult> removeDataset(String knowledgeBaseId);

    Mono<ServiceResult> editDataset(KnowledgeBase knowledgeBase);
}
