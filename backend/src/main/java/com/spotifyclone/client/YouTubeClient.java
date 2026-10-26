package com.spotifyclone.client;

import java.util.List;
import java.util.Optional;

/** Thin gateway to the YouTube Data API v3. Implementations throw ApiException on failure. */
public interface YouTubeClient {

    /** Embeddable music videos matching the query, with duration, in relevance order. */
    List<YouTubeVideo> searchMusic(String query);

    /** A single video, or empty if it does not exist / is private. */
    Optional<YouTubeVideo> findVideo(String videoId);
}
