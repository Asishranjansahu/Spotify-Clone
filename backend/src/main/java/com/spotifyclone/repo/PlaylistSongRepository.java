package com.spotifyclone.repo;

import com.spotifyclone.model.PlaylistSong;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface PlaylistSongRepository extends JpaRepository<PlaylistSong, Long> {
    @EntityGraph(attributePaths = {"song", "song.artist", "song.album"})
    List<PlaylistSong> findByPlaylistIdOrderByPositionAsc(Long playlistId);

    Optional<PlaylistSong> findByPlaylistIdAndSongId(Long playlistId, Long songId);

    @Query("select coalesce(max(ps.position), 0) from PlaylistSong ps where ps.playlist.id = ?1")
    int maxPosition(Long playlistId);

    long countByPlaylistId(Long playlistId);

    @Modifying
    @Query("delete from PlaylistSong ps where ps.playlist.id = ?1")
    void deleteByPlaylistId(Long playlistId);

    @Modifying
    @Query("delete from PlaylistSong ps where ps.song.id = ?1")
    void deleteBySongId(Long songId);
}
