from __future__ import annotations

import json
import os
import re
import sys
from pathlib import Path

from gd_proto import SECRET, build_upload_payloads, lookup_account_id, parse_gmd, post_boomlings

MIN_REASONABLE_LEVEL_ID = 1_000_000


def getenv(name: str, default: str = "") -> str:
    return os.environ.get(name, default).strip()


def as_bool(value: str, default: bool = False) -> bool:
    text = str(value or "").strip().lower()
    if not text:
        return default
    return text in {"1", "true", "yes", "y", "on"}


def summary(text: str) -> None:
    path = os.environ.get("GITHUB_STEP_SUMMARY")
    if path:
        with open(path, "a", encoding="utf-8") as f:
            f.write(text)


def fail(msg: str, attempts=None) -> None:
    print("ERROR:", msg)
    summary("## Upload failed\n\n" + msg + "\n")
    if attempts:
        Path("gdmodifier_debug_attempts.json").write_text(
            json.dumps(
                [
                    {
                        "endpoint": a.endpoint,
                        "url": a.url,
                        "status": a.status,
                        "ua": a.user_agent,
                        "blocked": a.blocked_looking,
                        "elapsed_ms": a.elapsed_ms,
                        "preview": a.preview,
                    }
                    for a in attempts
                ],
                indent=2,
            ),
            encoding="utf-8",
        )
    sys.exit(1)


def print_attempts(title, attempts):
    print("\n---", title, "---")
    for a in attempts:
        print(
            f"{a.endpoint} status={a.status} elapsed={a.elapsed_ms}ms "
            f"ua={a.user_agent!r} blocked={a.blocked_looking} preview={a.preview}"
        )


def looks_like_level_payload(text: str, level_id: str) -> bool:
    body = str(text or "").strip()
    if not body or body.startswith("-"):
        return False
    lowered = body[:1600].lower()
    if "<!doctype html" in lowered or "cloudflare" in lowered or "access denied" in lowered:
        return False
    if level_id in body and ":" in body:
        return True
    return False


def verify_level_id(level_id: str, account_id: str) -> tuple[bool, list]:
    """Try to prove a returned upload number is actually loadable.

    The upload endpoint can return positive numbers that are not usable level IDs.
    Because apparently a number is not always an answer, it is sometimes just a
    little rectangle of lies.
    """
    attempts_all = []

    lookup_payload = {
        "gameVersion": "22",
        "binaryVersion": "48",
        "gdw": "0",
        "type": "0",
        "str": level_id,
        "diff": "-",
        "len": "-",
        "page": "0",
        "total": "0",
        "uncompleted": "0",
        "onlyCompleted": "0",
        "featured": "0",
        "original": "0",
        "twoPlayer": "0",
        "coins": "0",
        "epic": "0",
        "secret": SECRET,
    }
    attempts = post_boomlings("getGJLevels21.php", lookup_payload, stop_on_compact=False)
    attempts_all.extend(attempts)
    if any(looks_like_level_payload(a.text, level_id) for a in attempts):
        return True, attempts_all

    download_payload = {
        "gameVersion": "22",
        "binaryVersion": "48",
        "gdw": "0",
        "levelID": level_id,
        "inc": "0",
        "extras": "0",
        "secret": SECRET,
    }
    if account_id:
        download_payload["accountID"] = str(account_id)

    attempts = post_boomlings("downloadGJLevel22.php", download_payload, stop_on_compact=False)
    attempts_all.extend(attempts)
    if any(looks_like_level_payload(a.text, level_id) for a in attempts):
        return True, attempts_all

    return False, attempts_all


