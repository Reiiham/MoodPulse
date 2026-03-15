package ma.reihaam.moodpulsegateway.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class MoodResponse {
    @JsonProperty("dominant_mood")
    private String dominantMood;
    @JsonProperty("dominant_emoji")
    private String dominantEmoji;
    private double confidence;
    @JsonProperty("radar_scores")
    private Map<String, Double> radarScores;
    private List<SongAnalysis> songs;
    private String summary;
}
