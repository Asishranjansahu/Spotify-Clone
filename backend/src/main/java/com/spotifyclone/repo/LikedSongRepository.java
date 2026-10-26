package com.spotifyclone.repo;

import com.spotifyclone.model.LikedSong;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface LikedSongRepository extends JpaRepository<LikedSong, Long> {
    @EntityGraph(attributePaths = {"song", "song.artist", "song.album"})
    List<LikedSong> findByUserIdOrderByLikedAtDesc(Long userId);

    boolean existsByUserIdAndSongId(Long userId, Long songId);

    @Modifying
    @Query("delete from LikedSong l where l.user.id = ?1 and l.song.id = ?2")
    void deleteByUserIdAndSongId(Long userId, Long songId);

    @Modifying
    @Query("delete from LikedSong l where l.song.id = ?1")
    void deleteBySongId(Long songId);
}