def main() -> None:
    gmd_path = getenv("GMD_PATH")
    username = getenv("GD_USERNAME")
    account_id = getenv("GD_ACCOUNT_ID")
    secret = os.environ.get("GD_PASSWORD", "")
    mode = getenv("UPLOAD_MODE", "modern-first")
    visibility = getenv("VISIBILITY", "public")
    force_stock_song = as_bool(getenv("FORCE_STOCK_SONG"))
    song_id_override = getenv("SONG_ID_OVERRIDE")
    audio_track_override = getenv("AUDIO_TRACK_OVERRIDE")
    verify_upload = as_bool(getenv("VERIFY_UPLOADED_ID"), True)

    if not gmd_path:
        fail("Missing gmd_path input.")
    if not username:
        fail("Missing gd_username input.")
    if not secret:
        fail("Missing repository secret GD_PASSWORD. Use a burner Geometry Dash account.")
    if mode not in {"modern-first", "legacy-first"}:
        fail("Invalid upload_mode. Use modern-first or legacy-first.")
    if visibility not in {"public", "unlisted"}:
        fail("Invalid visibility. Use public or unlisted.")

    p = Path(gmd_path)
    if not p.exists():
        fail(f"Could not find .gmd file: {gmd_path}")
    if not p.is_file():
        fail(f"gmd_path is not a file: {gmd_path}")

    try:
        data = parse_gmd(p)
    except Exception as exc:  # noqa: BLE001 - user-facing workflow error
        fail(f"Could not read .gmd file: {exc}")

    print("Detected level name:", data.get("k2"))
    print("Creator field:", data.get("k5", "(none)"))
    print("Compressed levelString length:", len(str(data.get("k4") or "")))
    print("Objects field k48:", data.get("k48", 0))
    print("Original songID k45:", data.get("k45", 0))
    print("Original audioTrack k8:", data.get("k8", 0))
    print("Force stock song:", force_stock_song)
    print("Song ID override:", song_id_override or "(none)")
    print("Audio track override:", audio_track_override or "(none)")
    print("Verify returned level ID:", verify_upload)

    all_attempts = []
    candidate_ids = []
    if not account_id:
        print("No accountID supplied. Trying lookup...")
        account_id, attempts = lookup_account_id(username)
        all_attempts.extend(attempts)
        print_attempts("lookup", attempts)
        if not account_id:
            fail("Could not auto-find accountID. Re-run with gd_account_id filled manually.", all_attempts)

    print("Using accountID:", account_id)

    try:
        payloads = build_upload_payloads(
            data=data,
            username=username,
            account_id=account_id,
            password=secret,
            mode=mode,
            level_name_override=getenv("LEVEL_NAME_OVERRIDE"),
            description_override=getenv("DESCRIPTION_OVERRIDE"),
            visibility=visibility,
            force_stock_song=force_stock_song,
            song_id_override=song_id_override,
            audio_track_override=audio_track_override,
        )
    except Exception as exc:  # noqa: BLE001 - user-facing workflow error
        fail(f"Could not build upload payload: {exc}", all_attempts)

    for name, payload in payloads:
        print("Uploading with variant:", name)
        print("Payload songID:", payload.get("songID"))
        print("Payload audioTrack:", payload.get("audioTrack"))
        attempts = post_boomlings("uploadGJLevel21.php", payload, stop_on_compact=True)
        all_attempts.extend(attempts)
        print_attempts("upload " + name, attempts)
        for a in attempts:
            response = a.text.strip()
            if re.fullmatch(r"[0-9]+", response) and int(response) > 0:
                candidate_ids.append(response)
                if int(response) < MIN_REASONABLE_LEVEL_ID:
                    print(
                        "Rejected candidate response as fake/too-small level ID:",
                        response,
                    )
                    continue

                if verify_upload:
                    print("Verifying returned level ID:", response)
                    ok, verify_attempts = verify_level_id(response, str(account_id))
                    all_attempts.extend(verify_attempts)
                    print_attempts("verify " + response, verify_attempts)
                    if not ok:
                        print("Returned number did not verify as a loadable/searchable level:", response)
                        continue

                print(f"::notice title=Geometry Dash Level ID::{response}")
                print("SUCCESS. Verified Level ID:" if verify_upload else "SUCCESS. Level ID:", response)
                summary(
                    f"## SUCCESS\n\n**Geometry Dash Level ID:** `{response}`\n\n"
                    "Open Geometry Dash → Search → enter that ID.\n"
                )
                return

    if candidate_ids:
        fail(
            "Upload returned positive number(s), but none verified as a real loadable level ID: "
            + ", ".join(candidate_ids),
            all_attempts,
        )
    fail("Upload failed. Check attempt previews and debug artifact.", all_attempts)


if __name__ == "__main__":
    main()
