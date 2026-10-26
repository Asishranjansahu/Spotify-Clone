package com.spotifyclone.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
public class PlayHistory {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    public User user;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    public Song song;
    public Instant playedAt = Instant.now();
}
