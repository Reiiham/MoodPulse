package ma.reihaam.moodpulsegateway.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.Map;

@Data
public class SongAnalysis {
    private String title;
    private String artist;
    private String emotion;
    private String emoji;
    private double confidence;
    @JsonProperty("all_scores")
    private Map<String, Double> allScores;
    private String image;
}
