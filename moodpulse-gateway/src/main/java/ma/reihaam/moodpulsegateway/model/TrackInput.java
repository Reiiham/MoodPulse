package ma.reihaam.moodpulsegateway.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

@Data
public class TrackInput {
    @NotEmpty
    private String title;
    @NotEmpty
    private String artist;
    @JsonProperty("spotify_id")
    private String spotifyId;
}
