from __future__ import annotations

import getpass
import os
import subprocess
import time
from pathlib import Path

import upload_gmd
from gd_proto import parse_gmd

REMOTE_REF = "origin/main"


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


def git_text(args: list[str]) -> str:
    proc = subprocess.run(["git", *args], text=True, capture_output=True)
    if proc.returncode != 0:
        raise RuntimeError((proc.stderr or proc.stdout or "git command failed").strip())
    return proc.stdout


def git_bytes(args: list[str]) -> bytes:
    proc = subprocess.run(["git", *args], capture_output=True)
    if proc.returncode != 0:
        msg = (proc.stderr or proc.stdout or b"git command failed").decode("utf-8", errors="replace")
        raise RuntimeError(msg.strip())
    return proc.stdout


def fetch_github_uploads() -> None:
    print("Fetching GitHub uploads from main...")
    git_text(["fetch", "origin", "main", "--quiet"])


def github_upload_files() -> list[str]:
    fetch_github_uploads()
    output = git_text(["ls-tree", "-r", "--name-only", REMOTE_REF, "--", "uploads"])
    files = []
    for line in output.splitlines():
        clean = line.strip()
        if clean.lower().endswith(".gmd"):
            files.append(clean)
    return sorted(files)


def copy_from_github_upload(path: str) -> str:
    data = git_bytes(["show", f"{REMOTE_REF}:{path}"])
    out_dir = Path("generated_uploads")
    out_dir.mkdir(exist_ok=True)
    safe_name = Path(path).name
    stamp = time.strftime("%Y%m%d_%H%M%S")
    out_path = out_dir / f"{stamp}_{safe_name}"
    out_path.write_bytes(data)
    return str(out_path)


def detected_level_name(gmd_path: str | Path) -> str:
    try:
        data = parse_gmd(gmd_path)
        return str(data.get("k2") or Path(gmd_path).stem)[:40]
    except Exception:
        return Path(gmd_path).stem[:40]


def choose_github_gmd() -> str:
    print("Step 1: choose the .gmd file from GitHub main/uploads")
    try:
        files = github_upload_files()
    except Exception as exc:
        print("ERROR: could not read GitHub uploads folder.")
        print(exc)
        raise SystemExit(1)

    if not files:
        print("ERROR: no .gmd files found in GitHub main/uploads.")
        raise SystemExit(1)

    for index, file in enumerate(files, start=1):
        print(f"{index}. {file}")
    print("")

    while True:
        choice = input("Type file number: ").strip()
        if choice.isdigit():
            index = int(choice)
            if 1 <= index <= len(files):
                selected = files[index - 1]
                local_copy = copy_from_github_upload(selected)
                print("GitHub file:", selected)
                print("Generated local copy:", local_copy)
                return local_copy
        print("Pick a number from the list.")


def main() -> None:
    print("GDmodifier local upload wizard")
    print("This uploader ONLY selects .gmd files tracked in GitHub main/uploads.")
    print("It ignores leftover local uploads/ files and creates a generated copy before uploading.")
    print("")

    gmd_path = choose_github_gmd()
    level_name_default = detected_level_name(gmd_path)
    print("")
    print("Selected generated file:", gmd_path)
    print("Detected level name:", level_name_default)
    print("")

    username = ask("GD username", "BrotherOnGod")
    account_id = ask("GD accountID (blank = auto lookup)", "")
    visibility = ask_choice("Visibility", ["public", "unlisted"], "public")
    upload_mode = ask_choice("Upload mode", ["modern-first", "legacy-first"], "modern-first")
    force_stock_song = ask_choice("Force stock song", ["true", "false"], "true")
    song_id_override = ask("Song ID override, blank normally", "")
    audio_track_override = ask("Audio track override", "0")
    level_name_override = ask("Level name override", level_name_default)

    path = Path(gmd_path)
    if not path.exists():
        print(f"ERROR: generated file not found: {gmd_path}")
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
