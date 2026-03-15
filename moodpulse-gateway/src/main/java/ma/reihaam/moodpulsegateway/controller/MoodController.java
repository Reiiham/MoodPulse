package ma.reihaam.moodpulsegateway.controller;


import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import ma.reihaam.moodpulsegateway.model.MoodResponse;
import ma.reihaam.moodpulsegateway.model.PredictRequest;
import ma.reihaam.moodpulsegateway.model.SpotifyTrack;
import ma.reihaam.moodpulsegateway.model.SpotifyUser;
import ma.reihaam.moodpulsegateway.service.MoodService;
import ma.reihaam.moodpulsegateway.service.SpotifyService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

import static reactor.netty.http.HttpConnectionLiveness.log;

@RestController
@RequestMapping("/api/mood")
@RequiredArgsConstructor
@Tag(name = "MoodPulse", description = "Mood analysis from Spotify tracks")
public class MoodController {

    private final MoodService moodService;
    private final SpotifyService spotifyService;

    @Operation(summary = "Get user's top tracks and predict mood (full flow)")
    @GetMapping("/analyze")
    public ResponseEntity<?> analyzeMood(
            @RequestHeader(value = "Authorization", required = false) String bearerToken,
            @RequestParam(defaultValue = "medium_term") String period) {

        log.info("Analyzing mood for period: {}", period);
        String accessToken = bearerToken.replace("Bearer ", "").trim();

        List<SpotifyTrack> topTracks = spotifyService.getTopTracks(accessToken, 5, period);
        if (topTracks.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "No tracks found for period: " + period));
        }

        MoodResponse mood = moodService.predictMood(topTracks, accessToken, period);

        if (mood != null && mood.getSongs() != null) {
            for (int i = 0; i < mood.getSongs().size(); i++) {
                if (i < topTracks.size()) {
                    mood.getSongs().get(i).setImage(topTracks.get(i).getImage());
                }
            }
        }

        return ResponseEntity.ok(mood);
    }
    @Operation(summary = "Predict mood from manually provided tracks")
    @PostMapping("/predict")
    public ResponseEntity<MoodResponse> predictFromTracks(
            @Valid @RequestBody PredictRequest request) {

        MoodResponse mood = moodService.predictMood(
                request.getTracks(),
                request.getSpotifyAccessToken(),
                request.getPeriod() != null ? request.getPeriod() : "medium_term"


        );
        return ResponseEntity.ok(mood);
    }

    @Operation(summary = "Get current user's Spotify profile")
    @GetMapping("/me")
    public ResponseEntity<SpotifyUser> getMe(
            @RequestHeader("Authorization") String bearerToken) {

        String accessToken = bearerToken.replace("Bearer ", "");
        SpotifyUser user = spotifyService.getCurrentUser(accessToken);
        return ResponseEntity.ok(user);
    }

    @Operation(summary = "Get user's top tracks without mood analysis")
    @GetMapping("/top-tracks")
    public ResponseEntity<List<SpotifyTrack>> getTopTracks(
            @RequestHeader("Authorization") String bearerToken,
            @RequestParam(defaultValue = "5") int limit, @RequestParam(defaultValue = "medium_term") String period) {

        String accessToken = bearerToken.replace("Bearer ", "");
        List<SpotifyTrack> tracks = spotifyService.getTopTracks(accessToken, limit, period);
        return ResponseEntity.ok(tracks);
    }
}