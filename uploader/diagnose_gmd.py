from __future__ import annotations

import os
import sys
from pathlib import Path

from gd_proto import build_upload_payloads, count_coins, parse_gmd


def env(name: str, default: str = "") -> str:
    return os.environ.get(name, default).strip()


def summary(text: str) -> None:
    path = os.environ.get("GITHUB_STEP_SUMMARY")
    if path:
        with open(path, "a", encoding="utf-8") as f:
            f.write(text)


def add_result(lines: list[str], label: str, value: object) -> None:
    lines.append(f"| {label} | `{value}` |")


def fail(lines: list[str], message: str) -> None:
    print("DIAGNOSIS FAILED:", message)
    lines.append("")
    lines.append("## Result")
    lines.append("")
    lines.append(f"FAILED: {message}")
    summary("\n".join(lines) + "\n")
    sys.exit(1)


def main() -> None:
    gmd_path = env("GMD_PATH", "uploads/level.gmd")
    username = env("GD_USERNAME", "diagnostic_user") or "diagnostic_user"
    account_id = env("GD_ACCOUNT_ID", "1") or "1"
    mode = env("UPLOAD_MODE", "modern-first") or "modern-first"
    visibility = env("VISIBILITY", "public") or "public"

    lines = ["# GDmodifier diagnosis", "", "| Check | Result |", "|---|---|"]
    path = Path(gmd_path)

    add_result(lines, "gmd_path", gmd_path)
    add_result(lines, "exists", path.exists())
    if not path.exists():
        fail(lines, "The selected .gmd file does not exist.")

    add_result(lines, "is_file", path.is_file())
    if not path.is_file():
        fail(lines, "The selected path exists but is not a file.")

    size = path.stat().st_size
    add_result(lines, "file_size_bytes", size)
    if size <= 0:
        fail(lines, "The selected .gmd file is empty.")

    try:
        data = parse_gmd(path)
    except Exception as exc:  # noqa: BLE001 - workflow diagnostic output
        fail(lines, f"Could not parse .gmd: {exc}")

    level_string = str(data.get("k4") or "")
    add_result(lines, "parse", "ok")
    add_result(lines, "level_name", str(data.get("k2") or ""))
    add_result(lines, "level_string_length", len(level_string))
    add_result(lines, "objects", data.get("k48", 0))
    add_result(lines, "coins", count_coins(data))
    add_result(lines, "song_id", data.get("k45", 0))
    add_result(lines, "audio_track", data.get("k8", 0))

    if len(level_string) < 20:
        fail(lines, "The levelString field is too short to be a normal exported level.")

    try:
        payloads = build_upload_payloads(
            data=data,
            username=username,
            account_id=account_id,
            password="diagnostic-only-placeholder",
            mode=mode,
            visibility=visibility,
        )
    except Exception as exc:  # noqa: BLE001 - workflow diagnostic output
        fail(lines, f"Could not build dry-run payload: {exc}")

    add_result(lines, "dry_run_payload_variants", len(payloads))
    for name, payload in payloads:
        add_result(lines, f"payload_{name}_field_count", len(payload))
        add_result(lines, f"payload_{name}_has_seed2", "seed2" in payload)
        add_result(lines, f"payload_{name}_has_levelString", "levelString" in payload)

    lines.append("")
    lines.append("## Result")
    lines.append("")
    lines.append("PASS: The file parses and the dry-run payload can be built. If the live upload still fails, the failure is server/auth/network side, not local .gmd parsing.")
    output = "\n".join(lines) + "\n"
    print(output)
    summary(output)


if __name__ == "__main__":
    main()
