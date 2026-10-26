package com.spotifyclone.dto;

import com.spotifyclone.model.*;
import jakarta.validation.constraints.*;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.domain.Page;

public final class Dtos {
    private Dtos() {}

    public record RegisterRequest(
            @NotBlank @Size(min = 3, max = 30) @Pattern(regexp = "[A-Za-z0-9_.-]+") String username,
            @NotBlank @Email @Size(max = 120) String email,
            @NotBlank @Size(min = 8, max = 72) String password) {}

    public record LoginRequest(@NotBlank String email, @NotBlank String password) {}

    public record UpdateProfileRequest(
            @NotBlank @Size(min = 3, max = 30) @Pattern(regexp = "[A-Za-z0-9_.-]+") String username,
            @NotBlank @Email @Size(max = 120) String email,
            @Size(min = 8, max = 72) String newPassword) {}

    public record UserDto(Long id, String username, String email, String role) {
        public static UserDto of(User u) {
            return new UserDto(u.id, u.username, u.email, u.role.name());
        }
    }

    public record AuthResponse(String token, UserDto user) {}

    public record SongDto(Long id, String title, int durationSec, String coverUrl, long playCount,
                          Long artistId, String artistName, Long albumId, String albumTitle,
                          String source, String externalId, String thumbnailUrl) {
        public static SongDto of(Song s) {
            SongSource src = s.source == null ? SongSource.LOCAL : s.source;
            String cover = s.coverUrl != null ? s.coverUrl
                    : s.thumbnailUrl != null ? s.thumbnailUrl
                    : (s.album != null ? s.album.coverUrl : null);
            return new SongDto(s.id, s.title, s.durationSec, cover, s.playCount,
                    s.artist == null ? null : s.artist.id,
                    s.artist == null ? "YouTube" : s.artist.name,
                    s.album == null ? null : s.album.id, s.album == null ? null : s.album.title,
                    src.name(), s.externalId, s.thumbnailUrl);
        }
    }

    public record PageDto<T>(List<T> content, int page, int size, long totalElements, int totalPages) {
        public static PageDto<SongDto> ofSongs(Page<Song> p) {
            return new PageDto<>(p.getContent().stream().map(SongDto::of).toList(),
                    p.getNumber(), p.getSize(), p.getTotalElements(), p.getTotalPages());
        }
    }

    public record SongUpdateRequest(
            @NotBlank @Size(max = 200) String title,
            @NotNull Long artistId,
            Long albumId,
            @PositiveOrZero int durationSec) {}

    public record ArtistDto(Long id, String name, String bio, String imageUrl) {
        public static ArtistDto of(Artist a) {
            return new ArtistDto(a.id, a.name, a.bio, a.imageUrl);
        }
    }

    public record AlbumDto(Long id, String title, String coverUrl, LocalDate releaseDate,
                           Long artistId, String artistName) {
        public static AlbumDto of(Album a) {
            return new AlbumDto(a.id, a.title, a.coverUrl, a.releaseDate, a.artist.id, a.artist.name);
        }
    }

    public record ArtistDetail(ArtistDto artist, List<AlbumDto> albums, List<SongDto> songs) {}

    public record AlbumDetail(AlbumDto album, List<SongDto> songs) {}

    public record PlaylistRequest(@NotBlank @Size(max = 100) String name, @Size(max = 500) String description) {}

    public record PlaylistDto(Long id, String name, String description, long songCount, Instant createdAt) {}

    public record PlaylistDetail(PlaylistDto playlist, List<SongDto> songs) {}

    public record HistoryRequest(@NotNull Long songId) {}

    public record HistoryEntry(SongDto song, Instant playedAt) {}

    public record YouTubeVideoDto(String videoId, String title, String channelTitle, String thumbnailUrl,
                                  int durationSec, Long songId) {
        public YouTubeVideoDto withSongId(Long id) {
            return new YouTubeVideoDto(videoId, title, channelTitle, thumbnailUrl, durationSec, id);
        }
    }

    public record YouTubeSongRequest(@NotBlank @Pattern(regexp = "[A-Za-z0-9_-]{11}") String videoId) {}
}
