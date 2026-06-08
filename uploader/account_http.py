from __future__ import annotations

import hashlib
import time
import urllib.error
import urllib.parse
import urllib.request
from typing import Any

from gd_proto import Attempt, looks_like_block

ACCOUNT_SECRET = "Wmfv3899gc9"
ACCOUNT_URLS = [
    "https://www.robtopgames.org/database/accounts/",
    "http://www.robtopgames.org/database/accounts/",
]
ACCOUNT_HEADERS = [
    {"User-Agent": "GeometryDash/2.2", "Accept": "*/*"},
    {"Accept": "*/*"},
]


def make_gjp2(secret_text: str) -> str:
    return hashlib.sha1((secret_text + "mI29fmAnxgTs").encode("utf-8")).hexdigest()


def post_account(endpoint: str, payload: dict[str, Any]) -> list[Attempt]:
    encoded = urllib.parse.urlencode({k: str(v) for k, v in payload.items()}).encode("utf-8")
    attempts: list[Attempt] = []
    for base in ACCOUNT_URLS:
        for headers in ACCOUNT_HEADERS:
            req = urllib.request.Request(
                base + endpoint,
                data=encoded,
                headers={"Content-Type": "application/x-www-form-urlencoded", **headers},
                method="POST",
            )
            start = time.monotonic()
            try:
                with urllib.request.urlopen(req, timeout=30) as resp:
                    text = resp.read().decode("utf-8", errors="replace").strip()
                    status = resp.status
            except urllib.error.HTTPError as exc:
                text = exc.read().decode("utf-8", errors="replace").strip()
                status = exc.code
            except Exception as exc:
                text = repr(exc)
                status = 0
            attempt = Attempt(endpoint, base + endpoint, status, str(headers.get("User-Agent", "(none)")), text, int((time.monotonic() - start) * 1000))
            attempts.append(attempt)
            if status and not looks_like_block(text):
                compact = text.strip()
                if compact == "1" or compact.startswith("-") or ";" in compact:
                    return attempts
    return attempts


def sync_save(account_id: str, secret_text: str):
    payload = {
        "accountID": account_id,
        "gjp2": make_gjp2(secret_text),
        "gameVersion": "22",
        "binaryVersion": "48",
        "udid": account_id,
        "uuid": account_id,
        "dvs": "2",
        "secret": ACCOUNT_SECRET,
    }
    attempts = post_account("syncGJAccountNew.php", payload)
    for a in attempts:
        text = a.text.strip()
        if text and not a.blocked_looking and not text.startswith("-") and ";" in text:
            return text.split(";"), attempts
    return [], attempts


def store_save(account_id: str, secret_text: str, parts: list[str]):
    if len(parts) < 2:
        return False, []
    payload = {
        "accountID": account_id,
        "gjp2": make_gjp2(secret_text),
        "gameVersion": parts[2] if len(parts) > 2 and parts[2] else "22",
        "binaryVersion": parts[3] if len(parts) > 3 and parts[3] else "48",
        "udid": account_id,
        "uuid": account_id,
        "dvs": "2",
        "saveData": parts[0] + ";" + parts[1],
        "secret": ACCOUNT_SECRET,
    }
    attempts = post_account("backupGJAccountNew.php", payload)
    return any(a.text.strip() == "1" and not a.blocked_looking for a in attempts), attempts
