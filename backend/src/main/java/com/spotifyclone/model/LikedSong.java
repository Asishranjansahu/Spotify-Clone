package com.spotifyclone.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "song_id"}))
public class LikedSong {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    public User user;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    public Song song;
    public Instant likedAt = Instant.now();
}
