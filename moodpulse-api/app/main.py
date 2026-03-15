from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel
from typing import List, Optional
import numpy as np
from app.model import EmotionClassifier
from app.spotify import fetch_track_features
from app.lyrics import fetch_lyrics
from app.fusion import compute_mood
from dotenv import load_dotenv
load_dotenv()
app = FastAPI(
    title="MoodPulse API",
    description="Analyze your Spotify top tracks and predict your weekly mood",
    version="1.0.0"
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_methods=["*"],
    allow_headers=["*"],
)

classifier = EmotionClassifier()

# ── Schemas ──────────────────────────────────────────────────

class TrackInput(BaseModel):
    title: str
    artist: str
    spotify_id: Optional[str] = None
    image: Optional[str] = None  # add this

class SongAnalysis(BaseModel):
    title: str
    artist: str
    emotion: str
    emoji: str
    confidence: float
    all_scores: dict
    image: Optional[str] = None  # add this

class MoodResponse(BaseModel):
    dominant_mood: str
    dominant_emoji: str
    confidence: float
    radar_scores: dict
    songs: List[SongAnalysis]
    summary: str

class LyricsRequest(BaseModel):
    tracks: List[TrackInput]
    spotify_access_token: Optional[str] = None
    period: Optional[str] = "medium_term"   # add this


# ── Routes ───────────────────────────────────────────────────

@app.get("/")
def root():
    return {
        "name": "MoodPulse API",
        "status": "running",
        "model": "reiiham/moodpulse-emotion",
        "endpoints": ["/predict", "/health", "/docs"]
    }

@app.get("/health")
def health():
    return {"status": "ok", "model_loaded": classifier.is_loaded()}

@app.post("/predict", response_model=MoodResponse)
async def predict_mood(request: LyricsRequest):
    if not request.tracks:
        raise HTTPException(status_code=400, detail="No tracks provided")
    if len(request.tracks) > 10:
        raise HTTPException(status_code=400, detail="Maximum 10 tracks allowed")

    songs_analysis = []
    all_emotion_scores = np.zeros(6)

    for track in request.tracks:
        lyrics = await fetch_lyrics(track.title, track.artist)

        # Safe fallback — use title + artist as minimal input
        if not lyrics or len(lyrics.strip()) < 10:
            lyrics = f"{track.title} {track.artist}"

        # 2. Run emotion model
        try:
            emotion_scores = classifier.predict(lyrics)
        except Exception as e:
            print(f"Model error for '{track.title}': {e}")
            # Default neutral scores
            emotion_scores = {
                "joy": 0.17, "sadness": 0.17, "anger": 0.17,
                "fear": 0.17, "love": 0.16, "surprise": 0.16
            }

        # 3. Fetch Spotify audio features if token provided
        audio_features = None
        if request.spotify_access_token and track.spotify_id:
            audio_features = await fetch_track_features(
                track.spotify_id,
                request.spotify_access_token
            )

        # 4. Fuse lyrics emotions + audio features
        fused_scores = compute_mood(emotion_scores, audio_features)

        top_label = max(fused_scores, key=fused_scores.get)
        top_score = fused_scores[top_label]
        emoji_map = {
            "joy": "😄", "sadness": "😢", "anger": "😠",
            "fear": "😨", "love": "💖", "surprise": "😲"
        }

        songs_analysis.append(SongAnalysis(
        title=track.title,
        artist=track.artist,
        emotion=top_label,
        emoji=emoji_map[top_label],
        confidence=round(top_score, 4),
        all_scores={k: round(v, 4) for k, v in fused_scores.items()},
        image=track.image if hasattr(track, 'image') else None  # add this
        ))

        for i, label in enumerate(["joy","sadness","anger","fear","love","surprise"]):
            all_emotion_scores[i] += fused_scores[label]

    # Aggregate
    all_emotion_scores /= len(songs_analysis)
    label_names = ["joy","sadness","anger","fear","love","surprise"]
    radar = {label_names[i]: round(float(all_emotion_scores[i]), 4) for i in range(6)}
    dominant_idx = int(np.argmax(all_emotion_scores))
    dominant = label_names[dominant_idx]
    dominant_emoji = list(emoji_map.values())[dominant_idx]

    summary = _build_summary(dominant, radar, songs_analysis)

    return MoodResponse(
    dominant_mood=dominant,
    dominant_emoji=dominant_emoji,
    confidence=round(float(all_emotion_scores[dominant_idx]), 4),
    radar_scores=radar,
    songs=songs_analysis,
    summary=_build_summary(dominant, radar, songs_analysis, request.period)
    )

def _build_summary(dominant: str, radar: dict, songs: list, period: str = "medium_term") -> str:
    period_label = {
        "short_term":  "this month",
        "medium_term": "these past 6 months",
        "long_term":   "all time"
    }.get(period, "recently")

    summaries = {
        "joy":     f"Your playlist is radiating positive energy {period_label}. You're in a great headspace!",
        "sadness": f"Your music taste leans melancholic {period_label}. Sometimes sad songs are the most honest.",
        "anger":   f"High-intensity tracks dominating {period_label}. You've got fire in you right now.",
        "fear":    f"Tense, anxious energy in your listening {period_label}. Your music reflects some uncertainty.",
        "love":    f"Romantic and tender vibes {period_label}. Love is clearly on your mind.",
    }
    return summaries.get(dominant, f"Your mood is complex and multi-layered {period_label}.")
