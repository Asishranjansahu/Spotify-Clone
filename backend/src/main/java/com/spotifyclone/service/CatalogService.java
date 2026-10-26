package com.spotifyclone.service;

import com.spotifyclone.dto.Dtos.*;
import com.spotifyclone.model.*;
import com.spotifyclone.repo.*;
import com.spotifyclone.web.GlobalExceptionHandler.ApiException;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class CatalogService {
    private final SongRepository songs;
    private final ArtistRepository artists;
    private final AlbumRepository albums;
    private final PlaylistSongRepository playlistSongs;
    private final LikedSongRepository likes;
    private final PlayHistoryRepository history;
    private final StorageService storage;

    public CatalogService(SongRepository songs, ArtistRepository artists, AlbumRepository albums,
                          PlaylistSongRepository playlistSongs, LikedSongRepository likes,
                          PlayHistoryRepository history, StorageService storage) {
        this.songs = songs;
        this.artists = artists;
        this.albums = albums;
        this.playlistSongs = playlistSongs;
        this.likes = likes;
        this.history = history;
        this.storage = storage;
    }

    // ---- songs ----
    @Transactional(readOnly = true)
    public PageDto<SongDto> searchSongs(String q, int page, int size) {
        String query = q == null || q.isBlank() ? null : q.trim();
        var pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100), Sort.by("id").descending());
        return PageDto.ofSongs(songs.search(query, pageable));
    }

    @Transactional(readOnly = true)
    public SongDto getSong(Long id) {
        return SongDto.of(songs.findById(id).orElseThrow(() -> notFound("Song")));
    }

    @Transactional(readOnly = true)
    public Song songEntity(Long id) {
        return songs.findById(id).orElseThrow(() -> notFound("Song"));
    }

    @Transactional
    public SongDto createSong(String title, Long artistId, Long albumId, int durationSec,
                              MultipartFile audio, MultipartFile cover) {
        if (title == null || title.isBlank() || title.length() > 200) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "title: must be 1-200 characters");
        }
        if (durationSec < 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "durationSec: must not be negative");
        }
        Artist artist = artist(artistId);
        Album album = albumId == null ? null : album(albumId);
        String audioName = storage.saveAudio(audio);
        String coverName = null;
        try {
            if (cover != null && !cover.isEmpty()) {
                coverName = storage.saveImage(cover);
            }
        } catch (RuntimeException e) {
            storage.deleteAudio(audioName);
            throw e;
        }
        Song s = new Song();
        s.title = title.trim();
        s.durationSec = durationSec;
        s.audioPath = audioName;
        s.coverUrl = coverName == null ? null : StorageService.imageUrl(coverName);
        s.artist = artist;
        s.album = album;
        return SongDto.of(songs.save(s));
    }

    @Transactional
    public SongDto updateSong(Long id, SongUpdateRequest r) {
        Song s = songs.findById(id).orElseThrow(() -> notFound("Song"));
        s.title = r.title().trim();
        s.durationSec = r.durationSec();
        s.artist = artist(r.artistId());
        s.album = r.albumId() == null ? null : album(r.albumId());
        return SongDto.of(s);
    }

    @Transactional
    public void deleteSong(Long id) {
        Song s = songs.findById(id).orElseThrow(() -> notFound("Song"));
        removeSong(s);
    }

    private void removeSong(Song s) {
        playlistSongs.deleteBySongId(s.id);
        likes.deleteBySongId(s.id);
        history.deleteBySongId(s.id);
        songs.delete(s);
        storage.deleteAudio(s.audioPath);
        storage.deleteImageByUrl(s.coverUrl);
    }

    @Transactional
    public void incrementPlayCount(Long id) {
        if (songs.incrementPlayCount(id) == 0) {
            throw notFound("Song");
        }
    }

    // ---- artists ----
    @Transactional(readOnly = true)
    public List<ArtistDto> listArtists() {
        return artists.findAll(Sort.by("name")).stream().map(ArtistDto::of).toList();
    }

    @Transactional(readOnly = true)
    public ArtistDetail artistDetail(Long id) {
        Artist a = artist(id);
        return new ArtistDetail(ArtistDto.of(a),
                albums.findByArtistId(id).stream().map(AlbumDto::of).toList(),
                songs.findByArtistIdOrderByTitle(id).stream().map(SongDto::of).toList());
    }

    @Transactional
    public ArtistDto createArtist(String name, String bio, MultipartFile image) {
        Artist a = new Artist();
        apply(a, name, bio, image);
        return ArtistDto.of(artists.save(a));
    }

    @Transactional
    public ArtistDto updateArtist(Long id, String name, String bio, MultipartFile image) {
        Artist a = artist(id);
        apply(a, name, bio, image);
        return ArtistDto.of(a);
    }

    private void apply(Artist a, String name, String bio, MultipartFile image) {
        if (name == null || name.isBlank() || name.length() > 150) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "name: must be 1-150 characters");
        }
        if (bio != null && bio.length() > 2000) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "bio: too long");
        }
        a.name = name.trim();
        a.bio = bio;
        if (image != null && !image.isEmpty()) {
            storage.deleteImageByUrl(a.imageUrl);
            a.imageUrl = StorageService.imageUrl(storage.saveImage(image));
        }
    }

    @Transactional
    public void deleteArtist(Long id) {
        Artist a = artist(id);
        songs.findByArtistIdOrderByTitle(id).forEach(this::removeSong);
        albums.findByArtistId(id).forEach(al -> {
            storage.deleteImageByUrl(al.coverUrl);
            albums.delete(al);
        });
        storage.deleteImageByUrl(a.imageUrl);
        artists.delete(a);
    }

    // ---- albums ----
    @Transactional(readOnly = true)
    public List<AlbumDto> listAlbums() {
        return albums.findAll().stream().map(AlbumDto::of).toList();
    }

    @Transactional(readOnly = true)
    public AlbumDetail albumDetail(Long id) {
        Album a = album(id);
        return new AlbumDetail(AlbumDto.of(a), songs.findByAlbumIdOrderByIdAsc(id).stream().map(SongDto::of).toList());
    }

    @Transactional
    public AlbumDto createAlbum(String title, Long artistId, LocalDate releaseDate, MultipartFile cover) {
        Album a = new Album();
        a.artist = artist(artistId);
        applyAlbum(a, title, releaseDate, cover);
        return AlbumDto.of(albums.save(a));
    }

    @Transactional
    public AlbumDto updateAlbum(Long id, String title, Long artistId, LocalDate releaseDate, MultipartFile cover) {
        Album a = album(id);
        a.artist = artist(artistId);
        applyAlbum(a, title, releaseDate, cover);
        return AlbumDto.of(a);
    }

    private void applyAlbum(Album a, String title, LocalDate releaseDate, MultipartFile cover) {
        if (title == null || title.isBlank() || title.length() > 200) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "title: must be 1-200 characters");
        }
        a.title = title.trim();
        a.releaseDate = releaseDate;
        if (cover != null && !cover.isEmpty()) {
            storage.deleteImageByUrl(a.coverUrl);
            a.coverUrl = StorageService.imageUrl(storage.saveImage(cover));
        }
    }

    @Transactional
    public void deleteAlbum(Long id) {
        Album a = album(id);
        songs.findByAlbumIdOrderByIdAsc(id).forEach(s -> s.album = null);
        storage.deleteImageByUrl(a.coverUrl);
        albums.delete(a);
    }

    private Artist artist(Long id) {
        return artists.findById(id).orElseThrow(() -> notFound("Artist"));
    }

    private Album album(Long id) {
        return albums.findById(id).orElseThrow(() -> notFound("Album"));
    }

    private ApiException notFound(String what) {
        return new ApiException(HttpStatus.NOT_FOUND, what + " not found");
    }
}
