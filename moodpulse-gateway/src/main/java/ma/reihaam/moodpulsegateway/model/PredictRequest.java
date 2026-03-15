package ma.reihaam.moodpulsegateway.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class PredictRequest {
    @NotEmpty
    @Size(min = 1, max = 10)
    private List<SpotifyTrack> tracks;
    @JsonProperty("spotify_access_token")
    private String spotifyAccessToken;
    private String period = "medium_term";
}
