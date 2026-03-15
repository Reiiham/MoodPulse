package ma.reihaam.moodpulsegateway.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class SpotifyUser {
    private String id;
    @JsonProperty("display_name")
    private String displayName;
    private String email;
    private List<SpotifyImage> images;
}
