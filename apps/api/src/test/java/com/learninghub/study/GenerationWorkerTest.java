package com.learninghub.study;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class GenerationWorkerTest {
    @Test
    void processesAClaimedJob() {
        StudyPlatformService service = mock(StudyPlatformService.class);
        UUID jobId = UUID.randomUUID();
        when(service.claimNextJob()).thenReturn(jobId);

        new GenerationWorker(service).processNext();

        verify(service).claimNextJob();
        verify(service).generate(jobId);
    }

    @Test
    void remainsIdleWhenNoJobIsAvailable() {
        StudyPlatformService service = mock(StudyPlatformService.class);

        new GenerationWorker(service).processNext();

        verify(service).claimNextJob();
        verifyNoMoreInteractions(service);
    }
}
