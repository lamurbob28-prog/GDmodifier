from __future__ import annotations

import json
import os
import re
import sys
from pathlib import Path

from gd_proto import build_upload_payloads, lookup_account_id, parse_gmd, post_boomlings


def getenv(name: str, default: str = "") -> str:
    return os.environ.get(name, default).strip()


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


def main() -> None:
    gmd_path = getenv("GMD_PATH")
    username = getenv("GD_USERNAME")
    account_id = getenv("GD_ACCOUNT_ID")
    secret = os.environ.get("GD_PASSWORD", "")
    mode = getenv("UPLOAD_MODE", "modern-first")
    visibility = getenv("VISIBILITY", "public")

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

    all_attempts = []
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
        )
    except Exception as exc:  # noqa: BLE001 - user-facing workflow error
        fail(f"Could not build upload payload: {exc}", all_attempts)

    for name, payload in payloads:
        print("Uploading with variant:", name)
        attempts = post_boomlings("uploadGJLevel21.php", payload, stop_on_compact=True)
        all_attempts.extend(attempts)
        print_attempts("upload " + name, attempts)
        for a in attempts:
            response = a.text.strip()
            if re.fullmatch(r"[0-9]+", response) and int(response) > 0:
                print(f"::notice title=Geometry Dash Level ID::{response}")
                print("SUCCESS. Level ID:", response)
                summary(
                    f"## SUCCESS\n\n**Geometry Dash Level ID:** `{response}`\n\n"
                    "Open Geometry Dash → Search → enter that ID.\n"
                )
                return

    fail("Upload failed. Check attempt previews and debug artifact.", all_attempts)


if __name__ == "__main__":
    main()
