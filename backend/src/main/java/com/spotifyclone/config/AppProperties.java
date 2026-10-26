package com.spotifyclone.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public record AppProperties(
        String jwtSecret,
        long jwtExpirationMs,
        String corsOrigin,
        String storageDir,
        long maxAudioBytes,
        long maxImageBytes,
        String adminEmail,
        String adminPassword,
        boolean seedDemo,
        String youtubeApiKey,
        String youtubeApiBase,
        long youtubeCacheSeconds) {
}
