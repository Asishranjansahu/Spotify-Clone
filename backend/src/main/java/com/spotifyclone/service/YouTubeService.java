package com.spotifyclone.service;

import com.spotifyclone.client.YouTubeClient;
import com.spotifyclone.client.YouTubeVideo;
import com.spotifyclone.config.AppProperties;
import com.spotifyclone.dto.Dtos.SongDto;
import com.spotifyclone.dto.Dtos.YouTubeVideoDto;
import com.spotifyclone.model.Song;
import com.spotifyclone.model.SongSource;
import com.spotifyclone.repo.SongRepository;
import com.spotifyclone.web.GlobalExceptionHandler.ApiException;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class YouTubeService {
    private final YouTubeClient client;
    private final SongRepository songs;
    // Search results are cached briefly to save API quota; nothing here is persisted.
    private final TtlCache<String, List<YouTubeVideoDto>> searchCache;

    public YouTubeService(YouTubeClient client, SongRepository songs, AppProperties props) {
        this.client = client;
        this.songs = songs;
        this.searchCache = new TtlCache<>(Duration.ofSeconds(Math.max(props.youtubeCacheSeconds(), 1)), 200);
    }

    public List<YouTubeVideoDto> search(String q) {
        String query = q == null ? "" : q.trim();
        if (query.isEmpty() || query.length() > 100) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "q: must be 1-100 characters");
        }
        String key = query.toLowerCase();
        var cached = searchCache.get(key);
        if (cached.isPresent()) {
            return withSongIds(cached.get());
        }
        List<YouTubeVideoDto> result = client.searchMusic(query).stream().map(YouTubeService::toDto).toList();
        searchCache.put(key, result);
        return withSongIds(result);
    }

    // Marks results already saved as songs (liked, in a playlist, played) so the client can reuse their ids.
    private List<YouTubeVideoDto> withSongIds(List<YouTubeVideoDto> results) {
        Map<String, Long> saved = songs.findByExternalIdIn(results.stream().map(YouTubeVideoDto::videoId).toList())
                .stream().collect(Collectors.toMap(s -> s.externalId, s -> s.id));
        return results.stream().map(r -> r.withSongId(saved.get(r.videoId()))).toList();
    }

    /** Returns the Song for a video, creating it on first use. Only id, title, thumbnail and duration are stored. */
    public SongDto getOrCreateSong(String videoId) {
        var existing = songs.findByExternalId(videoId);
        if (existing.isPresent()) {
            return SongDto.of(existing.get());
        }
        YouTubeVideo v = client.findVideo(videoId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "YouTube video not found"));
        if (!v.embeddable()) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "This video cannot be played in embedded players");
        }
        Song s = new Song();
        s.source = SongSource.YOUTUBE;
        s.externalId = v.id();
        s.title = v.title().length() > 200 ? v.title().substring(0, 200) : v.title();
        s.thumbnailUrl = v.thumbnailUrl();
        s.durationSec = v.durationSec();
        try {
            return SongDto.of(songs.saveAndFlush(s));
        } catch (DataIntegrityViolationException race) {
            return SongDto.of(songs.findByExternalId(videoId).orElseThrow(() -> race));
        }
    }

    private static YouTubeVideoDto toDto(YouTubeVideo v) {
        return new YouTubeVideoDto(v.id(), v.title(), v.channelTitle(), v.thumbnailUrl(), v.durationSec(), null);
    }
}
