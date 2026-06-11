from __future__ import annotations

import getpass
import hashlib
import json
import os
import subprocess
import time
from pathlib import Path
from typing import Any

from gd_proto import build_upload_payloads, lookup_account_id, parse_gmd, post_boomlings

REMOTE_REF = "origin/main"
DEBUG_PATH = Path("last_upload_debug.json")


def ask(prompt: str, default: str = "") -> str:
    shown = f"{prompt} [{default}]: " if default else f"{prompt}: "
    value = input(shown).strip()
    return value or default


def ask_choice(prompt: str, choices: list[str], default: str) -> str:
    while True:
        value = ask(prompt, default)
        if value in choices:
            return value
        print("Choose one of: " + ", ".join(choices))


def run_git_text(args: list[str]) -> str:
    proc = subprocess.run(["git", *args], text=True, capture_output=True)
    if proc.returncode != 0:
        raise RuntimeError((proc.stderr or proc.stdout or "git command failed").strip())
    return proc.stdout


def run_git_bytes(args: list[str]) -> bytes:
    proc = subprocess.run(["git", *args], capture_output=True)
    if proc.returncode != 0:
        msg = (proc.stderr or proc.stdout or b"git command failed").decode("utf-8", errors="replace")
        raise RuntimeError(msg.strip())
    return proc.stdout


def fetch_main_uploads() -> None:
    print("Syncing GitHub main/uploads...")
    run_git_text(["fetch", "origin", "main", "--quiet"])


def list_github_uploads() -> list[str]:
    fetch_main_uploads()
    output = run_git_text(["ls-tree", "-r", "--name-only", REMOTE_REF, "--", "uploads"])
    files: list[str] = []
    for line in output.splitlines():
        item = line.strip()
        if item.lower().endswith(".gmd"):
            files.append(item)
    return sorted(files)


def copy_github_file(path: str) -> str:
    blob = run_git_bytes(["show", f"{REMOTE_REF}:{path}"])
    out_dir = Path("generated_uploads")
    out_dir.mkdir(exist_ok=True)
    stamp = time.strftime("%Y%m%d_%H%M%S")
    out_path = out_dir / f"{stamp}_{Path(path).name}"
    out_path.write_bytes(blob)
    return str(out_path)


def pick_gmd() -> tuple[str, str]:
    files = list_github_uploads()
    if not files:
        raise SystemExit("ERROR: no .gmd files are tracked in GitHub main/uploads.")

    print("\nTracked GitHub uploads:")
    for i, file in enumerate(files, start=1):
        print(f"{i}. {file}")

    while True:
        choice = input("\nPick file number: ").strip()
        if choice.isdigit() and 1 <= int(choice) <= len(files):
            source = files[int(choice) - 1]
            local_copy = copy_github_file(source)
            return source, local_copy
        print("Pick a number from the list. Nothing else. The wizard is done accepting chaos.")


def sha256_text(text: str) -> str:
    return hashlib.sha256(text.encode("utf-8", errors="replace")).hexdigest()


def inspect_gmd(path: str) -> dict[str, Any]:
    data = parse_gmd(path)
    level_string = str(data.get("k4") or "")
    return {
        "k1_original_level_id": str(data.get("k1", "")),
        "k2_level_name": str(data.get("k2", "")),
        "k4_level_string_length": len(level_string),
        "k4_level_string_sha256": sha256_text(level_string),
        "k5_creator_field": str(data.get("k5", "")),
        "k8_audio_track": str(data.get("k8", 0)),
        "k16_level_version": str(data.get("k16", 1)),
        "k23_level_length": str(data.get("k23", 0)),
        "k45_song_id": str(data.get("k45", 0)),
        "k48_objects": str(data.get("k48", 0)),
    }


def print_inspection(source: str, local_copy: str, info: dict[str, Any]) -> None:
    print("\nUpload inspection")
    print("GitHub source file:", source)
    print("Generated local copy:", local_copy)
    print("Internal name k2:", info["k2_level_name"])
    print("Original online ID k1:", info["k1_original_level_id"] or "(blank)")
    print("Objects k48:", info["k48_objects"])
    print("Song k45:", info["k45_song_id"])
    print("LevelString length:", info["k4_level_string_length"])
    print("LevelString sha256:", info["k4_level_string_sha256"][:16] + "...")


def redact_payload(payload: dict[str, Any]) -> dict[str, Any]:
    safe = dict(payload)
    for key in ["gjp", "gjp2"]:
        if key in safe:
            safe[key] = "<redacted>"
    level_string = str(safe.get("levelString", ""))
    safe["levelString"] = f"<omitted length={len(level_string)} sha256={sha256_text(level_string)}>"
    return safe


def attempt_to_dict(attempt: Any) -> dict[str, Any]:
    return {
        "endpoint": attempt.endpoint,
        "url": attempt.url,
        "status": attempt.status,
        "user_agent": attempt.user_agent,
        "blocked_looking": attempt.blocked_looking,
        "elapsed_ms": attempt.elapsed_ms,
        "preview": attempt.preview,
    }


