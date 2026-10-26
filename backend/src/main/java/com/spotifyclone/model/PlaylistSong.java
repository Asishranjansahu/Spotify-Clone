package com.spotifyclone.model;

import jakarta.persistence.*;

@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"playlist_id", "song_id"}))
public class PlaylistSong {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    public Playlist playlist;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    public Song song;
    public int position;
}
