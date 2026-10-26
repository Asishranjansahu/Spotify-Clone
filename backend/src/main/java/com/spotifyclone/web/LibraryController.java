package com.spotifyclone.web;

import com.spotifyclone.dto.Dtos.*;
import com.spotifyclone.security.AuthUser;
import com.spotifyclone.service.LibraryService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class LibraryController {
    private final LibraryService lib;

    public LibraryController(LibraryService lib) {
        this.lib = lib;
    }

    @GetMapping("/playlists")
    public List<PlaylistDto> playlists(@AuthenticationPrincipal AuthUser u) {
        return lib.myPlaylists(u.id());
    }

    @PostMapping("/playlists")
    @ResponseStatus(HttpStatus.CREATED)
    public PlaylistDto create(@AuthenticationPrincipal AuthUser u, @Valid @RequestBody PlaylistRequest r) {
        return lib.createPlaylist(u.id(), r);
    }

    @GetMapping("/playlists/{id}")
    public PlaylistDetail detail(@AuthenticationPrincipal AuthUser u, @PathVariable Long id) {
        return lib.playlistDetail(u.id(), id);
    }

    @PutMapping("/playlists/{id}")
    public PlaylistDto rename(@AuthenticationPrincipal AuthUser u, @PathVariable Long id,
                              @Valid @RequestBody PlaylistRequest r) {
        return lib.updatePlaylist(u.id(), id, r);
    }

    @DeleteMapping("/playlists/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal AuthUser u, @PathVariable Long id) {
        lib.deletePlaylist(u.id(), id);
    }

    @PostMapping("/playlists/{id}/songs/{songId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void addSong(@AuthenticationPrincipal AuthUser u, @PathVariable Long id, @PathVariable Long songId) {
        lib.addSong(u.id(), id, songId);
    }

    @DeleteMapping("/playlists/{id}/songs/{songId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeSong(@AuthenticationPrincipal AuthUser u, @PathVariable Long id, @PathVariable Long songId) {
        lib.removeSong(u.id(), id, songId);
    }

    @PostMapping("/songs/{id}/like")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void like(@AuthenticationPrincipal AuthUser u, @PathVariable Long id) {
        lib.like(u.id(), id);
    }

    @DeleteMapping("/songs/{id}/like")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unlike(@AuthenticationPrincipal AuthUser u, @PathVariable Long id) {
        lib.unlike(u.id(), id);
    }

    @GetMapping("/me/liked")
    public List<SongDto> liked(@AuthenticationPrincipal AuthUser u) {
        return lib.liked(u.id());
    }

    @GetMapping("/history")
    public List<HistoryEntry> history(@AuthenticationPrincipal AuthUser u,
                                      @RequestParam(defaultValue = "20") int limit) {
        return lib.recent(u.id(), limit);
    }

    @PostMapping("/history")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void record(@AuthenticationPrincipal AuthUser u, @Valid @RequestBody HistoryRequest r) {
        lib.recordPlay(u.id(), r.songId());
    }
}
