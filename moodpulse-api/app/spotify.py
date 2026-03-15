import httpx
from typing import Optional

SPOTIFY_API = "https://api.spotify.com/v1"

async def fetch_track_features(spotify_id: str, access_token: str) -> Optional[dict]:
    """
    Fetch audio features for a track from Spotify API.
    Returns normalized features useful for mood fusion.
    """
    headers = {"Authorization": f"Bearer {access_token}"}

    async with httpx.AsyncClient(timeout=8.0) as client:
        try:
            resp = await client.get(
                f"{SPOTIFY_API}/audio-features/{spotify_id}",
                headers=headers
            )
            if resp.status_code != 200:
                return None

            data = resp.json()
            return {
                "valence":          data.get("valence", 0.5),       # 0=sad, 1=happy
                "energy":           data.get("energy", 0.5),        # 0=calm, 1=energetic
                "danceability":     data.get("danceability", 0.5),  # 0=stiff, 1=danceable
                "acousticness":     data.get("acousticness", 0.5),  # 0=electric, 1=acoustic
                "instrumentalness": data.get("instrumentalness", 0),# 0=vocals, 1=instrumental
                "tempo":            data.get("tempo", 120),         # BPM
                "loudness":         data.get("loudness", -10),      # dB
            }
        except Exception as e:
            print(f"Spotify API error for {spotify_id}: {e}")
            return None


async def search_track(query: str, access_token: str) -> Optional[dict]:
    """Search for a track and return its Spotify ID + metadata."""
    headers = {"Authorization": f"Bearer {access_token}"}
    params  = {"q": query, "type": "track", "limit": 1}

    async with httpx.AsyncClient(timeout=8.0) as client:
        try:
            resp = await client.get(
                f"{SPOTIFY_API}/search",
                headers=headers,
                params=params
            )
            if resp.status_code != 200:
                return None

            items = resp.json().get("tracks", {}).get("items", [])
            if not items:
                return None

            track = items[0]
            return {
                "id":     track["id"],
                "title":  track["name"],
                "artist": track["artists"][0]["name"],
                "album":  track["album"]["name"],
                "image":  track["album"]["images"][0]["url"] if track["album"]["images"] else None,
                "preview_url": track.get("preview_url"),
            }
        except Exception as e:
            print(f"Spotify search error: {e}")
            return None


async def get_top_tracks(access_token: str, limit: int = 5) -> list:
    """Fetch user's top tracks from the last 4 weeks."""
    headers = {"Authorization": f"Bearer {access_token}"}
    params  = {"time_range": "short_term", "limit": limit}

    async with httpx.AsyncClient(timeout=8.0) as client:
        try:
            resp = await client.get(
                f"{SPOTIFY_API}/me/top/tracks",
                headers=headers,
                params=params
            )
            if resp.status_code != 200:
                return []

            items = resp.json().get("items", [])
            return [
                {
                    "id":     t["id"],
                    "title":  t["name"],
                    "artist": t["artists"][0]["name"],
                    "album":  t["album"]["name"],
                    "image":  t["album"]["images"][0]["url"] if t["album"]["images"] else None,
                    "preview_url": t.get("preview_url"),
                }
                for t in items
            ]
        except Exception as e:
            print(f"Spotify top tracks error: {e}")
            return []