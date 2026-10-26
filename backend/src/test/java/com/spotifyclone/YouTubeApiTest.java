package com.spotifyclone;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.spotifyclone.client.YouTubeClient;
import com.spotifyclone.client.YouTubeVideo;
import com.spotifyclone.web.GlobalExceptionHandler.ApiException;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:yt;MODE=PostgreSQL",
        "spring.datasource.username=sa", "spring.datasource.password=",
        "app.storage-dir=target/test-storage-yt"})
@ActiveProfiles("dev")
@AutoConfigureMockMvc
class YouTubeApiTest {
    private static final String VIDEO_ID = "dQw4w9WgXcQ";

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @MockBean YouTubeClient client;

    private static YouTubeVideo video(String id, boolean embeddable) {
        return new YouTubeVideo(id, "Some Song", "Some Channel", "https://i.ytimg.com/vi/" + id + "/mqdefault.jpg",
                213, embeddable);
    }

    private String register(String name) throws Exception {
        String body = "{\"username\":\"%s\",\"email\":\"%s@x.com\",\"password\":\"password123\"}".formatted(name, name);
        String res = mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return json.readTree(res).get("token").asText();
    }

    @Test
    void searchRequiresJwtAndIsCached() throws Exception {
        mvc.perform(get("/api/youtube/search").param("q", "lofi")).andExpect(status().isUnauthorized());

        when(client.searchMusic("lofi beats")).thenReturn(List.of(video(VIDEO_ID, true)));
        String token = register("searcher");
        for (String q : new String[] {"lofi beats", "  LOFI Beats "}) {
            mvc.perform(get("/api/youtube/search").param("q", q).header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].videoId", is(VIDEO_ID)))
                    .andExpect(jsonPath("$[0].title", is("Some Song")))
                    .andExpect(jsonPath("$[0].channelTitle", is("Some Channel")))
                    .andExpect(jsonPath("$[0].thumbnailUrl", startsWith("https://i.ytimg.com/")))
                    .andExpect(jsonPath("$[0].durationSec", is(213)));
        }
        // second (case/whitespace-different) query served from cache
        verify(client, times(1)).searchMusic(anyString());
    }

    @Test
    void searchErrorsAreReturnedAsJson() throws Exception {
        String token = register("erroruser");
        when(client.searchMusic("quota")).thenThrow(
                new ApiException(HttpStatus.TOO_MANY_REQUESTS, "YouTube API quota exceeded. Try again later."));
        when(client.searchMusic("nokey")).thenThrow(new ApiException(HttpStatus.SERVICE_UNAVAILABLE,
                "YouTube is not configured on the server (YOUTUBE_API_KEY is missing)"));
        mvc.perform(get("/api/youtube/search").param("q", "quota").header("Authorization", "Bearer " + token))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.message", containsString("quota")));
        mvc.perform(get("/api/youtube/search").param("q", "nokey").header("Authorization", "Bearer " + token))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.message", containsString("YOUTUBE_API_KEY")));
        mvc.perform(get("/api/youtube/search").param("q", " ").header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createsYoutubeSongOnceAndItWorksWithLibraryFeatures() throws Exception {
        mvc.perform(post("/api/youtube/songs").contentType(MediaType.APPLICATION_JSON)
                .content("{\"videoId\":\"" + VIDEO_ID + "\"}")).andExpect(status().isUnauthorized());

        when(client.findVideo(VIDEO_ID)).thenReturn(Optional.of(video(VIDEO_ID, true)));
        String token = register("ytuser");
        String auth = "Bearer " + token;
        String body = "{\"videoId\":\"" + VIDEO_ID + "\"}";

        String res = mvc.perform(post("/api/youtube/songs").header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.source", is("YOUTUBE")))
                .andExpect(jsonPath("$.externalId", is(VIDEO_ID)))
                .andExpect(jsonPath("$.title", is("Some Song")))
                .andExpect(jsonPath("$.thumbnailUrl", startsWith("https://i.ytimg.com/")))
                .andReturn().getResponse().getContentAsString();
        long id = json.readTree(res).get("id").asLong();

        // second call returns the same row without hitting the YouTube API again
        mvc.perform(post("/api/youtube/songs").header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id", is((int) id)));
        verify(client, times(1)).findVideo(VIDEO_ID);

        // existing playlist / like / history / play-count code works unchanged
        String pl = mvc.perform(post("/api/playlists").header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Mixed\"}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        long pid = json.readTree(pl).get("id").asLong();
        mvc.perform(post("/api/playlists/" + pid + "/songs/" + id).header("Authorization", auth))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/playlists/" + pid).header("Authorization", auth))
                .andExpect(jsonPath("$.songs[0].source", is("YOUTUBE")));
        mvc.perform(post("/api/songs/" + id + "/like").header("Authorization", auth)).andExpect(status().isNoContent());
        mvc.perform(get("/api/me/liked").header("Authorization", auth))
                .andExpect(jsonPath("$[0].externalId", is(VIDEO_ID)));
        mvc.perform(post("/api/history").header("Authorization", auth).contentType(MediaType.APPLICATION_JSON)
                .content("{\"songId\":" + id + "}")).andExpect(status().is2xxSuccessful());
        mvc.perform(post("/api/songs/" + id + "/played").header("Authorization", auth))
                .andExpect(status().isNoContent());

        // once saved, search results expose the song id so the client can reuse it
        when(client.searchMusic("saved")).thenReturn(List.of(video(VIDEO_ID, true)));
        mvc.perform(get("/api/youtube/search").param("q", "saved").header("Authorization", auth))
                .andExpect(jsonPath("$[0].songId", is((int) id)));

        // no audio file to stream, and the local library listing excludes it
        mvc.perform(get("/api/songs/" + id + "/stream").header("Authorization", auth))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/songs")).andExpect(jsonPath("$.content[*].source", everyItem(is("LOCAL"))));
    }

    @Test
    void rejectsBadIdsAndNonEmbeddableVideos() throws Exception {
        String auth = "Bearer " + register("ytbad");
        mvc.perform(post("/api/youtube/songs").header("Authorization", auth)
                .contentType(MediaType.APPLICATION_JSON).content("{\"videoId\":\"not valid!\"}"))
                .andExpect(status().isBadRequest());
        when(client.findVideo("AAAAAAAAAAA")).thenReturn(Optional.of(video("AAAAAAAAAAA", false)));
        when(client.findVideo("BBBBBBBBBBB")).thenReturn(Optional.empty());
        mvc.perform(post("/api/youtube/songs").header("Authorization", auth)
                .contentType(MediaType.APPLICATION_JSON).content("{\"videoId\":\"AAAAAAAAAAA\"}"))
                .andExpect(status().isUnprocessableEntity());
        mvc.perform(post("/api/youtube/songs").header("Authorization", auth)
                .contentType(MediaType.APPLICATION_JSON).content("{\"videoId\":\"BBBBBBBBBBB\"}"))
                .andExpect(status().isNotFound());
    }
}
