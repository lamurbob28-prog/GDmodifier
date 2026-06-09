from __future__ import annotations

import getpass
import json
import os
import shutil
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent
UPLOADER = ROOT / "uploader"
sys.path.insert(0, str(UPLOADER))

from gd_proto import lookup_account_id, parse_gmd  # noqa: E402
import upload_gmd  # noqa: E402

CONFIG_PATH = ROOT / ".gdmodifier_config.json"
UPLOADS = ROOT / "uploads"
DOWNLOADS = Path.home() / "storage" / "downloads"


def clearish() -> None:
    print("\n" + "=" * 48)


def pause() -> None:
    input("\nPress Enter to continue...")


def load_config() -> dict:
    if CONFIG_PATH.exists():
        try:
            return json.loads(CONFIG_PATH.read_text(encoding="utf-8"))
        except Exception:
            return {}
    return {}


def save_config(config: dict) -> None:
    safe = {
        "username": config.get("username", ""),
        "account_id": config.get("account_id", ""),
        "last_file": config.get("last_file", ""),
    }
    CONFIG_PATH.write_text(json.dumps(safe, indent=2), encoding="utf-8")


def yes_no(prompt: str, default: bool = True) -> bool:
    suffix = "Y/n" if default else "y/N"
    while True:
        value = input(f"{prompt} [{suffix}]: ").strip().lower()
        if not value:
            return default
        if value in {"y", "yes"}:
            return True
        if value in {"n", "no"}:
            return False
        print("Type y or n.")


def find_gmd_files() -> list[Path]:
    UPLOADS.mkdir(exist_ok=True)
    found: list[Path] = []
    for folder in (UPLOADS, DOWNLOADS):
        if folder.exists():
            found.extend(sorted(folder.glob("*.gmd")))
            found.extend(sorted(folder.glob("*.GMD")))
    unique: list[Path] = []
    seen = set()
    for path in found:
        key = str(path.resolve())
        if key not in seen:
            seen.add(key)
            unique.append(path)
    return unique


def choose_file(config: dict) -> Path:
    clearish()
    print("Step 1: Choose the .gmd file")
    files = find_gmd_files()
    if not files:
        print("I could not find any .gmd files in uploads or Downloads.")
        print("Put your .gmd file in Android Downloads, then run this again.")
        raise SystemExit(1)

    for i, path in enumerate(files, start=1):
        where = "uploads" if UPLOADS in path.parents else "downloads"
        print(f"{i}. {path.name} ({where})")

    while True:
        value = input("\nType the number of the file to upload: ").strip()
        try:
            index = int(value)
        except ValueError:
            print("Type a number from the list.")
            continue
        if 1 <= index <= len(files):
            selected = files[index - 1]
            break
        print("That number is not in the list.")

    if UPLOADS not in selected.parents:
        target = UPLOADS / selected.name
        shutil.copy2(selected, target)
        selected = target
        print(f"Copied into uploads: {selected.name}")

    rel = selected.relative_to(ROOT)
    config["last_file"] = str(rel)
    return rel


def choose_account(config: dict) -> tuple[str, str]:
    clearish()
    print("Step 2: Choose the GD account")
    saved_user = config.get("username") or ""
    saved_id = config.get("account_id") or ""

    if saved_user and saved_id and yes_no(f"Use saved account {saved_user} ({saved_id})?", True):
        return saved_user, saved_id

    username = input("GD username: ").strip()
    if not username:
        print("No username entered.")
        raise SystemExit(1)

    print("Looking up accountID...")
    account_id, attempts = lookup_account_id(username)
    if account_id:
        print(f"Found accountID: {account_id}")
    else:
        print("Could not auto-find accountID.")
        print("You can paste it manually if you know it.")
        account_id = input("GD accountID: ").strip()
        if not account_id:
            raise SystemExit(1)

    config["username"] = username
    config["account_id"] = account_id
    save_config(config)
    return username, account_id


def show_file_info(gmd_path: Path) -> str:
    try:
        data = parse_gmd(gmd_path)
    except Exception as exc:
        print(f"Could not read .gmd: {exc}")
        raise SystemExit(1)
    level_name = str(data.get("k2") or gmd_path.stem)[:40]
    print(f"Level name found: {level_name}")
    print(f"Objects: {data.get('k48', 0)}")
    print(f"Level string length: {len(str(data.get('k4') or ''))}")
    return level_name


def run_once(*, gmd_path: str, username: str, account_id: str, password: str, mode: str, level_name: str) -> bool:
    os.environ["GD_PASSWORD"] = password
    os.environ["GMD_PATH"] = gmd_path
    os.environ["GD_USERNAME"] = username
    os.environ["GD_ACCOUNT_ID"] = account_id
    os.environ["VISIBILITY"] = "public"
    os.environ["UPLOAD_MODE"] = mode
    os.environ["FORCE_STOCK_SONG"] = "true"
    os.environ["SONG_ID_OVERRIDE"] = ""
    os.environ["AUDIO_TRACK_OVERRIDE"] = "0"
    os.environ["LEVEL_NAME_OVERRIDE"] = level_name
    os.environ["DESCRIPTION_OVERRIDE"] = ""
    try:
        upload_gmd.main()
        return True
    except SystemExit as exc:
        return int(exc.code or 0) == 0
    finally:
        os.environ.pop("GD_PASSWORD", None)


def main() -> None:
    print("GDmodifier Easy Upload")
    print("Answer a few prompts. Press Enter only when asked.")

    config = load_config()
    rel_file = choose_file(config)
    username, account_id = choose_account(config)

    clearish()
    print("Step 3: Confirm level")
    level_name = show_file_info(ROOT / rel_file)
    custom = input(f"Upload name [{level_name}]: ").strip()
    if custom:
        level_name = custom[:40]

    clearish()
    print("Step 4: Password")
    print("Type the burner password. It will NOT show while typing.")
    password = getpass.getpass("GD burner password: ")
    if not password:
        print("Empty password. Stopping.")
        raise SystemExit(1)

    clearish()
    print("Uploading now. Do not type while it runs.")
    print("Trying modern-first...")
    ok = run_once(
        gmd_path=str(rel_file),
        username=username,
        account_id=account_id,
        password=password,
        mode="modern-first",
        level_name=level_name,
    )
    if ok:
        print("\nDone.")
        return

    print("\nModern-first failed. Trying legacy-first automatically...")
    ok = run_once(
        gmd_path=str(rel_file),
        username=username,
        account_id=account_id,
        password=password,
        mode="legacy-first",
        level_name=level_name,
    )
    if ok:
        print("\nDone.")
        return

    print("\nUpload still failed.")
    print("The file/account/network reached the server, but GD rejected the upload.")
    print("Try a different burner account, different network, or a simpler .gmd file.")


if __name__ == "__main__":
    main()
