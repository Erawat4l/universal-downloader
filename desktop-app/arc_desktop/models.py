"""Qt-free target/track/audio normalization helpers matching MainActivity.kt."""

from __future__ import annotations

import re
from typing import Optional


def sort_quality_labels(raw_list: list[str]) -> list[str]:
    """Port of MainActivity.kt's sortQualitiesDescending()."""
    cleaned = [str(q).strip() for q in (raw_list or []) if str(q).strip()]
    if not cleaned:
        return ["Best Available"]
    order = {
        "4k": 4000,
        "2160p": 2160,
        "1440p": 1440,
        "1080p": 1080,
        "720p": 720,
        "480p": 480,
        "360p": 360,
        "240p": 240,
        "auto": 50,
        "best": 10,
    }

    def rank(q: str) -> int:
        low = q.lower()
        m = re.search(r"\b(\d{3,4})(?:p|kbps)?\b", low) or re.search(r"(\d{3,4})p", low)
        if m:
            try:
                return int(m.group(1))
            except ValueError:
                pass
        for k, v in order.items():
            if k in low:
                return v
        return 0

    seen: set[str] = set()
    unique: list[str] = []
    for item in sorted(cleaned, key=rank, reverse=True):
        if item not in seen:
            seen.add(item)
            unique.append(item)
    return unique


def match_quality_key(requested: str, available: list[str]) -> Optional[str]:
    """Find best matching quality label in available options."""
    if not requested or not available:
        return None
    req_low = requested.strip().lower()
    for opt in available:
        if opt.lower() == req_low:
            return opt
    m = re.search(r"(\d{3,4})", req_low)
    if m:
        digits = m.group(1)
        for opt in available:
            if digits in opt:
                return opt
    if "best" in req_low:
        return available[0]
    return None


def prioritize_audio_choices(audios: list[dict]) -> list[dict]:
    """Order multi-audio choices consistently: Hindi -> English -> Original -> regional -> rest."""
    world = [
        "tamil", "telugu", "bengali", "spanish", "french", "arabic",
        "portuguese", "russian", "german", "japanese", "korean", "chinese", "indonesian",
    ]

    def rank(item: dict) -> tuple[int, str]:
        lang = str(item.get("language") or "").lower()
        if "hindi" in lang:
            return (1, lang)
        if "english" in lang:
            return (2, lang)
        if "original" in lang or "default" in lang:
            return (3, lang)
        for idx, w in enumerate(world):
            if w in lang:
                return (10 + idx, lang)
        return (100, lang)

    return sorted([dict(a) for a in (audios or []) if isinstance(a, dict)], key=rank)


def resolve_track_for_quality(
    quality: str,
    quality_tracks: dict[str, dict],
    quality_urls: dict[str, str],
    fallback_direct_url: str,
    fallback_spec: Optional[dict] = None,
) -> dict:
    """Port of MainActivity.kt's resolveTrackForQuality()."""
    fb = dict(fallback_spec or {})
    if quality in quality_tracks and isinstance(quality_tracks[quality], dict):
        return dict(quality_tracks[quality])
    q_low = (quality or "").lower()
    for k, v in quality_tracks.items():
        if not isinstance(v, dict):
            continue
        k_low = k.lower()
        if (
            (q_low and q_low in k_low)
            or (q_low.startswith("2160") and "2160" in k_low)
            or (q_low.startswith("1440") and "1440" in k_low)
            or (q_low.startswith("1080") and "1080" in k_low)
            or (q_low.startswith("720") and "720" in k_low)
            or (q_low.startswith("480") and "480" in k_low)
            or (q_low.startswith("360") and "360" in k_low)
        ):
            return dict(v)
    mapped = str(quality_urls.get(quality) or "").strip()
    if mapped:
        matches_fb = mapped == fb.get("url") and bool(fb.get("video_url")) and bool(fb.get("audio_url"))
        return {
            "url": mapped,
            "video_url": fb.get("video_url", "") if matches_fb else "",
            "audio_url": fb.get("audio_url", "") if matches_fb else "",
            "fallback_url": fb.get("fallback_url", ""),
            "video_format": fb.get("video_format", "") if matches_fb else "",
            "audio_format": fb.get("audio_format", "") if matches_fb else "",
            "fallback_format": fb.get("fallback_format", ""),
        }
    if fb.get("url") or (fb.get("video_url") and fb.get("audio_url")):
        spec = dict(fb)
        if not spec.get("url"):
            spec["url"] = fallback_direct_url
        return spec
    return {"url": fallback_direct_url, "video_url": "", "audio_url": "", "fallback_url": ""}
