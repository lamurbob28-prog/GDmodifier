"""Geometry Dash protocol helpers for GDmodifier.

This module uses only Python's standard library so GitHub Actions can run without a
dependency install step.
"""

from __future__ import annotations

import base64
import hashlib
import re
import time
import urllib.error
import urllib.parse
import urllib.request
import xml.etree.ElementTree as ET
from dataclasses import dataclass
from pathlib import Path
from typing import Any, Dict, Iterable, List, Optional, Tuple

SECRET = "Wmfd2893gb7"

BOOMLING_BASES = [
    "https://www.boomlings.com/database/",
    "http://www.boomlings.com/database/",
    "https://boomlings.com/database/",
    "http://boomlings.com/database/",
]

HEADER_VARIANTS = [
    {"User-Agent": "GeometryDash/2.2", "Accept": "*/*"},
    {"User-Agent": "", "Accept": "*/*"},
    {"Accept": "*/*"},
]


@dataclass
class Attempt:
    endpoint: str
    url: str
    status: int
    user_agent: str
    text: str
    elapsed_ms: int

    @property
    def preview(self) -> str:
        return preview(self.text)

    @property
    def blocked_looking(self) -> bool:
        return looks_like_block(self.text)


def preview(text: str, length: int = 260) -> str:
    return str(text or "")[:length].replace("\n", "\\n")


def xor_cipher(text: str, key: str) -> str:
    return "".join(chr(ord(c) ^ ord(key[i % len(key)])) for i, c in enumerate(text))


def b64url_bytes(data: bytes) -> str:
    return base64.urlsafe_b64encode(data).decode("ascii").rstrip("=")


def old_gjp(password: str) -> str:
    """Legacy gjp: password XOR key 37526, URL-safe base64."""
    return b64url_bytes(xor_cipher(password, "37526").encode("utf-8"))


def gjp2(password: str) -> str:
    """Newer GD auth hash used by current clients."""
    return hashlib.sha1((password + "mI29fmAnxgTs").encode("utf-8")).hexdigest()


