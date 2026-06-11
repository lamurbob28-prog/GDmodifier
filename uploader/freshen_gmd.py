from __future__ import annotations

import base64
import gzip
import re
import sys
import time
from html import escape
from pathlib import Path


def b64decode_urlsafe(text: str) -> bytes:
    text = text.strip()
    text += "=" * ((4 - len(text) % 4) % 4)
    return base64.urlsafe_b64decode(text.encode("ascii"))


def b64encode_urlsafe(data: bytes) -> str:
    return base64.urlsafe_b64encode(data).decode("ascii").rstrip("=")


def replace_plist_string(xml: str, key: str, value: str) -> str:
    pattern = rf"(<k>{re.escape(key)}</k><s>)(.*?)(</s>)"
    replacement = rf"\1{escape(value)}\3"
    new_xml, count = re.subn(pattern, replacement, xml, count=1, flags=re.DOTALL)
    if count != 1:
        raise SystemExit(f"Could not find plist string key {key!r}.")
    return new_xml


def get_plist_string(xml: str, key: str) -> str:
    match = re.search(rf"<k>{re.escape(key)}</k><s>(.*?)</s>", xml, flags=re.DOTALL)
    if not match:
        raise SystemExit(f"Could not find plist string key {key!r}.")
    return match.group(1)


def main() -> None:
    if len(sys.argv) < 3:
        raise SystemExit("Usage: python uploader/freshen_gmd.py INPUT.gmd OUTPUT.gmd [LEVEL_NAME]")

    src = Path(sys.argv[1]).expanduser()
    dst = Path(sys.argv[2]).expanduser()
    new_name = sys.argv[3] if len(sys.argv) >= 4 else None

    xml = src.read_text(encoding="utf-8", errors="replace")
    old_k4 = get_plist_string(xml, "k4")

    raw = b64decode_urlsafe(old_k4)
    level_text = gzip.decompress(raw)

    # Re-gzip the exact same level data with a fresh timestamp. Gameplay stays the
    # same, but the uploaded levelString is no longer byte-for-byte identical.
    fresh_raw = gzip.compress(level_text, compresslevel=9, mtime=int(time.time()))
    fresh_k4 = b64encode_urlsafe(fresh_raw)

    xml = replace_plist_string(xml, "k4", fresh_k4)
    if new_name:
        xml = replace_plist_string(xml, "k2", new_name[:40])

    dst.parent.mkdir(parents=True, exist_ok=True)
    dst.write_text(xml, encoding="utf-8")

    print("Created:", dst)
    print("Output level name:", new_name or get_plist_string(xml, "k2"))
    print("Old k4 length:", len(old_k4))
    print("New k4 length:", len(fresh_k4))


if __name__ == "__main__":
    main()
