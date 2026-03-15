from typing import Optional
import numpy as np

# Weight split: 70% lyrics emotion, 30% audio features
LYRICS_WEIGHT = 0.20
AUDIO_WEIGHT  = 0.80

def compute_mood(
    emotion_scores: dict,
    audio_features: Optional[dict] = None
) -> dict:
    labels = ["joy", "sadness", "anger", "fear", "love", "surprise"]
    
    lyrics_vec = np.array([emotion_scores.get(l, 0.0) for l in labels])
    
    # Redistribute surprise score — high valence surprise → joy, low valence → sadness
    surprise_score = lyrics_vec[5]
    if audio_features:
        valence = audio_features.get("valence", 0.5)
        if valence >= 0.5:
            lyrics_vec[0] += surprise_score * 0.7  # → joy
            lyrics_vec[4] += surprise_score * 0.3  # → love
        else:
            lyrics_vec[1] += surprise_score * 0.6  # → sadness
            lyrics_vec[3] += surprise_score * 0.4  # → fear
    else:
        lyrics_vec[1] += surprise_score * 0.5  # default → sadness
        lyrics_vec[0] += surprise_score * 0.5  # and joy
    lyrics_vec[5] = 0.0  # zero out surprise

    if audio_features is None:
        total = lyrics_vec.sum()
        if total > 0:
            lyrics_vec = lyrics_vec / total
        return {l: float(lyrics_vec[i]) for i, l in enumerate(labels)}

    v  = audio_features.get("valence",      0.5)
    e  = audio_features.get("energy",       0.5)
    d  = audio_features.get("danceability", 0.5)
    ac = audio_features.get("acousticness", 0.5)

    audio_vec = np.array([
        (v * 0.5 + d * 0.3 + e * 0.2),
        ((1 - v) * 0.5 + ac * 0.3 + (1 - e) * 0.2),
        ((1 - v) * 0.4 + e * 0.4 + (1 - ac) * 0.2),
        ((1 - v) * 0.3 + (1 - e) * 0.4 + (1 - d) * 0.3),
        (v * 0.4 + ac * 0.4 + (1 - e) * 0.2),
        0.0  # surprise always 0
    ])

    audio_sum = audio_vec.sum()
    if audio_sum > 0:
        audio_vec = audio_vec / audio_sum

    fused = LYRICS_WEIGHT * lyrics_vec + AUDIO_WEIGHT * audio_vec
    fused_sum = fused.sum()
    if fused_sum > 0:
        fused = fused / fused_sum

    return {l: float(fused[i]) for i, l in enumerate(labels)}