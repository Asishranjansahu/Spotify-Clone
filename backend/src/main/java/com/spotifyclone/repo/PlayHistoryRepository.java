package com.spotifyclone.repo;

import com.spotifyclone.model.PlayHistory;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface PlayHistoryRepository extends JpaRepository<PlayHistory, Long> {
    @EntityGraph(attributePaths = {"song", "song.artist", "song.album"})
    List<PlayHistory> findByUserIdOrderByPlayedAtDesc(Long userId, Pageable pageable);

    @Modifying
    @Query("delete from PlayHistory h where h.song.id = ?1")
    void deleteBySongId(Long songId);
}
