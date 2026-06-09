from __future__ import annotations

import getpass
import os
from pathlib import Path

import upload_gmd


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


def main() -> None:
    print("GDmodifier local upload wizard")
    print("This prompts for each value so you do not have to juggle export commands.")
    print("")

    gmd_path = ask("GMD path", "uploads/DaB00bsPlat2.gmd")
    username = ask("GD username", "SchugglyBear")
    account_id = ask("GD accountID", "42450747")
    visibility = ask_choice("Visibility", ["public", "unlisted"], "public")
    upload_mode = ask_choice("Upload mode", ["modern-first", "legacy-first"], "modern-first")
    force_stock_song = ask_choice("Force stock song", ["true", "false"], "true")
    song_id_override = ask("Song ID override, blank normally", "")
    audio_track_override = ask("Audio track override", "0")
    level_name_override = ask("Level name override", "DaB00bsPlat2")

    path = Path(gmd_path)
    if not path.exists():
        print(f"ERROR: file not found: {gmd_path}")
        print("Check your uploads folder with: ls -lh uploads")
        raise SystemExit(1)

    gd_password = getpass.getpass("GD burner password: ")
    if not gd_password:
        print("ERROR: empty password")
        raise SystemExit(1)

    os.environ["GD_PASSWORD"] = gd_password
    os.environ["GMD_PATH"] = gmd_path
    os.environ["GD_USERNAME"] = username
    os.environ["GD_ACCOUNT_ID"] = account_id
    os.environ["VISIBILITY"] = visibility
    os.environ["UPLOAD_MODE"] = upload_mode
    os.environ["FORCE_STOCK_SONG"] = force_stock_song
    os.environ["SONG_ID_OVERRIDE"] = song_id_override
    os.environ["AUDIO_TRACK_OVERRIDE"] = audio_track_override
    os.environ["LEVEL_NAME_OVERRIDE"] = level_name_override
    os.environ["DESCRIPTION_OVERRIDE"] = ""

    try:
        upload_gmd.main()
    finally:
        os.environ.pop("GD_PASSWORD", None)


if __name__ == "__main__":
    main()
