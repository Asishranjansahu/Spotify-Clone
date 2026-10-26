package com.spotifyclone.model;

import jakarta.persistence.*;

@Entity
public class Song {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;
    @Column(nullable = false)
    public String title;
    public int durationSec;
    // null for YOUTUBE songs (played through the official embedded player only)
    public String audioPath;
    public String coverUrl;
    public long playCount;
    // DB column stays nullable so existing rows survive ddl-auto=update; post-schema.sql backfills LOCAL.
    @Enumerated(EnumType.STRING)
    public SongSource source = SongSource.LOCAL;
    // YouTube video ID for YOUTUBE songs
    public String externalId;
    public String thumbnailUrl;
    // null for YOUTUBE songs (channel names are deliberately not stored)
    @ManyToOne
    public Artist artist;
    @ManyToOne
    public Album album;
}
