package com.spotifyclone.web;

import com.spotifyclone.dto.Dtos.*;
import com.spotifyclone.service.CatalogService;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api")
public class CatalogController {
    private final CatalogService catalog;

    public CatalogController(CatalogService catalog) {
        this.catalog = catalog;
    }

    @GetMapping("/artists")
    public List<ArtistDto> artists() {
        return catalog.listArtists();
    }

    @GetMapping("/artists/{id}")
    public ArtistDetail artist(@PathVariable Long id) {
        return catalog.artistDetail(id);
    }

    @PostMapping(value = "/artists", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public ArtistDto createArtist(@RequestParam String name, @RequestParam(required = false) String bio,
                                  @RequestParam(value = "image", required = false) MultipartFile image) {
        return catalog.createArtist(name, bio, image);
    }

    @PutMapping(value = "/artists/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ArtistDto updateArtist(@PathVariable Long id, @RequestParam String name,
                                  @RequestParam(required = false) String bio,
                                  @RequestParam(value = "image", required = false) MultipartFile image) {
        return catalog.updateArtist(id, name, bio, image);
    }

    @DeleteMapping("/artists/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteArtist(@PathVariable Long id) {
        catalog.deleteArtist(id);
    }

    @GetMapping("/albums")
    public List<AlbumDto> albums() {
        return catalog.listAlbums();
    }

    @GetMapping("/albums/{id}")
    public AlbumDetail album(@PathVariable Long id) {
        return catalog.albumDetail(id);
    }

    @PostMapping(value = "/albums", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public AlbumDto createAlbum(@RequestParam String title, @RequestParam Long artistId,
                                @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate releaseDate,
                                @RequestParam(value = "cover", required = false) MultipartFile cover) {
        return catalog.createAlbum(title, artistId, releaseDate, cover);
    }

    @PutMapping(value = "/albums/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public AlbumDto updateAlbum(@PathVariable Long id, @RequestParam String title, @RequestParam Long artistId,
                                @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate releaseDate,
                                @RequestParam(value = "cover", required = false) MultipartFile cover) {
        return catalog.updateAlbum(id, title, artistId, releaseDate, cover);
    }

    @DeleteMapping("/albums/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteAlbum(@PathVariable Long id) {
        catalog.deleteAlbum(id);
    }
}
