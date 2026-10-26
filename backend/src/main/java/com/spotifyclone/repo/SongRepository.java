package com.spotifyclone.repo;

import com.spotifyclone.model.Song;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SongRepository extends JpaRepository<Song, Long> {
    @EntityGraph(attributePaths = {"artist", "album"})
    @Query("""
        select s from Song s join s.artist ar left join s.album al
        where (s.source is null or s.source = com.spotifyclone.model.SongSource.LOCAL)
          and (:q is null
           or lower(s.title) like lower(concat('%', cast(:q as string), '%'))
           or lower(ar.name) like lower(concat('%', cast(:q as string), '%'))
           or lower(al.title) like lower(concat('%', cast(:q as string), '%')))
        """)
    Page<Song> search(@Param("q") String q, Pageable pageable);

    Optional<Song> findByExternalId(String externalId);

    List<Song> findByExternalIdIn(java.util.Collection<String> externalIds);

    @EntityGraph(attributePaths = {"artist", "album"})
    List<Song> findByArtistIdOrderByTitle(Long artistId);

    @EntityGraph(attributePaths = {"artist", "album"})
    List<Song> findByAlbumIdOrderByIdAsc(Long albumId);

    @Modifying
    @Query("update Song s set s.playCount = s.playCount + 1 where s.id = :id")
    int incrementPlayCount(@Param("id") Long id);
}
