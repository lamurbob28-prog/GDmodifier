from __future__ import annotations

import getpass
import os
from pathlib import Path

import upload_gmd
from gd_proto import parse_gmd


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


def find_gmd_files() -> list[Path]:
    uploads = Path("uploads")
    if not uploads.exists():
        return []
    files = sorted(list(uploads.glob("*.gmd")) + list(uploads.glob("*.GMD")))
    # Remove duplicates on case-insensitive weirdness.
    out: list[Path] = []
    seen: set[str] = set()
    for file in files:
        key = str(file)
        if key not in seen:
            seen.add(key)
            out.append(file)
    return out


def choose_gmd_path() -> str:
    files = find_gmd_files()
    print("Step 1: choose the .gmd file")

    if files:
        for index, file in enumerate(files, start=1):
            print(f"{index}. {file}")
        print("")
        while True:
            choice = input("Type file number, or type a path manually: ").strip()
            if not choice and len(files) == 1:
                return str(files[0])
            if choice.isdigit():
                index = int(choice)
                if 1 <= index <= len(files):
                    return str(files[index - 1])
                print("That number is not in the list.")
                continue
            if choice:
                path = Path(choice)
                if path.exists():
                    return str(path)
                print(f"File not found: {choice}")
                continue
            print("Pick a number from the list. No more accidental default-file nonsense.")

    print("No .gmd files found in uploads/.")
    while True:
        choice = input("Type the .gmd path manually: ").strip()
        if choice and Path(choice).exists():
            return choice
        print("File not found. Check with: ls -lh uploads")


def detected_level_name(gmd_path: str) -> str:
    try:
        data = parse_gmd(gmd_path)
        return str(data.get("k2") or Path(gmd_path).stem)[:40]
    except Exception:
        return Path(gmd_path).stem[:40]


def main() -> None:
    print("GDmodifier local upload wizard")
    print("This is the classic working uploader, now with file selection so it does not default-trap you into DaB00bs.")
    print("")

    gmd_path = choose_gmd_path()
    level_name_default = detected_level_name(gmd_path)
    print("")
    print("Selected file:", gmd_path)
    print("Detected level name:", level_name_default)
    print("")

    username = ask("GD username", "SchugglyBear")
    account_id = ask("GD accountID", "42450747")
    visibility = ask_choice("Visibility", ["public", "unlisted"], "public")
    upload_mode = ask_choice("Upload mode", ["modern-first", "legacy-first"], "modern-first")
    force_stock_song = ask_choice("Force stock song", ["true", "false"], "true")
    song_id_override = ask("Song ID override, blank normally", "")
    audio_track_override = ask("Audio track override", "0")
    level_name_override = ask("Level name override", level_name_default)

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
