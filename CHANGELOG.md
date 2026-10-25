# Changelog

รูปแบบอ้างอิง [Keep a Changelog](https://keepachangelog.com/) — รายละเอียดการใช้งานดูที่ [README.md](README.md)

## [2.4.0] - 2026-10-26

### Added
- **Folia support** — ตรวจจับ Folia อัตโนมัติ และรันงานผ่าน `SchedulerCompat` (global region scheduler / entity scheduler ของแต่ละผู้เล่น / async scheduler): expire จากคลิก/ใช้/ดาเมจ/หยิบ, periodic scan และ actionbar ทำงานบน region thread ของเจ้าของถูกต้อง, `expiry_commands` dispatch บน global region, recovery store ปลอดภัยกับหลาย region thread — ข้อจำกัด: `scan_world_items` (หมดอายุบนพื้น/เฟรม/stand) ยังไม่รองรับบน Folia และจะถูกข้ามไป
- **Discord webhook ในตัว** — `Settings.discord_webhook.*` (`enabled`/`url`/`username`/`message` มี `{player}`/`{item}`/`{world}`) โพสต์แจ้งทุกครั้งที่ไอเทมหมดอายุ บน background thread โดยไม่ต้องติดตั้ง DiscordSRV (failure เงียบ + warn จำกัดความถี่)
- **Multi-language** — `Settings.language` เลือกไฟล์ `lang/messages_en.yml` / `lang/messages_th.yml` (หรือวางไฟล์ภาษาอื่นเอง); ข้อความ `Messages.*`/`DateFormat.*` ย้ายออกจาก config.yml — ค่าที่เคยแก้ไว้จะถูก migrate เข้าไฟล์ภาษาอัตโนมัติครั้งแรก; key ที่หาย self-heal จาก default อังกฤษ
- **GUI ตามภาษา** — ป้ายชื่อทั้งหมดใน `/ti menu` และ `/ti restore` (ชื่อหน้าต่าง, ปุ่ม, status book, ข้อมูล entry) อยู่ใน section `Gui` ของไฟล์ภาษา

### Changed
- `recovery.log` เขียนผ่าน background thread เดียว (snapshot ก่อน → เขียน temp file → atomic move) ไม่บล็อก main/region thread อีกต่อไป; ปิด plugin จะ flush งานค้างก่อน (`RecoveryStore.close()`)
- `PdcCompat` cache Method ทั้งหมดตอน init (เดิม lookup ใหม่ทุก operation บน hot path: ทุกคลิก/สแกน/actionbar tick)
- bStats ห่อ try/catch กัน crash บนเซิร์ฟเวอร์ที่ metrics ใช้ไม่ได้

### Fixed
- แผนที่เตือน (`warned`) เป็น `ConcurrentHashMap` — จำเป็นเมื่อหลาย region thread แตะพร้อมกันบน Folia

## [2.3.1] - 2026-10-26

### Fixed
- รวมโค้ดหมดอายุ 2 ชุด (ผู้เล่น vs ไอเทมบนพื้น/เฟรม/armor stand) เป็น pipeline เดียว `ExpiryService` — เสียง/particle/recovery/`expiry_commands` ทำงานสม่ำเสมอเหมือนกันทั้งสองกรณี และลบ branch ตายที่จะข้าม `expiry_commands` โดยไม่บอก
- `recovery.log` ข้ามเวอร์ชัน Minecraft แล้วอ่านไม่ได้ → แต่ละบรรทัดเพิ่ม format tag (`nbt`/`bos`) และตอนโหลดจะ re-encode เป็นฟอร์แมตของเซิร์ฟเวอร์ปัจจุบันเอง (บรรทัดเก่า 4 ช่องยังอ่านได้) ของที่อ่านไม่ได้จริงจะถูกข้ามพร้อมแจ้งจำนวนใน console แทนการหายเงียบ
- bStats ยังใช้ placeholder id `17777` (ถูกนับรวมเป็นของปลั๊กอินอื่น) → ลงทะเบียนจริงแล้วเป็น [TimedItemRewrite (ID 34946)](https://bstats.org/plugin/bukkit/TimedItemRewrite/34946) และลบระบบเตือน placeholder ที่ไม่จำเป็นออก

### Added
- `Settings.expire_check.ignore_creative` (default `true` = พฤติกรรมเดิม) — เปิด/ปิดการข้ามผู้เล่น creative ซึ่งก่อนหน้า hardcode ไว้และไม่ได้เขียนไว้ในเอกสาร
- Tab completion เพิ่ม time hint `1d12h`, `30d`
- ไฟล์ `CHANGELOG.md` นี้

## [2.3.0]

### Added
- Tab completion ครบทุกคำสั่ง (subcommands, time hints, ผู้เล่นออนไลน์, เลขหน้า)
- `/ti give <ผู้เล่น> <เวลา> [จำนวน]` — ส่งไอเทม timed ให้ผู้เล่นทันที
- ระบบกู้คืนไอเทม: recovery.log + `/ti restore [หน้า]` (GUI แบบแบ่งหน้า)
- เตือนล่วงหน้าหลายระยะ `Settings.warning.stages`
- Material blacklist `Settings.blacklist.materials`
- Update checker จาก GitHub Releases (แจ้ง console + แอดมินตอน join)

### Fixed
- จุดบอด hotbar-swap (สแกน inventory ซ้ำ next tick) และ lore line ของ timer เสียบังบรรทัดอื่น

## [2.2.0]

### Added
- เก็บ timer ใน PersistentDataContainer (NBT) บน 1.14+ — lore เป็น display + fallback
- Developer API: `TimedItemAPI`, `ItemExpireEvent` (cancellable), `ItemTimerStartEvent`
- PlaceholderAPI (`%timeditem_remaining|seconds|time|type|date%`)
- GUI menu `/ti menu` (preset เวลา / pause-resume / remove)
- หมดอายุบนพื้น/item frame/armor stand (`Settings.expire_check.scan_world_items`)
- Unit tests (JUnit 5) และ MIT License

## [2.1.0]

### Added
- bStats metrics (shaded + relocated), CI (GitHub Actions build/release)
- หยุด/เดินเวลาต่อ `/ti pause` / `/ti resume`
- เตือนล่วงหน้า + particle ตอนหมดอายุ + เสียง + actionbar countdown
- `Settings.expiry_commands` — console commands ตอนไอเทมหมดอายุ

## [2.0.0]

- Refactor ใหม่ทั้งหมดจากปลั๊กอินต้นฉบับปี 2019 รองรับ Minecraft 1.13 → ปัจจุบัน (jar เดียว, Java 8 bytecode, compat ผ่าน reflection shims)

## [1.x]

- ปลั๊กอินเวอร์ชันแรก — timer แบบ lore, หมดอายุเมื่อคลิก/ใช้ไอเทม
