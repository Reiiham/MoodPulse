package ma.reihaam.moodpulsegateway.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class SpotifyTrack {
    private String id;
    private String title;
    private String artist;
    private String album;
    private String image;
    @JsonProperty("preview_url")
    private String previewUrl;
    @JsonProperty("spotify_id")
    private String spotifyId;
}