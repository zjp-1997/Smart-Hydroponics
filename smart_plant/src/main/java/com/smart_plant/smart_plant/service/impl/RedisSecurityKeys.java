package com.smart_plant.smart_plant.service.impl;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/** Redis Key 只保存不可逆摘要，避免账号、IP 和令牌标识直接暴露在运维界面。 */
final class RedisSecurityKeys {

    private RedisSecurityKeys() {
    }

    static String key(String prefix, String category, Object identifier) {
        return prefix + ':' + category + ':' + sha256(String.valueOf(identifier));
    }

    private static String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("JVM 不支持 SHA-256", exception);
        }
    }
}
