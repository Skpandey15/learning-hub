package com.learninghub.study;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "generation.worker.enabled", havingValue = "true", matchIfMissing = true)
public class GenerationWorker {
    private final StudyPlatformService service;
    GenerationWorker(StudyPlatformService service) { this.service = service; }

    @Scheduled(fixedDelayString = "${generation.poll-delay-ms:2000}")
    public void processNext() {
        var jobId = service.claimNextJob();
        if (jobId != null) service.generate(jobId);
    }
}
