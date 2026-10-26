package com.spotifyclone.web;

import com.spotifyclone.dto.Dtos.*;
import com.spotifyclone.service.YouTubeService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/youtube")
public class YouTubeController {
    private final YouTubeService youtube;

    public YouTubeController(YouTubeService youtube) {
        this.youtube = youtube;
    }

    @GetMapping("/search")
    public List<YouTubeVideoDto> search(@RequestParam String q) {
        return youtube.search(q);
    }

    @PostMapping("/songs")
    public SongDto song(@Valid @RequestBody YouTubeSongRequest r) {
        return youtube.getOrCreateSong(r.videoId());
    }
}
