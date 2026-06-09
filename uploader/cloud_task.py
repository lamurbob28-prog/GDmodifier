from __future__ import annotations

import json
import os
import sys
from pathlib import Path

from account_http import store_save, sync_save
from level_merge import decode_text, encode_text, merge_level


def env(name: str, default: str = "") -> str:
    return os.environ.get(name, default).strip()


def summary(text: str) -> None:
    path = os.environ.get("GITHUB_STEP_SUMMARY")
    if path:
        with open(path, "a", encoding="utf-8") as f:
            f.write(text)


def dump(attempts) -> None:
    Path("gdmodifier_cloud_debug.json").write_text(
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


def die(msg: str, attempts=None) -> None:
    print("ERROR:", msg)
    summary("## Cloud import failed\n\n" + msg + "\n")
    if attempts:
        dump(attempts)
    sys.exit(1)


def show(label: str, attempts) -> None:
    print("\n---", label, "---")
    for a in attempts:
        print(
            f"{a.endpoint} status={a.status} elapsed={a.elapsed_ms}ms "
            f"ua={a.user_agent!r} blocked={a.blocked_looking} preview={a.preview}"
        )


def main() -> None:
    gmd_path = env("GMD_PATH")
    account_id = env("GD_ACCOUNT_ID")
    gd_secret = os.environ.get("GD_PASSWORD", "")
    rename = env("LEVEL_NAME_OVERRIDE")

    if not gmd_path:
        die("Missing gmd_path.")
    if not account_id:
        die("Missing gd_account_id.")
    if not gd_secret:
        die("Missing GD_PASSWORD repository secret.")
    if not Path(gmd_path).exists():
        die("Could not find .gmd file: " + gmd_path)
    if not Path(gmd_path).is_file():
        die("gmd_path is not a file: " + gmd_path)

    attempts_all = []
    print("Syncing account save...")
    parts, attempts = sync_save(account_id, gd_secret)
    attempts_all += attempts
    show("sync", attempts)
    if len(parts) < 2:
        die(
            "Could not sync cloud save. Log into the burner account in Geometry Dash, "
            "press Save once, then rerun.",
            attempts_all,
        )

    try:
        local_xml = decode_text(parts[1])
        patched_xml, slot, title = merge_level(local_xml, gmd_path, rename)
        parts[1] = encode_text(patched_xml)
    except Exception as exc:  # noqa: BLE001 - user-facing workflow error
        die("Could not patch local levels: " + repr(exc), attempts_all)

    print("Saving patched data...")
    ok, attempts = store_save(account_id, gd_secret, parts)
    attempts_all += attempts
    show("store", attempts)
    if not ok:
        die("Patched data was built, but the account server did not accept it.", attempts_all)

    print("SUCCESS")
    print("Level:", title)
    print("Slot:", slot)
    summary(
        "## SUCCESS\n\nInjected `"
        + title
        + "` into the burner account cloud save.\n\n"
        "Open Geometry Dash on that burner account and press **Load**. Then check **Create**.\n"
    )


if __name__ == "__main__":
    main()
