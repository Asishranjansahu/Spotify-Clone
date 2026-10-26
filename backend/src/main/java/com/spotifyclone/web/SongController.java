package com.spotifyclone.web;

import com.spotifyclone.dto.Dtos.*;
import com.spotifyclone.model.Song;
import com.spotifyclone.service.CatalogService;
import com.spotifyclone.service.StorageService;
import jakarta.validation.Valid;
import java.nio.file.Path;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import com.spotifyclone.web.GlobalExceptionHandler.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api")
public class SongController {
    private final CatalogService catalog;
    private final StorageService storage;

    public SongController(CatalogService catalog, StorageService storage) {
        this.catalog = catalog;
        this.storage = storage;
    }

    @GetMapping("/songs")
    public PageDto<SongDto> list(@RequestParam(required = false) String query,
                                 @RequestParam(defaultValue = "0") int page,
                                 @RequestParam(defaultValue = "20") int size) {
        return catalog.searchSongs(query, page, size);
    }

    @GetMapping("/songs/{id}")
    public SongDto get(@PathVariable Long id) {
        return catalog.getSong(id);
    }

    // Spring serves Resource bodies with Range support (206 Partial Content) automatically.
    @GetMapping("/songs/{id}/stream")
    public ResponseEntity<Resource> stream(@PathVariable Long id) {
        Song s = catalog.songEntity(id);
        if (s.audioPath == null) {
            throw new ApiException(HttpStatus.NOT_FOUND, "This song has no streamable audio file");
        }
        Path file = storage.audioPath(s.audioPath);
        String name = file.getFileName().toString();
        return ResponseEntity.ok()
                .header(HttpHeaders.ACCEPT_RANGES, "bytes")
                .contentType(mediaType(name))
                .cacheControl(CacheControl.noStore())
                .body(new FileSystemResource(file));
    }

    @PostMapping("/songs/{id}/played")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void played(@PathVariable Long id) {
        catalog.incrementPlayCount(id);
    }

    @PostMapping(value = "/songs", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public SongDto create(@RequestParam String title, @RequestParam Long artistId,
                          @RequestParam(required = false) Long albumId,
                          @RequestParam(defaultValue = "0") int durationSec,
                          @RequestParam("audio") MultipartFile audio,
                          @RequestParam(value = "cover", required = false) MultipartFile cover) {
        return catalog.createSong(title, artistId, albumId, durationSec, audio, cover);
    }

    @PutMapping("/songs/{id}")
    public SongDto update(@PathVariable Long id, @Valid @RequestBody SongUpdateRequest r) {
        return catalog.updateSong(id, r);
    }

    @DeleteMapping("/songs/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        catalog.deleteSong(id);
    }

    @GetMapping("/files/images/{name:[A-Za-z0-9-]+\\.[a-z]+}")
    public ResponseEntity<Resource> image(@PathVariable String name) {
        Path p = storage.imagePath(name);
        return ResponseEntity.ok()
                .contentType(mediaType(name))
                .cacheControl(CacheControl.maxAge(java.time.Duration.ofDays(7)).cachePublic())
                .body(new FileSystemResource(p));
    }

    private static MediaType mediaType(String filename) {
        String ext = filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
        return switch (ext) {
            case "mp3" -> MediaType.parseMediaType("audio/mpeg");
            case "ogg" -> MediaType.parseMediaType("audio/ogg");
            case "m4a" -> MediaType.parseMediaType("audio/mp4");
            case "wav" -> MediaType.parseMediaType("audio/wav");
            case "aac" -> MediaType.parseMediaType("audio/aac");
            case "png" -> MediaType.IMAGE_PNG;
            case "webp" -> MediaType.parseMediaType("image/webp");
            default -> MediaType.IMAGE_JPEG;
        };
    }
}
