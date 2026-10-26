package com.spotifyclone.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.spotifyclone.config.AppProperties;
import com.spotifyclone.web.GlobalExceptionHandler.ApiException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

@Component
public class YouTubeApiClient implements YouTubeClient {
    private static final Logger log = LoggerFactory.getLogger(YouTubeApiClient.class);
    private static final Set<String> QUOTA_REASONS =
            Set.of("quotaExceeded", "dailyLimitExceeded", "rateLimitExceeded", "userRateLimitExceeded");
    private static final int MAX_RESULTS = 15;

    private final RestClient http;
    private final AppProperties props;
    private final ObjectMapper mapper = new ObjectMapper();

    @Autowired
    public YouTubeApiClient(AppProperties props, RestClient.Builder builder) {
        this(props, withTimeouts(builder).baseUrl(props.youtubeApiBase()).build());
    }

    /** For tests: supply a pre-built RestClient (e.g. bound to a mock server). */
    public YouTubeApiClient(AppProperties props, RestClient http) {
        this.props = props;
        this.http = http;
    }

    private static RestClient.Builder withTimeouts(RestClient.Builder builder) {
        var factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout((int) Duration.ofSeconds(5).toMillis());
        factory.setReadTimeout((int) Duration.ofSeconds(8).toMillis());
        return builder.requestFactory(factory);
    }

    @Override
    public List<YouTubeVideo> searchMusic(String query) {
        JsonNode search = get(b -> b.path("/search")
                .queryParam("part", "snippet")
                .queryParam("type", "video")
                .queryParam("videoCategoryId", "10")
                .queryParam("videoEmbeddable", "true")
                .queryParam("maxResults", MAX_RESULTS)
                .queryParam("q", "{q}")
                .build(query));
        List<String> ids = new ArrayList<>();
        for (JsonNode item : search.path("items")) {
            String id = item.path("id").path("videoId").asText("");
            if (!id.isEmpty()) {
                ids.add(id);
            }
        }
        if (ids.isEmpty()) {
            return List.of();
        }
        return videos(ids).stream().filter(YouTubeVideo::embeddable).toList();
    }

    @Override
    public Optional<YouTubeVideo> findVideo(String videoId) {
        return videos(List.of(videoId)).stream().findFirst();
    }

    // videos.list also gives plain-text titles (search.list titles contain HTML entities).
    private List<YouTubeVideo> videos(List<String> ids) {
        JsonNode body = get(b -> b.path("/videos")
                .queryParam("part", "snippet,contentDetails,status")
                .queryParam("id", String.join(",", ids))
                .build());
        List<YouTubeVideo> byId = new ArrayList<>();
        for (JsonNode v : body.path("items")) {
            String id = v.path("id").asText("");
            JsonNode sn = v.path("snippet");
            JsonNode thumbs = sn.path("thumbnails");
            String thumb = thumbs.path("medium").path("url").asText(
                    thumbs.path("default").path("url").asText("https://i.ytimg.com/vi/" + id + "/mqdefault.jpg"));
            byId.add(new YouTubeVideo(id, sn.path("title").asText(""), sn.path("channelTitle").asText(""), thumb,
                    parseSeconds(v.path("contentDetails").path("duration").asText("")),
                    v.path("status").path("embeddable").asBoolean(false)));
        }
        // keep the relevance order returned by search
        List<YouTubeVideo> ordered = new ArrayList<>();
        for (String id : ids) {
            byId.stream().filter(v -> v.id().equals(id)).findFirst().ifPresent(ordered::add);
        }
        return ordered;
    }

    static int parseSeconds(String iso) {
        try {
            return (int) Math.min(Integer.MAX_VALUE, Duration.parse(iso).getSeconds());
        } catch (RuntimeException e) {
            return 0;
        }
    }

    private JsonNode get(java.util.function.Function<org.springframework.web.util.UriBuilder, java.net.URI> uri) {
        String key = props.youtubeApiKey();
        if (key == null || key.isBlank()) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE,
                    "YouTube is not configured on the server (YOUTUBE_API_KEY is missing)");
        }
        try {
            String json = http.get().uri(uri).header("x-goog-api-key", key).retrieve().body(String.class);
            return mapper.readTree(json == null ? "{}" : json);
        } catch (RestClientResponseException e) {
            throw map(e);
        } catch (RestClientException | java.io.IOException e) {
            // never log the exception message/URL verbatim; keep the key out of logs
            log.warn("YouTube API unreachable: {}", e.getClass().getSimpleName());
            throw new ApiException(HttpStatus.BAD_GATEWAY, "Could not reach the YouTube API");
        }
    }

    private ApiException map(RestClientResponseException e) {
        String reason = "";
        try {
            reason = mapper.readTree(e.getResponseBodyAsString()).path("error").path("errors").path(0)
                    .path("reason").asText("");
        } catch (Exception ignored) {
            // body is not JSON
        }
        int status = e.getStatusCode().value();
        log.warn("YouTube API returned {} ({})", status, reason);
        if (QUOTA_REASONS.contains(reason) || status == 429) {
            return new ApiException(HttpStatus.TOO_MANY_REQUESTS, "YouTube API quota exceeded. Try again later.");
        }
        if (status == 400 || status == 403) {
            return new ApiException(HttpStatus.BAD_GATEWAY,
                    "YouTube API rejected the request (check YOUTUBE_API_KEY and that YouTube Data API v3 is enabled)");
        }
        return new ApiException(HttpStatus.BAD_GATEWAY, "YouTube API error");
    }
}
