package com.learninghub.shared.error;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

public final class ErrorFingerprint {
    private ErrorFingerprint() {}

    public static String of(Throwable throwable) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update(throwable.getClass().getName().getBytes(StandardCharsets.UTF_8));
            for (StackTraceElement element : throwable.getStackTrace()) {
                digest.update(element.getClassName().getBytes(StandardCharsets.UTF_8));
                digest.update(element.getMethodName().getBytes(StandardCharsets.UTF_8));
                digest.update(Integer.toString(element.getLineNumber()).getBytes(StandardCharsets.UTF_8));
            }
            return HexFormat.of().formatHex(digest.digest(), 0, 8);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 must be available", exception);
        }
    }
}
