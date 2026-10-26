package com.spotifyclone.client;

/** Minimal video metadata returned by the YouTube Data API. */
public record YouTubeVideo(String id, String title, String channelTitle, String thumbnailUrl,
                           int durationSec, boolean embeddable) {}
