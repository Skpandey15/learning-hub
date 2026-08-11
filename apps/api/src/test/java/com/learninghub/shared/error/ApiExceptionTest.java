package com.learninghub.shared.error;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.Test;

class ApiExceptionTest {
    @Test
    void constructorsExposeOnlyStableErrorMetadata() {
        ApiException simple = new ApiException(ErrorCode.RESOURCE_NOT_FOUND);
        ApiException withProperties = new ApiException(
                ErrorCode.CONFLICT, Map.of("version", 2));

        assertThat(simple.errorCode()).isEqualTo(ErrorCode.RESOURCE_NOT_FOUND);
        assertThat(simple.safeProperties()).isEmpty();
        assertThat(simple.getMessage()).isEqualTo("RESOURCE_NOT_FOUND");
        assertThat(withProperties.safeProperties()).containsEntry("version", 2);
    }

    @Test
    void fingerprintsAreStableForEmptyAndPopulatedStacks() {
        RuntimeException populated = new RuntimeException("not included");
        RuntimeException empty = new RuntimeException("not included");
        empty.setStackTrace(new StackTraceElement[0]);

        assertThat(ErrorFingerprint.of(populated)).hasSize(16);
        assertThat(ErrorFingerprint.of(populated)).isEqualTo(ErrorFingerprint.of(populated));
        assertThat(ErrorFingerprint.of(empty)).hasSize(16);
    }

    @Test
    void errorCodesExposeStableProblemMetadata() {
        for (ErrorCode code : ErrorCode.values()) {
            assertThat(code.status()).isNotNull();
            assertThat(code.title()).isNotBlank();
            assertThat(code.detail()).isNotBlank();
            assertThat(code.type()).startsWith("https://learninghub.dev/problems/");
        }
    }
}
