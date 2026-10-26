package com.spotifyclone.model;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
public class Album {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;
    @Column(nullable = false)
    public String title;
    public String coverUrl;
    public LocalDate releaseDate;
    @ManyToOne(optional = false)
    public Artist artist;
}
