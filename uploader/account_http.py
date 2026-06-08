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
    {"User-Agent": "", "Accept": "*/*"},
    {"Accept": "*/*"},
]


def make_gjp2(secret_text: str) -> str:
    return hashlib.sha1((secret_text + "mI29fmAnxgTs").encode("utf-8")).hexdigest()


def post_account(endpoint: str, payload: dict[str, Any], *, stop_on_negative: bool = False) -> list[Attempt]:
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
                if compact == "1" or ";" in compact:
                    return attempts
                if stop_on_negative and compact.startswith("-"):
                    return attempts
    return attempts


def account_payload_variants(account_id: str, secret_text: str):
    auth = make_gjp2(secret_text)
    return [
        {
            "accountID": account_id,
            "gjp2": auth,
            "secret": ACCOUNT_SECRET,
        },
        {
            "accountID": account_id,
            "gjp2": auth,
            "gameVersion": "22",
            "binaryVersion": "48",
            "secret": ACCOUNT_SECRET,
        },
        {
            "accountID": account_id,
            "gjp2": auth,
            "gameVersion": "22",
            "binaryVersion": "48",
            "gdw": "0",
            "dvs": "2",
            "secret": ACCOUNT_SECRET,
        },
        {
            "accountID": account_id,
            "gjp2": auth,
            "gameVersion": "21",
            "binaryVersion": "35",
            "secret": ACCOUNT_SECRET,
        },
        {
            "accountID": account_id,
            "gjp2": auth,
            "gameVersion": "22",
            "binaryVersion": "48",
            "udid": "S" + account_id,
            "uuid": account_id,
            "dvs": "2",
            "secret": ACCOUNT_SECRET,
        },
    ]


def sync_save(account_id: str, secret_text: str):
    all_attempts: list[Attempt] = []
    for payload in account_payload_variants(account_id, secret_text):
        attempts = post_account("syncGJAccountNew.php", payload, stop_on_negative=False)
        all_attempts.extend(attempts)
        for a in attempts:
            text = a.text.strip()
            if text and not a.blocked_looking and not text.startswith("-") and ";" in text:
                return text.split(";"), all_attempts
    return [], all_attempts


def store_save(account_id: str, secret_text: str, parts: list[str]):
    if len(parts) < 2:
        return False, []
    all_attempts: list[Attempt] = []
    save_data = parts[0] + ";" + parts[1]
    for base_payload in account_payload_variants(account_id, secret_text):
        payload = dict(base_payload)
        payload["gameVersion"] = parts[2] if len(parts) > 2 and parts[2] else payload.get("gameVersion", "22")
        payload["binaryVersion"] = parts[3] if len(parts) > 3 and parts[3] else payload.get("binaryVersion", "48")
        payload["saveData"] = save_data
        attempts = post_account("backupGJAccountNew.php", payload, stop_on_negative=False)
        all_attempts.extend(attempts)
        if any(a.text.strip() == "1" and not a.blocked_looking for a in attempts):
            return True, all_attempts
    return False, all_attempts
