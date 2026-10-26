package com.spotifyclone.service;

import com.spotifyclone.dto.Dtos.*;
import com.spotifyclone.model.*;
import com.spotifyclone.repo.*;
import com.spotifyclone.web.GlobalExceptionHandler.ApiException;
import java.util.List;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LibraryService {
    private final PlaylistRepository playlists;
    private final PlaylistSongRepository playlistSongs;
    private final SongRepository songs;
    private final UserRepository users;
    private final LikedSongRepository likes;
    private final PlayHistoryRepository history;

    public LibraryService(PlaylistRepository playlists, PlaylistSongRepository playlistSongs, SongRepository songs,
                          UserRepository users, LikedSongRepository likes, PlayHistoryRepository history) {
        this.playlists = playlists;
        this.playlistSongs = playlistSongs;
        this.songs = songs;
        this.users = users;
        this.likes = likes;
        this.history = history;
    }

    // ---- playlists (owner-only: NFR-9) ----
    @Transactional(readOnly = true)
    public List<PlaylistDto> myPlaylists(Long userId) {
        return playlists.findByOwnerIdOrderByCreatedAtDesc(userId).stream().map(this::dto).toList();
    }

    @Transactional
    public PlaylistDto createPlaylist(Long userId, PlaylistRequest r) {
        Playlist p = new Playlist();
        p.name = r.name().trim();
        p.description = r.description();
        p.owner = users.getReferenceById(userId);
        return dto(playlists.save(p));
    }

    @Transactional(readOnly = true)
    public PlaylistDetail playlistDetail(Long userId, Long id) {
        Playlist p = owned(userId, id);
        List<SongDto> list = playlistSongs.findByPlaylistIdOrderByPositionAsc(id).stream()
                .map(ps -> SongDto.of(ps.song)).toList();
        return new PlaylistDetail(dto(p), list);
    }

    @Transactional
    public PlaylistDto updatePlaylist(Long userId, Long id, PlaylistRequest r) {
        Playlist p = owned(userId, id);
        p.name = r.name().trim();
        p.description = r.description();
        return dto(p);
    }

    @Transactional
    public void deletePlaylist(Long userId, Long id) {
        Playlist p = owned(userId, id);
        playlistSongs.deleteByPlaylistId(id);
        playlists.delete(p);
    }

    @Transactional
    public void addSong(Long userId, Long id, Long songId) {
        Playlist p = owned(userId, id);
        Song s = songs.findById(songId).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Song not found"));
        if (playlistSongs.findByPlaylistIdAndSongId(id, songId).isPresent()) {
            return;
        }
        PlaylistSong ps = new PlaylistSong();
        ps.playlist = p;
        ps.song = s;
        ps.position = playlistSongs.maxPosition(id) + 1;
        playlistSongs.save(ps);
    }

    @Transactional
    public void removeSong(Long userId, Long id, Long songId) {
        owned(userId, id);
        playlistSongs.findByPlaylistIdAndSongId(id, songId).ifPresent(playlistSongs::delete);
    }

    private Playlist owned(Long userId, Long id) {
        Playlist p = playlists.findById(id).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Playlist not found"));
        if (!p.owner.id.equals(userId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "You can only access your own playlists");
        }
        return p;
    }

    private PlaylistDto dto(Playlist p) {
        return new PlaylistDto(p.id, p.name, p.description, playlistSongs.countByPlaylistId(p.id), p.createdAt);
    }

    // ---- likes ----
    @Transactional
    public void like(Long userId, Long songId) {
        Song s = songs.findById(songId).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Song not found"));
        if (likes.existsByUserIdAndSongId(userId, songId)) {
            return;
        }
        LikedSong l = new LikedSong();
        l.user = users.getReferenceById(userId);
        l.song = s;
        try {
            likes.saveAndFlush(l);
        } catch (DataIntegrityViolationException ignored) {
            // concurrent duplicate like is harmless
        }
    }

    @Transactional
    public void unlike(Long userId, Long songId) {
        likes.deleteByUserIdAndSongId(userId, songId);
    }

    @Transactional(readOnly = true)
    public List<SongDto> liked(Long userId) {
        return likes.findByUserIdOrderByLikedAtDesc(userId).stream().map(l -> SongDto.of(l.song)).toList();
    }

    // ---- history ----
    @Transactional
    public void recordPlay(Long userId, Long songId) {
        Song s = songs.findById(songId).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Song not found"));
        PlayHistory h = new PlayHistory();
        h.user = users.getReferenceById(userId);
        h.song = s;
        history.save(h);
    }

    @Transactional(readOnly = true)
    public List<HistoryEntry> recent(Long userId, int limit) {
        return history.findByUserIdOrderByPlayedAtDesc(userId, PageRequest.of(0, Math.min(Math.max(limit, 1), 100)))
                .stream().map(h -> new HistoryEntry(SongDto.of(h.song), h.playedAt)).toList();
    }
}