def upload_seed(level_string: str, chars: int = 50) -> str:
    if len(level_string) < chars:
        return level_string
    step = max(1, len(level_string) // chars)
    return level_string[::step][:chars]


def chk(values: Iterable[Any], key: str, salt: str) -> str:
    raw = "".join(map(str, values)) + salt
    digest = hashlib.sha1(raw.encode("utf-8")).hexdigest()
    return b64url_bytes(xor_cipher(digest, key).encode("utf-8"))


def b64desc(text: str) -> str:
    return b64url_bytes(text.encode("utf-8"))


def _element_children(elem: ET.Element) -> List[ET.Element]:
    return [child for child in list(elem) if isinstance(child.tag, str)]


def _parse_value(node: Optional[ET.Element]) -> Any:
    if node is None:
        return ""
    tag = node.tag
    text = node.text or ""
    if tag in ("s", "string"):
        return text
    if tag in ("i", "integer"):
        try:
            return int(text)
        except ValueError:
            return 0
    if tag in ("r", "real"):
        try:
            return float(text)
        except ValueError:
            return 0.0
    if tag in ("t", "true"):
        return True
    if tag in ("f", "false"):
        return False
    if tag in ("d", "dict"):
        return _parse_dict(node)
    return text


def _parse_dict(elem: ET.Element) -> Dict[str, Any]:
    kids = _element_children(elem)
    out: Dict[str, Any] = {}
    i = 0
    while i < len(kids):
        key_node = kids[i]
        if key_node.tag not in ("k", "key"):
            i += 1
            continue
        key = key_node.text or ""
        value_node = kids[i + 1] if i + 1 < len(kids) else None
        out[key] = _parse_value(value_node)
        i += 2
    return out


def first_plist_dict(root: ET.Element) -> ET.Element:
    root_dict = root.find("dict")
    if root_dict is None:
        root_dict = root.find("d")
    if root_dict is None:
        raise ValueError("No plist dictionary found in .gmd.")
    return root_dict


def parse_gmd(path: str | Path) -> Dict[str, Any]:
    path = Path(path)
    try:
        text = path.read_text(encoding="utf-8", errors="replace")
    except OSError as exc:
        raise ValueError(f"Could not read .gmd file: {exc}") from exc

    try:
        root = ET.fromstring(text)
    except ET.ParseError as exc:
        raise ValueError(
            "Could not parse .gmd as XML/plist. Re-export the level as a plist-style "
            ".gmd file, not a raw/compressed level string."
        ) from exc

    data = _parse_dict(first_plist_dict(root))
    missing = [key for key in ("k2", "k4") if key not in data]
    if missing:
        raise ValueError(
            "Missing required GD level keys "
            + ", ".join(missing)
            + "; this does not look like a normal .gmd export."
        )
    return data


def count_coins(data: Dict[str, Any]) -> int:
    return sum(1 for key in ("k61", "k62", "k63") if data.get(key))


def as_int(value: Any, default: int = 0) -> int:
    try:
        return int(value)
    except (TypeError, ValueError):
        return default


def optional_int(value: Any) -> Optional[int]:
    text = str(value or "").strip()
    if text == "":
        return None
    try:
        return int(text)
    except ValueError as exc:
        raise ValueError(f"Expected integer override, got {text!r}.") from exc


def parse_colon_map(text: str) -> Dict[str, str]:
    clean = str(text or "").split("#")[0]
    parts = clean.split(":")
    out: Dict[str, str] = {}
    for i in range(0, len(parts) - 1, 2):
        out[parts[i]] = parts[i + 1]
    return out


def looks_like_block(text: str) -> bool:
    lowered = str(text or "")[:1600].lower()
    return any(
        marker in lowered
        for marker in (
            "<!doctype html",
            "cloudflare",
            "error 1020",
            "access denied",
            "attention required",
            "checking your browser",
        )
    )


def is_positive_int_response(text: str) -> bool:
    stripped = str(text or "").strip()
    return bool(re.fullmatch(r"[0-9]+", stripped) and int(stripped) > 0)


def is_colon_data_response(text: str) -> bool:
    stripped = str(text or "").strip()
    return ":" in stripped and not stripped.startswith("-") and not looks_like_block(stripped)


def post_boomlings(endpoint: str, payload: Dict[str, Any], *, stop_on_compact: bool = False) -> List[Attempt]:
    encoded = urllib.parse.urlencode({k: str(v) for k, v in payload.items()}).encode("utf-8")
    attempts: List[Attempt] = []

    for base in BOOMLING_BASES:
        for headers in HEADER_VARIANTS:
            url = base + endpoint
            merged_headers = {"Content-Type": "application/x-www-form-urlencoded", **headers}
            req = urllib.request.Request(url, data=encoded, headers=merged_headers, method="POST")
            start = time.monotonic()
            try:
                with urllib.request.urlopen(req, timeout=30) as resp:
                    body = resp.read().decode("utf-8", errors="replace").strip()
                    status = resp.status
            except urllib.error.HTTPError as exc:
                body = exc.read().decode("utf-8", errors="replace").strip()
                status = exc.code
            except Exception as exc:  # noqa: BLE001 - we want every endpoint failure in the debug artifact
                body = repr(exc)
                status = 0

            elapsed_ms = int((time.monotonic() - start) * 1000)
            attempt = Attempt(
                endpoint=endpoint,
                url=url,
                status=status,
                user_agent=str(headers.get("User-Agent", "(none)")),
                text=body,
                elapsed_ms=elapsed_ms,
            )
            attempts.append(attempt)

            if stop_on_compact and status and not attempt.blocked_looking:
                stripped = body.strip()
                # Positive numbers are successful upload IDs. Colon payloads are
                # successful lookup-style responses. Do not stop on -1; that just
                # means this variant was rejected and the next endpoint/header
                # combination may still work.
                if is_positive_int_response(stripped) or is_colon_data_response(stripped):
                    return attempts

    return attempts


def lookup_account_id(username: str) -> Tuple[Optional[str], List[Attempt]]:
    all_attempts: List[Attempt] = []
    variants = [
        {
            "gameVersion": "22",
            "binaryVersion": "48",
            "gdw": "0",
            "str": username,
            "page": "0",
            "total": "0",
            "secret": SECRET,
        },
        {
            "gameVersion": "21",
            "binaryVersion": "35",
            "gdw": "0",
            "str": username,
            "page": "0",
            "total": "0",
            "secret": SECRET,
        },
    ]
    for payload in variants:
        attempts = post_boomlings("getGJUsers20.php", payload, stop_on_compact=True)
        all_attempts.extend(attempts)
        for attempt in attempts:
            parsed = parse_colon_map(attempt.text)
            if parsed.get("16"):
                return parsed["16"], all_attempts
    return None, all_attempts


def build_upload_payloads(
    *,
    data: Dict[str, Any],
    username: str,
    account_id: str,
    password: str,
    mode: str,
    level_name_override: str = "",
    description_override: str = "",
    visibility: str = "public",
    force_stock_song: bool = False,
    song_id_override: str = "",
    audio_track_override: str = "",
) -> List[Tuple[str, Dict[str, Any]]]:
    if mode not in {"modern-first", "legacy-first"}:
        raise ValueError("upload_mode must be modern-first or legacy-first.")
    if visibility not in {"public", "unlisted"}:
        raise ValueError("visibility must be public or unlisted.")

    level_string = str(data.get("k4") or "")
    if len(level_string) < 20:
        raise ValueError("Bad/missing k4 levelString.")

    level_name = (level_name_override.strip() or str(data.get("k2") or "Imported GMD"))[:40]
    level_desc = b64desc(description_override.strip()) if description_override.strip() else str(data.get("k3") or "")

    song_override = optional_int(song_id_override)
    track_override = optional_int(audio_track_override)
    original_song_id = as_int(data.get("k45"), 0)
    original_audio_track = as_int(data.get("k8"), 0)

    if force_stock_song:
        song_id = 0
        audio_track = track_override if track_override is not None else 0
    elif song_override is not None:
        song_id = song_override
        audio_track = track_override if track_override is not None else (0 if song_id > 0 else original_audio_track)
    else:
        song_id = original_song_id
        audio_track = track_override if track_override is not None else (0 if song_id > 0 else original_audio_track)

    common = {
        "accountID": str(account_id),
        "gjp2": gjp2(password),
        "gjp": old_gjp(password),
        "userName": username,
        "levelID": "0",
        "levelName": level_name,
        "levelDesc": level_desc,
        "levelVersion": str(as_int(data.get("k16"), 1)),
        "levelLength": str(as_int(data.get("k23"), 0)),
        "audioTrack": str(audio_track),
        "auto": "0",
        "password": "1",
        "original": "0",
        "twoPlayer": str(as_int(data.get("k43"), 0)),
        "songID": str(song_id),
        "objects": str(as_int(data.get("k48"), 0)),
        "coins": str(count_coins(data)),
        "requestedStars": str(as_int(data.get("k66"), 0)),
        "unlisted": "0" if visibility == "public" else "1",
        "ldm": str(as_int(data.get("k72"), 0)),
        "levelString": level_string,
        "seed2": chk([upload_seed(level_string)], "41274", "xI25fpAapCQg"),
        "secret": SECRET,
    }

    modern = {
        **common,
        "gameVersion": "22",
        "binaryVersion": "48",
        "gdw": "0",
        "dvs": "2",
    }
    legacy = {
        **common,
        "gameVersion": "21",
        "binaryVersion": "35",
        "gdw": "0",
    }

    if mode == "legacy-first":
        return [("legacy-first", legacy), ("modern-second", modern)]
    return [("modern-first", modern), ("legacy-second", legacy)]
