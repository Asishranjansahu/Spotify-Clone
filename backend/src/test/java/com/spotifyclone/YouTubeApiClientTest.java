package com.spotifyclone;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.spotifyclone.client.YouTubeApiClient;
import com.spotifyclone.client.YouTubeVideo;
import com.spotifyclone.config.AppProperties;
import com.spotifyclone.web.GlobalExceptionHandler.ApiException;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

/** Exercises the real HTTP client against a mocked YouTube Data API. */
class YouTubeApiClientTest {
    private static final String BASE = "https://yt.test/youtube/v3";

    private static AppProperties props(String key) {
        return new AppProperties("x", 1, "http://x", "s", 1, 1, "a", "p", false, key, BASE, 60);
    }

    @Test
    void searchUsesMusicEmbeddableFiltersKeyHeaderAndParsesDuration() {
        var builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        var client = new YouTubeApiClient(props("secret-key"), builder.baseUrl(BASE).build());

        server.expect(requestTo(org.hamcrest.Matchers.allOf(
                        org.hamcrest.Matchers.startsWith(BASE + "/search?"),
                        org.hamcrest.Matchers.containsString("type=video"),
                        org.hamcrest.Matchers.containsString("videoCategoryId=10"),
                        org.hamcrest.Matchers.containsString("videoEmbeddable=true"),
                        org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("secret-key")))))
                .andExpect(header("x-goog-api-key", "secret-key"))
                .andRespond(withSuccess("{\"items\":[{\"id\":{\"videoId\":\"v1\"}},{\"id\":{\"videoId\":\"v2\"}}]}",
                        MediaType.APPLICATION_JSON));
        server.expect(requestTo(org.hamcrest.Matchers.containsString("/videos?")))
                .andRespond(withSuccess("""
                        {"items":[
                         {"id":"v2","snippet":{"title":"B & C","channelTitle":"Ch2","thumbnails":{"medium":{"url":"https://i.ytimg.com/b.jpg"}}},
                          "contentDetails":{"duration":"PT1H2M3S"},"status":{"embeddable":true}},
                         {"id":"v1","snippet":{"title":"A","channelTitle":"Ch1","thumbnails":{}},
                          "contentDetails":{"duration":"PT3M5S"},"status":{"embeddable":false}}]}
                        """, MediaType.APPLICATION_JSON));

        List<YouTubeVideo> out = client.searchMusic("a b");

        // v1 is not embeddable and is dropped; v2 keeps plain-text title and parsed duration
        assertThat(out).hasSize(1);
        assertThat(out.get(0).id()).isEqualTo("v2");
        assertThat(out.get(0).title()).isEqualTo("B & C");
        assertThat(out.get(0).durationSec()).isEqualTo(3723);
        server.verify();
    }

    @Test
    void quotaExceededMapsTo429() {
        var builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo(org.hamcrest.Matchers.containsString("/search")))
                .andRespond(withStatus(HttpStatus.FORBIDDEN).contentType(MediaType.APPLICATION_JSON)
                        .body("{\"error\":{\"errors\":[{\"reason\":\"quotaExceeded\"}]}}"));
        var client = new YouTubeApiClient(props("k"), builder.baseUrl(BASE).build());

        assertThatThrownBy(() -> client.searchMusic("x"))
                .isInstanceOfSatisfying(ApiException.class, e -> {
                    assertThat(e.status).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
                    assertThat(e.getMessage()).containsIgnoringCase("quota");
                });
    }

    @Test
    void missingKeyMapsTo503WithoutCallingYouTube() {
        var builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        var client = new YouTubeApiClient(props(""), builder.baseUrl(BASE).build());

        assertThatThrownBy(() -> client.searchMusic("x"))
                .isInstanceOfSatisfying(ApiException.class, e -> {
                    assertThat(e.status).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
                    assertThat(e.getMessage()).contains("YOUTUBE_API_KEY");
                });
        server.verify(); // no requests were expected or made
    }
}
