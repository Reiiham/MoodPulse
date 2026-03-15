import httpx
import re
import os
from typing import Optional
from bs4 import BeautifulSoup
from dotenv import load_dotenv
load_dotenv()
GENIUS_TOKEN = os.getenv("GENIUS_TOKEN", "")
GENIUS_API   = "https://api.genius.com"

async def fetch_lyrics(title: str, artist: str) -> Optional[str]:
    """Fetch lyrics from Genius API."""
    if not GENIUS_TOKEN:
        print("Warning: GENIUS_TOKEN not set, skipping lyrics fetch")
        return None

    headers = {"Authorization": f"Bearer {GENIUS_TOKEN}"}
    params  = {"q": f"{title} {artist}"}

    async with httpx.AsyncClient(timeout=10.0) as client:
        try:
            # Step 1 — search for the song
            resp = await client.get(
                f"{GENIUS_API}/search",
                headers=headers,
                params=params
            )
            if resp.status_code != 200:
                return None

            hits = resp.json().get("response", {}).get("hits", [])
            if not hits:
                return None

            # Pick best match
            song_url = None
            for hit in hits[:3]:
                result = hit.get("result", {})
                if artist.lower() in result.get("primary_artist", {}).get("name", "").lower():
                    song_url = result.get("url")
                    break

            if not song_url:
                song_url = hits[0]["result"].get("url")

            if not song_url:
                return None

            # Step 2 — scrape lyrics from Genius page
            resp = await client.get(song_url, follow_redirects=True)
            if resp.status_code != 200:
                return None

            soup = BeautifulSoup(resp.text, "html.parser")

            # Genius uses data-lyrics-container divs
            containers = soup.find_all("div", attrs={"data-lyrics-container": "true"})
            if not containers:
                return None

            lyrics_parts = []
            for container in containers:
                for br in container.find_all("br"):
                    br.replace_with("\n")
                lyrics_parts.append(container.get_text())

            raw_lyrics = "\n".join(lyrics_parts)
            return _clean_lyrics(raw_lyrics)

        except Exception as e:
            print(f"Lyrics fetch error for '{title}': {e}")
            return None


def _clean_lyrics(lyrics: str) -> str:
    """Remove section headers and clean whitespace."""
    lyrics = re.sub(r'\[.*?\]', '', lyrics)
    lyrics = re.sub(r'\n{3,}', '\n\n', lyrics)
    lyrics = lyrics.strip()
    # Return first 400 chars — enough for model, avoids overflow
    return lyrics[:400]