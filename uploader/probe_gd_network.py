from __future__ import annotations

import os
import sys

from gd_proto import lookup_account_id


def env(name: str, default: str = "") -> str:
    return os.environ.get(name, default).strip()


def summary(text: str) -> None:
    path = os.environ.get("GITHUB_STEP_SUMMARY")
    if path:
        with open(path, "a", encoding="utf-8") as f:
            f.write(text)


def classify_response(text: str) -> str:
    value = str(text or "").strip()
    lowered = value[:1600].lower()
    if not value:
        return "empty"
    if value.startswith("-"):
        return "negative"
    if "<!doctype html" in lowered or "cloudflare" in lowered or "access denied" in lowered:
        return "blocked_html"
    if ":" in value:
        return "colon_data"
    return "other"


def main() -> None:
    username = env("GD_USERNAME")
    if not username:
        print("Missing GD_USERNAME")
        sys.exit(1)

    lines = ["# GD network probe", "", "| Check | Result |", "|---|---|"]
    lines.append(f"| username | `{username}` |")

    account_id, attempts = lookup_account_id(username)
    lines.append(f"| attempts | `{len(attempts)}` |")
    lines.append(f"| account_id_found | `{bool(account_id)}` |")
    lines.append(f"| account_id | `{account_id or ''}` |")

    for index, attempt in enumerate(attempts, start=1):
        lines.append(f"| attempt_{index}_status | `{attempt.status}` |")
        lines.append(f"| attempt_{index}_ua | `{attempt.user_agent}` |")
        lines.append(f"| attempt_{index}_class | `{classify_response(attempt.text)}` |")
        lines.append(f"| attempt_{index}_preview | `{attempt.preview}` |")

    lines.append("")
    lines.append("## Result")
    lines.append("")
    if account_id:
        lines.append("PASS: GitHub Actions can reach the GD user lookup endpoint and get an accountID.")
    else:
        lines.append("FAIL: GitHub Actions could not resolve that username through the GD user lookup endpoint.")

    output = "\n".join(lines) + "\n"
    print(output)
    summary(output)
    if not account_id:
        sys.exit(1)


if __name__ == "__main__":
    main()
