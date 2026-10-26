package com.spotifyclone.config;

import com.spotifyclone.model.*;
import com.spotifyclone.repo.*;
import com.spotifyclone.service.StorageService;
import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataSeeder implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final AppProperties props;
    private final UserRepository users;
    private final ArtistRepository artists;
    private final AlbumRepository albums;
    private final SongRepository songs;
    private final PasswordEncoder encoder;
    private final StorageService storage;

    public DataSeeder(AppProperties props, UserRepository users, ArtistRepository artists, AlbumRepository albums,
                      SongRepository songs, PasswordEncoder encoder, StorageService storage) {
        this.props = props;
        this.users = users;
        this.artists = artists;
        this.albums = albums;
        this.songs = songs;
        this.encoder = encoder;
        this.storage = storage;
    }

    @Override
    public void run(ApplicationArguments args) {
        seedAdmin();
        if (props.seedDemo() && songs.count() == 0) {
            seedDemo();
        }
    }

    private void seedAdmin() {
        String pw = props.adminPassword();
        if (pw == null || pw.isBlank() || users.existsByEmailIgnoreCase(props.adminEmail())) {
            return;
        }
        User u = new User();
        u.username = "admin";
        u.email = props.adminEmail().toLowerCase();
        u.passwordHash = encoder.encode(pw);
        u.role = User.Role.ADMIN;
        users.save(u);
        log.info("Created admin user {}", u.email);
    }

    private void seedDemo() {
        Artist a = new Artist();
        a.name = "Demo Synth";
        a.bio = "Generated sine-wave demo artist.";
        artists.save(a);
        Album al = new Album();
        al.title = "Test Tones";
        al.artist = a;
        albums.save(al);
        String[] titles = {"A Major Chord", "C Major Chord", "G Major Chord"};
        double[][] chords = {{440, 554.37, 659.25}, {261.63, 329.63, 392.0}, {392.0, 493.88, 587.33}};
        for (int i = 0; i < titles.length; i++) {
            Song s = new Song();
            s.title = titles[i];
            s.durationSec = 40;
            s.audioPath = storage.saveAudioBytes("wav", wav(chords[i], 40));
            s.artist = a;
            s.album = al;
            songs.save(s);
        }
        log.info("Seeded demo catalog");
    }

    private static byte[] wav(double[] freqs, int seconds) {
        int rate = 22050;
        int n = rate * seconds;
        ByteBuffer pcm = ByteBuffer.allocate(n * 2).order(ByteOrder.LITTLE_ENDIAN);
        for (int i = 0; i < n; i++) {
            double t = (double) i / rate;
            double v = 0;
            for (double f : freqs) {
                v += Math.sin(2 * Math.PI * f * t);
            }
            double env = Math.min(1.0, Math.min(t * 10, (seconds - t) * 10));
            pcm.putShort((short) (v / freqs.length * env * 0.4 * Short.MAX_VALUE));
        }
        ByteBuffer h = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN);
        h.put("RIFF".getBytes()).putInt(36 + n * 2).put("WAVE".getBytes()).put("fmt ".getBytes())
                .putInt(16).putShort((short) 1).putShort((short) 1).putInt(rate).putInt(rate * 2)
                .putShort((short) 2).putShort((short) 16).put("data".getBytes()).putInt(n * 2);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.writeBytes(h.array());
        out.writeBytes(pcm.array());
        return out.toByteArray();
    }
}
