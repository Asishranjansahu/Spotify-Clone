package com.spotifyclone.model;

import jakarta.persistence.*;

@Entity
public class Artist {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;
    @Column(nullable = false)
    public String name;
    @Column(length = 2000)
    public String bio;
    public String imageUrl;
}