def save_debug(debug: dict[str, Any]) -> None:
    DEBUG_PATH.write_text(json.dumps(debug, indent=2), encoding="utf-8")
    print("Debug receipt saved:", DEBUG_PATH)


def main() -> None:
    print("GDmodifier Safe Uploader v2")
    print("One source: GitHub main/uploads. One mode: CREATE NEW with payload levelID=0.")
    print("This tool copies the selected .gmd to generated_uploads/ and does not modify level data.\n")

    source, local_copy = pick_gmd()
    try:
        data = parse_gmd(local_copy)
        info = inspect_gmd(local_copy)
    except Exception as exc:
        raise SystemExit(f"ERROR: selected .gmd could not be parsed: {exc}") from exc

    print_inspection(source, local_copy, info)

    default_name = (info["k2_level_name"] or Path(source).stem)[:40]
    username = ask("GD username", "BrotherOnGod")
    account_id = ask("GD accountID (blank = auto lookup)", "")
    visibility = ask_choice("Visibility", ["public", "unlisted"], "unlisted")
    force_stock_song = ask_choice("Force stock song", ["true", "false"], "false")
    song_id_override = ask("Song ID override, blank normally", "")
    audio_track_override = ask("Audio track override", "0")
    level_name_override = ask("Online level name", default_name)

    print("\nCREATE-NEW payload preview")
    print("Payload levelID: 0")
    print("Online level name:", level_name_override)
    print("Username:", username)
    print("AccountID:", account_id or "auto lookup")
    print("Visibility:", visibility)
    print("Force stock song:", force_stock_song)
    print("Song override:", song_id_override or "(none)")
    print("Audio track override:", audio_track_override or "(none)")
    print("Internal k1 remains in file but is NOT sent as payload levelID:", info["k1_original_level_id"] or "(blank)")

    confirm = input("\nType UPLOAD to continue: ").strip()
    if confirm != "UPLOAD":
        raise SystemExit("Cancelled before upload.")

    gd_password = getpass.getpass("GD burner password: ")
    if not gd_password:
        raise SystemExit("ERROR: empty password")

    debug: dict[str, Any] = {
        "source": source,
        "generated_local_copy": local_copy,
        "inspection": info,
        "settings": {
            "username": username,
            "account_id_input": account_id,
            "visibility": visibility,
            "force_stock_song": force_stock_song,
            "song_id_override": song_id_override,
            "audio_track_override": audio_track_override,
            "level_name_override": level_name_override,
            "create_new_payload_levelID": "0",
        },
        "lookup_attempts": [],
        "upload_variants": [],
        "success_level_id": None,
    }

    all_attempts = []
    if not account_id:
        print("\nLooking up account ID...")
        account_id, lookup_attempts = lookup_account_id(username)
        all_attempts.extend(lookup_attempts)
        debug["lookup_attempts"] = [attempt_to_dict(a) for a in lookup_attempts]
        if not account_id:
            save_debug(debug)
            raise SystemExit("ERROR: account ID lookup failed. Check username or type accountID manually.")
    debug["settings"]["resolved_account_id"] = account_id
    print("Using accountID:", account_id)

    try:
        payloads = build_upload_payloads(
            data=data,
            username=username,
            account_id=account_id,
            password=gd_password,
            mode="modern-first",
            level_name_override=level_name_override,
            description_override="",
            visibility=visibility,
            force_stock_song=(force_stock_song == "true"),
            song_id_override=song_id_override,
            audio_track_override=audio_track_override,
        )
    except Exception as exc:
        save_debug(debug)
        raise SystemExit(f"ERROR: could not build upload payload: {exc}") from exc

    for variant_name, payload in payloads:
        variant_debug = {
            "variant": variant_name,
            "payload": redact_payload(payload),
            "attempts": [],
        }
        debug["upload_variants"].append(variant_debug)
        print("\nUploading variant:", variant_name)
        print("Payload levelID:", payload.get("levelID"))
        print("Payload levelName:", payload.get("levelName"))
        print("Payload objects:", payload.get("objects"))
        print("Payload songID:", payload.get("songID"))
        print("Payload audioTrack:", payload.get("audioTrack"))

        attempts = post_boomlings("uploadGJLevel21.php", payload, stop_on_compact=True)
        all_attempts.extend(attempts)
        variant_debug["attempts"] = [attempt_to_dict(a) for a in attempts]

        for attempt in attempts:
            print(
                f"{attempt.endpoint} status={attempt.status} elapsed={attempt.elapsed_ms}ms "
                f"blocked={attempt.blocked_looking} preview={attempt.preview}"
            )
            response = attempt.text.strip()
            if response.isdigit() and int(response) > 0:
                debug["success_level_id"] = response
                save_debug(debug)
                print("SUCCESS. Level ID:", response)
                print("Search this exact ID in Geometry Dash. Do not search by name.")
                return

    save_debug(debug)
    raise SystemExit("ERROR: upload failed. Open last_upload_debug.json for the receipt.")


if __name__ == "__main__":
    main()
