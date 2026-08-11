package com.learninghub.shared.error;

import java.util.Map;

public class ApiException extends RuntimeException {
    private final ErrorCode errorCode;
    private final Map<String, Object> safeProperties;

    public ApiException(ErrorCode errorCode) {
        this(errorCode, Map.of(), null);
    }

    public ApiException(ErrorCode errorCode, Map<String, Object> safeProperties) {
        this(errorCode, safeProperties, null);
    }

    public ApiException(ErrorCode errorCode, Map<String, Object> safeProperties, Throwable cause) {
        super(errorCode.name(), cause, true, false);
        this.errorCode = errorCode;
        this.safeProperties = Map.copyOf(safeProperties);
    }

    public ErrorCode errorCode() {
        return errorCode;
    }

    public Map<String, Object> safeProperties() {
        return safeProperties;
    }
}
