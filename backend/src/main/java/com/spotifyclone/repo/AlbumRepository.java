package com.spotifyclone.repo;

import com.spotifyclone.model.Album;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AlbumRepository extends JpaRepository<Album, Long> {
    @EntityGraph(attributePaths = "artist")
    List<Album> findByArtistId(Long artistId);

    @Override
    @EntityGraph(attributePaths = "artist")
    List<Album> findAll();
}
