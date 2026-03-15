package ma.reihaam.moodpulsegateway.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;
import ma.reihaam.moodpulsegateway.model.SpotifyTrack;
import ma.reihaam.moodpulsegateway.model.SpotifyUser;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class SpotifyService {

    private static final String SPOTIFY_API = "https://api.spotify.com/v1";
    private final ObjectMapper mapper = new ObjectMapper();

    private final WebClient webClient = WebClient.builder()
            .baseUrl(SPOTIFY_API)
            .build();

    public List<SpotifyTrack> getTopTracks(String accessToken, int limit, String period) {
        try {
            String responseStr = webClient.get()
                    .uri("/me/top/tracks?time_range=" + period + "&limit=" + limit)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            if (responseStr == null) return List.of();

            JsonNode response = mapper.readTree(responseStr);
            List<SpotifyTrack> tracks = new ArrayList<>();

            if (!response.has("items")) {
                log.error("No items in response: {}", responseStr.substring(0, Math.min(200, responseStr.length())));
                return tracks;
            }

            for (JsonNode item : response.get("items")) {
                SpotifyTrack track = new SpotifyTrack();
                track.setId(item.get("id").asText());
                track.setSpotifyId(item.get("id").asText());
                track.setTitle(item.get("name").asText());
                track.setArtist(item.get("artists").get(0).get("name").asText());
                track.setAlbum(item.get("album").get("name").asText());

                JsonNode images = item.get("album").get("images");
                if (images.size() > 0) {
                    track.setImage(images.get(0).get("url").asText());
                }

                if (item.has("preview_url") && !item.get("preview_url").isNull()) {
                    track.setPreviewUrl(item.get("preview_url").asText());
                }

                tracks.add(track);
            }

            log.info("Fetched {} top tracks", tracks.size());
            return tracks;

        } catch (Exception e) {
            log.error("Failed to fetch top tracks: {}", e.getMessage(), e);
            return List.of();
        }
    }

    public SpotifyUser getCurrentUser(String accessToken) {
        try {
            String responseStr = webClient.get()
                    .uri("/me")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            if (responseStr == null) return null;

            JsonNode response = mapper.readTree(responseStr);

            SpotifyUser user = new SpotifyUser();
            user.setId(response.get("id").asText());
            user.setDisplayName(response.has("display_name")
                    ? response.get("display_name").asText() : "");
            user.setEmail(response.has("email")
                    ? response.get("email").asText() : "");

            return user;

        } catch (Exception e) {
            log.error("Failed to fetch user profile: {}", e.getMessage(), e);
            return null;
        }
    }
}