from __future__ import annotations

import base64
import copy
import gzip
import re
import xml.etree.ElementTree as ET
from pathlib import Path

from gd_proto import as_int, first_plist_dict, parse_gmd


def decode_text(value: str) -> str:
    s = str(value).strip()
    s += "=" * (-len(s) % 4)
    try:
        raw = base64.urlsafe_b64decode(s.encode("ascii"))
        return gzip.decompress(raw).decode("utf-8", errors="replace")
    except Exception as exc:  # noqa: BLE001 - this becomes a workflow error
        raise ValueError("cloud save local-level data is not valid gzip/base64 text") from exc


def encode_text(value: str) -> str:
    return base64.urlsafe_b64encode(gzip.compress(value.encode("utf-8"))).decode("ascii").rstrip("=")


def children(elem: ET.Element):
    return [x for x in list(elem) if isinstance(x.tag, str)]


def first_dict(root: ET.Element) -> ET.Element:
    return first_plist_dict(root)


def value_after_key(d: ET.Element, key: str):
    c = children(d)
    for i, item in enumerate(c[:-1]):
        if item.tag in ("k", "key") and (item.text or "") == key:
            return c[i + 1]
    return None


def put_value(d: ET.Element, key: str, tag: str, text: str | None = None):
    val = value_after_key(d, key)
    if val is None:
        ET.SubElement(d, "k").text = key
        val = ET.SubElement(d, tag)
    else:
        val.tag = tag
    val.text = text
    return val


def level_root(d: ET.Element) -> ET.Element:
    val = value_after_key(d, "LLM_01")
    if val is not None:
        return val
    ET.SubElement(d, "k").text = "LLM_01"
    val = ET.SubElement(d, "d")
    ET.SubElement(val, "k").text = "_isArr"
    ET.SubElement(val, "t")
    return val


def next_name(levels: ET.Element):
    high = -1
    for item in children(levels):
        if item.tag in ("k", "key"):
            match = re.fullmatch(r"k_(\d+)", item.text or "")
            if match:
                high = max(high, int(match.group(1)))
    index = high + 1
    return f"k_{index}", index


def merge_level(local_xml: str, gmd_path: str | Path, rename: str = ""):
    try:
        local = ET.fromstring(local_xml)
    except ET.ParseError as exc:
        raise ValueError("cloud save local-level XML could not be parsed") from exc

    levels = level_root(first_dict(local))
    item_key, item_index = next_name(levels)

    try:
        gmd_root = ET.parse(str(gmd_path)).getroot()
    except ET.ParseError as exc:
        raise ValueError("the .gmd file could not be parsed as XML/plist") from exc

    level = copy.deepcopy(first_dict(gmd_root))
    gmd_data = parse_gmd(gmd_path)
    title = (rename.strip() or str(gmd_data.get("k2") or "Imported GMD"))[:40]

    put_value(level, "k1", "i", str(item_index + 1))
    put_value(level, "k2", "s", title)
    put_value(level, "k13", "t")
    put_value(level, "k21", "i", "2")
    put_value(level, "k47", "t")
    put_value(level, "k50", "i", str(as_int(gmd_data.get("k50"), 47)))

    ET.SubElement(levels, "k").text = item_key
    levels.append(level)
    out = ET.tostring(local, encoding="unicode", short_empty_elements=True)
    if not out.startswith("<?xml"):
        out = '<?xml version="1.0"?>' + out
    return out, item_key, title
