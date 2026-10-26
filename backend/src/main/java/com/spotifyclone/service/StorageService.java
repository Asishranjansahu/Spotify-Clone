package com.spotifyclone.service;

import com.spotifyclone.config.AppProperties;
import com.spotifyclone.web.GlobalExceptionHandler.ApiException;
import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class StorageService {
    private static final Map<String, Set<String>> AUDIO = Map.of(
            "mp3", Set.of("audio/mpeg", "audio/mp3"),
            "ogg", Set.of("audio/ogg", "application/ogg"),
            "m4a", Set.of("audio/mp4", "audio/aac", "audio/x-m4a"),
            "aac", Set.of("audio/aac", "audio/aacp"));
    private static final Map<String, Set<String>> IMAGE = Map.of(
            "jpg", Set.of("image/jpeg"), "jpeg", Set.of("image/jpeg"),
            "png", Set.of("image/png"), "webp", Set.of("image/webp"));

    private final AppProperties props;
    private Path audioDir;
    private Path imageDir;

    public StorageService(AppProperties props) {
        this.props = props;
    }

    @PostConstruct
    void init() throws IOException {
        Path root = Path.of(props.storageDir()).toAbsolutePath().normalize();
        audioDir = Files.createDirectories(root.resolve("audio"));
        imageDir = Files.createDirectories(root.resolve("images"));
    }

    public String saveAudio(MultipartFile f) {
        return save(f, AUDIO, props.maxAudioBytes(), audioDir);
    }

    public String saveImage(MultipartFile f) {
        return save(f, IMAGE, props.maxImageBytes(), imageDir);
    }

    /** Used by the demo seeder only; bypasses upload validation. */
    public String saveAudioBytes(String ext, byte[] data) {
        String name = UUID.randomUUID() + "." + ext;
        try {
            Files.write(audioDir.resolve(name), data);
        } catch (IOException e) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not store file");
        }
        return name;
    }

    public Path audioPath(String name) {
        return resolve(audioDir, name);
    }

    public Path imagePath(String name) {
        return resolve(imageDir, name);
    }

    public void deleteAudio(String name) {
        if (name != null) {
            deleteQuietly(audioDir, name);
        }
    }

    public void deleteImageByUrl(String url) {
        String prefix = "/api/files/images/";
        if (url != null && url.startsWith(prefix)) {
            deleteQuietly(imageDir, url.substring(prefix.length()));
        }
    }

    public static String imageUrl(String name) {
        return "/api/files/images/" + name;
    }

    private String save(MultipartFile f, Map<String, Set<String>> allowed, long maxBytes, Path dir) {
        if (f == null || f.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "File is required");
        }
        if (f.getSize() > maxBytes) {
            throw new ApiException(HttpStatus.PAYLOAD_TOO_LARGE, "File exceeds " + maxBytes / 1_000_000 + " MB limit");
        }
        String original = f.getOriginalFilename() == null ? "" : f.getOriginalFilename();
        int dot = original.lastIndexOf('.');
        String ext = dot < 0 ? "" : original.substring(dot + 1).toLowerCase();
        Set<String> types = allowed.get(ext);
        String ct = f.getContentType() == null ? "" : f.getContentType().toLowerCase();
        if (types == null || !types.contains(ct)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Unsupported file type. Allowed: " + allowed.keySet());
        }
        String name = UUID.randomUUID() + "." + ext;
        try (InputStream in = f.getInputStream()) {
            Files.copy(in, dir.resolve(name), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not store file");
        }
        return name;
    }

    private Path resolve(Path dir, String name) {
        Path p = dir.resolve(name).normalize();
        if (!p.startsWith(dir) || !Files.isRegularFile(p)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "File not found");
        }
        return p;
    }

    private void deleteQuietly(Path dir, String name) {
        try {
            Path p = dir.resolve(name).normalize();
            if (p.startsWith(dir)) {
                Files.deleteIfExists(p);
            }
        } catch (IOException ignored) {
            // best effort
        }
    }
}
