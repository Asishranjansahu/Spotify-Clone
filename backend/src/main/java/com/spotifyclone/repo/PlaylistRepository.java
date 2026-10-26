package com.spotifyclone.repo;

import com.spotifyclone.model.Playlist;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlaylistRepository extends JpaRepository<Playlist, Long> {
    List<Playlist> findByOwnerIdOrderByCreatedAtDesc(Long ownerId);
}
