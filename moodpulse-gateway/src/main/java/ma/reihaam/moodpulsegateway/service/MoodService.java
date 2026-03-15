package ma.reihaam.moodpulsegateway.service;

import lombok.extern.slf4j.Slf4j;
import ma.reihaam.moodpulsegateway.model.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class MoodService {

    @Value("${moodpulse.api.url}")
    private String apiUrl;

    private WebClient webClient() {
        return WebClient.builder().baseUrl(apiUrl).build();
    }

    public MoodResponse predictMood(List<SpotifyTrack> tracks, String spotifyAccessToken, String period) {
        var payload = Map.of(
                "tracks", tracks.stream().map(t -> Map.of(
                        "title",      t.getTitle(),
                        "artist",     t.getArtist(),
                        "spotify_id", t.getSpotifyId() != null ? t.getSpotifyId() : "",
                        "image",      t.getImage() != null ? t.getImage() : ""
                )).toList(),
                "spotify_access_token", spotifyAccessToken != null ? spotifyAccessToken : "",
                "period", period != null ? period : "medium_term"
        );

        try {
            MoodResponse response = webClient().post()
                    .uri("/predict")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(payload)
                    .retrieve()
                    .bodyToMono(MoodResponse.class)
                    .block();

            log.info("Mood prediction: {} ({})",
                    response != null ? response.getDominantMood() : "null",
                    response != null ? response.getConfidence() : 0);

            return response;

        } catch (Exception e) {
            log.error("Failed to call mood API: {}", e.getMessage());
            throw new RuntimeException("Mood prediction failed: " + e.getMessage());
        }
    }
}
