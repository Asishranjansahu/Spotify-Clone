package com.spotifyclone.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
public class Playlist {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;
    @Column(nullable = false)
    public String name;
    public String description;
    @ManyToOne(optional = false)
    public User owner;
    public Instant createdAt = Instant.now();
}
