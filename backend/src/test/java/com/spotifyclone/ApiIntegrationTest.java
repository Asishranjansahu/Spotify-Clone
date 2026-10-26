package com.spotifyclone;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:test;MODE=PostgreSQL",
        "spring.datasource.username=sa", "spring.datasource.password=",
        "app.storage-dir=target/test-storage"})
@ActiveProfiles("dev")
@AutoConfigureMockMvc
class ApiIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;

    private String register(String name) throws Exception {
        String body = "{\"username\":\"%s\",\"email\":\"%s@x.com\",\"password\":\"password123\"}".formatted(name, name);
        String res = mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return json.readTree(res).get("token").asText();
    }

    @Test
    void protectedEndpointRequiresAuth() throws Exception {
        mvc.perform(get("/api/playlists")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/songs/1/stream")).andExpect(status().isUnauthorized());
    }

    @Test
    void duplicateRegistrationRejected() throws Exception {
        register("dupe");
        String body = "{\"username\":\"dupe2\",\"email\":\"dupe@x.com\",\"password\":\"password123\"}";
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isConflict());
    }

    @Test
    void streamSupportsRangeRequests() throws Exception {
        String token = register("listener");
        JsonNode songs = json.readTree(mvc.perform(get("/api/songs")).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());
        long id = songs.get("content").get(0).get("id").asLong();
        mvc.perform(get("/api/songs/" + id)).andExpect(status().isOk())
                .andExpect(jsonPath("$.artistName", is("Demo Synth")));
        mvc.perform(get("/api/songs/" + id + "/stream").header("Range", "bytes=100-199")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isPartialContent())
                .andExpect(header().string("Content-Range", startsWith("bytes 100-199/")));
        mvc.perform(get("/api/songs/" + id + "/stream").param("token", token))
                .andExpect(status().isOk()).andExpect(header().string("Accept-Ranges", "bytes"));
        mvc.perform(post("/api/songs/" + id + "/played").header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());
    }

    @Test
    void playlistsArePrivateAndNonAdminCannotUpload() throws Exception {
        String a = register("alice");
        String b = register("bob");
        String res = mvc.perform(post("/api/playlists").header("Authorization", "Bearer " + a)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Mine\"}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        long pid = json.readTree(res).get("id").asLong();
        mvc.perform(get("/api/playlists/" + pid).header("Authorization", "Bearer " + b))
                .andExpect(status().isForbidden());
        mvc.perform(delete("/api/playlists/" + pid).header("Authorization", "Bearer " + b))
                .andExpect(status().isForbidden());
        mvc.perform(delete("/api/songs/1").header("Authorization", "Bearer " + a))
                .andExpect(status().isForbidden());
    }
}
