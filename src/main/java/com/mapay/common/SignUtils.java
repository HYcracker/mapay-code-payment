package com.mapay.common;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Map;
import java.util.TreeMap;

public final class SignUtils {
    private SignUtils() {
    }

    public static String sign(Map<String, ?> params, String key) {
        TreeMap<String, Object> sorted = new TreeMap<>();
        params.forEach((k, v) -> {
            if (!"sign".equals(k) && !"sign_type".equals(k) && v != null && !String.valueOf(v).isBlank()) {
                sorted.put(k, v);
            }
        });
        StringBuilder source = new StringBuilder();
        sorted.forEach((k, v) -> source.append(k).append('=').append(v).append('&'));
        if (!source.isEmpty()) {
            source.setLength(source.length() - 1);
        }
        source.append(key == null ? "" : key);
        return md5(source.toString());
    }

    public static String md5(String value) {
        return digest(value, "MD5");
    }

    public static String sha256(String value) {
        return digest(value, "SHA-256");
    }

    private static String digest(String value, String algorithm) {
        try {
            MessageDigest digest = MessageDigest.getInstance(algorithm);
            byte[] bytes = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : bytes) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (Exception e) {
            throw new IllegalStateException(algorithm + " unavailable", e);
        }
    }
}
