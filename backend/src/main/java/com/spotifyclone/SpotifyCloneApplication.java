package com.spotifyclone;

import com.spotifyclone.config.AppProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(AppProperties.class)
public class SpotifyCloneApplication {
    public static void main(String[] args) {
        SpringApplication.run(SpotifyCloneApplication.class, args);
    }
}
