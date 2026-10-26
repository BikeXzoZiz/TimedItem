#!/usr/bin/env python3
"""Sync README.md กับแหล่งความจริงหลักของ repo (single sources of truth)

ทำ 3 อย่าง:
  1. เวอร์ชัน — อ่าน <version> จาก pom.xml แล้วแทนที่ "TimedItem-x.y.z.jar"
     ทุกจุดใน README.md (เช่น คำสั่งดาวน์โหลดใน Quick start)
  2. ตัวอย่าง config — แทนที่ block ระหว่าง marker
     <!-- BEGIN:sync-readme:config --> / <!-- END:sync-readme:config -->
     ด้วยเนื้อหา src/main/resources/config.yml จริงทั้งไฟล์
  3. ครอบคลุม Migration — เทียบเวอร์ชันใน CHANGELOG.md กับแถวในตาราง
     Migration ของ README ถ้าขาดเวอร์ชันใหม่จะเตือน (แถวสรุปต้องเขียนมือ
     script ไม่ gen ข้อความให้)

การใช้:
  python scripts/sync-readme.py --apply   # แก้ README ให้เป็นล่าสุด
  python scripts/sync-readme.py --check   # ตรวจเฉย ๆ exit 1 ถ้าไม่ sync (ใช้ก่อน release)

รันจากที่ไหนก็ได้ — อ้าง path จากตำแหน่ง script เอง ไม่ใช่ working directory
"""

import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
README = ROOT / "README.md"
POM = ROOT / "pom.xml"
CONFIG = ROOT / "src" / "main" / "resources" / "config.yml"
CHANGELOG = ROOT / "CHANGELOG.md"

BEGIN = "<!-- BEGIN:sync-readme:config -->"
END = "<!-- END:sync-readme:config -->"

# เวอร์ชันแบบ 3 ช่อง (3.1.2) — จุดเดียวที่ต้องแก้ถ้าเปลี่ยนรูปแบบเลขเวอร์ชัน
VERSION_RE = re.compile(r"\d+\.\d+\.\d+")
JAR_REF_RE = re.compile(r"TimedItem-\d+\.\d+\.\d+\.jar")
POM_VERSION_RE = re.compile(r"<version>(\d+\.\d+\.\d+)</version>")
CHANGELOG_HEADING_RE = re.compile(r"^## \[(\d+\.\d+\.\d+)\]", re.MULTILINE)
MIGRATION_ROW_RE = re.compile(r"^\| \*\*v(\d+\.\d+\.\d+)\*\* \|", re.MULTILINE)


def read(path: Path) -> str:
    return path.read_text(encoding="utf-8")


def new_pom_version() -> str:
    m = POM_VERSION_RE.search(read(POM))
    if not m:
        sys.exit(f"ERROR: หา <version>x.y.z</version> ใน {POM.name} ไม่เจอ")
    return m.group(1)


def render_config_block() -> str:
    config = read(CONFIG).rstrip("\n")
    return f"{BEGIN}\n```yaml\n{config}\n```\n{END}"


def sync_version(readme: str, version: str) -> tuple[str, bool]:
    """คืน (เนื้อหาใหม่, มีการเปลี่ยนไหม)"""
    updated = JAR_REF_RE.sub(f"TimedItem-{version}.jar", readme)
    return updated, updated != readme


def sync_config(readme: str) -> tuple[str, bool]:
    if BEGIN not in readme or END not in readme:
        sys.exit(
            "ERROR: ไม่เจอ marker sync ใน README.md\n"
            f"ต้องมี {BEGIN} และ {END} ครอบ block ตัวอย่าง config.yml"
        )
    start = readme.index(BEGIN)
    end = readme.index(END) + len(END)
    block = render_config_block()
    updated = readme[:start] + block + readme[end:]
    return updated, updated != readme


def check_migration(readme: str) -> list[str]:
    """คืนรายการเวอร์ชันที่มีใน CHANGELOG แต่ไม่มีแถวในตาราง Migration"""
    changelog_versions = set(CHANGELOG_HEADING_RE.findall(read(CHANGELOG)))
    readme_versions = set(MIGRATION_ROW_RE.findall(readme))
    missing = sorted(
        changelog_versions - readme_versions,
        key=lambda v: [int(p) for p in v.split(".")],
        reverse=True,
    )
    return [f"v{v}" for v in missing]


def main() -> None:
    mode = sys.argv[1] if len(sys.argv) > 1 else ""
    if mode not in ("--apply", "--check"):
        print(__doc__)
        sys.exit(2)

    version = new_pom_version()
    readme = read(README)

    # คำนวณผลลัพธ์ทั้งหมดก่อนแตะไฟล์
    stale = []
    version_updated, version_changed = sync_version(readme, version)
    if version_changed:
        stale.append(f"ชื่อ jar ใน README ไม่ตรงกับ pom.xml ({version})")
    config_updated, config_changed = sync_config(version_updated)
    if config_changed:
        stale.append("block ตัวอย่าง config.yml ไม่ตรงกับ src/main/resources/config.yml")
    missing = check_migration(config_updated)
    if missing:
        stale.append("ตาราง Migration ขาดแถว: " + ", ".join(missing))

    if not stale:
        print(f"OK — README ตรงกับ pom.xml ({version}) และ config.yml ครบทุกเวอร์ชันแล้ว")
        return

    if mode == "--check":
        print(f"OUT OF SYNC — พบ {len(stale)} จุดที่ README ล้าสมัย:")
        for item in stale:
            print(f"  - {item}")
        print("รัน: python scripts/sync-readme.py --apply")
        sys.exit(1)

    README.write_text(config_updated, encoding="utf-8", newline="\n")
    print(f"อัพเดท README.md แล้ว (เวอร์ชัน {version}):")
    for item in stale:
        print(f"  - {item}")
    if missing:
        print(
            "เตือน: แถว Migration ขาดเวอร์ชันด้านบน — เขียนสรุปเพิ่มในตาราง "
            "## ⬆️ Migration ด้วยมือ (script ไม่ gen ข้อความให้)"
        )


if __name__ == "__main__":
    main()
