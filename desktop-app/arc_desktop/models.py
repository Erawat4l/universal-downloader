"""Qt-free target/track/audio normalization helpers matching MainActivity.kt."""

from __future__ import annotations

from typing import Optional


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
