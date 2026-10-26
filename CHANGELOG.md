# Changelog

รูปแบบอ้างอิง [Keep a Changelog](https://keepachangelog.com/) — รายละเอียดการใช้งานดูที่ [README.md](README.md)

## [2.6.0] - 2026-10-26

### Added
- **Integration 9 ปลั๊กอินยอดนิยม** (ระบบ `Hooks.*` ใน config, ตรวจจับอัตโนมัติ, ปิดรายตัวได้, optional ทั้งหมด — ไม่มีติดตั้งก็รันเหมือนเดิม):
  - **Item plugins** — MMOItems · ItemsAdder · Oraxen · Nexo · ExecutableItems · Slimefun · EcoItems: จำตัวตน item ของปลั๊กอินนั้น (เช่น `mmoitems:SWORD:CUTLASS`, `itemsadder:namespace:id`)
  - **CrazyCrates** — สร้าง prize จาก crate (`cc:<crate>:<prize>`) เป็น timed item ได้
  - **Distribution auto-timer** — Shopkeepers (ของที่ซื้อจากร้าน) และ CrazyCrates (ของจากการเปิด crate) ใส่เวลาหมดอายุอัตโนมัติตาม `Hooks.<plugin>.auto_timer` (default ปิด; ไม่ซ้อน timer เดิม, เคารพ material blacklist)
- **`/ti give <player> <plugin:id> <time> [amount]`** — สร้างของ custom จาก plugin ที่ integrate เป็น timed item ทันที ไม่ต้องหยิบของในมือ → console ใช้ได้; syntax เดิม (ของในมือ) ทำงานเหมือนเดิม; tab completion เสนอ prefix ของ plugin ที่ hook อยู่
- **`%timeditem_itemid%`** (PlaceholderAPI) — คืน `plugin:id` ของ item custom หรือชื่อ material
- **Discord webhook `{item}`** — โชว์ display name ของ item (ตัดรหัสสี), หรือ `plugin:id` สำหรับ item custom ที่ไม่มี display name แทนชื่อ material ดิบ
- **`/ti debug export`** — รายงานบั๊กมีรายการ hooks ที่ active/off ทั้งหมด

### Technical
- Integration ทั้งหมดเป็น **reflection ล้วน** (`hook/HookReflection`) — ไม่เพิ่ม Maven dependency และคง Java 8 bytecode / jar เดียว 1.13–26.x; ทุก adapter probe ชื่อ class หลาย candidate รองรับเวอร์ชันเก่า/ใหม่ (Oraxen 2.x package move, CrazyCrates badbones69→ryderbelserion rewrite, MI 6.7 ก่อน/หลัง template API) — API ไม่ตรง = hook นั้นปิดตัวเองอย่างปลอดภัย
- Event hooks (Shopkeepers/CrazyCrates) ลงทะเบียนผ่าน `EventExecutor` + `HandlerList` โดยตรง ไม่ผูก event class ของปลั๊กอินอื่นตอน compile; ยกเลิก/ลงทะเบียนใหม่ได้ทุก `/ti reload`
- `plugin.yml` เพิ่ม softdepend: MythicLib, MMOItems, ItemsAdder, Oraxen, Nexo, Slimefun, ExecutableItems, SCore, EcoItems, Shopkeepers, CrazyCrates; `config-version` 8; ข้อความใหม่ `Messages.item_not_found` (en/th)

### ไม่รองรับ (โดยการออกแบบ)
- **MythicMobs** — item ของ MythicMobs เป็น vanilla stack ไม่มี tag ระบุตัวตน จึงไม่มีจุด hook ที่เชื่อถือได้ (ไม่ใช่ข้อจำกัดของ TimedItem)

## [2.5.0] - 2026-10-26

### Added
- **ระบบ Debug & Diagnostics** — `/ti debug [on|off|export]` สำหรับไล่บั๊ก: `on`/`off` เปิด-ปิดโหมด verbose ของ session (รีเซ็ตตาม config ตอน reload/restart), `export` เขียนรายงานบั๊กเป็นไฟล์ `plugins/TimedItem/debug/report-<timestamp>.txt` (สภาพแวดล้อม, settings, สถิติ recovery, log ล่าสุด)
- **จับ log ในหน่วยความจำ** — ทุก log ของปลั๊กอิน (warning, migration notice, scheduler failure) ถูกเก็บใน ring buffer 500 แถวอัตโนมัติ พร้อมดัมพ์ลงรายงาน โดยไม่ต้องเก็บไฟล์ log แยก
- **Verbose logging `[Debug]`** — จุดที่เคยหายเงียบมองเห็นได้แล้วเมื่อเปิด debug: PDC เขียน/อ่านไม่สำเร็จ (lore ยังเป็นที่เก็บสำรอง), serializer ตกไปใช้ legacy path, ทุกเหตุการณ์หมดอายุ, การบันทึก recovery (มี dedupe ข้อความซ้ำภายใน 5 วินาที กัน spam จาก hot path)
- **`Settings.debug`** (default `false`) — ค่า default ของโหมด debug ตอนเริ่มระบบ
- **Expiry check กัน crash** — periodic scan ที่โยน exception จะถูกจับ log พร้อม throwable เต็ม (debug on → มี stack trace ใน buffer) แล้วทำงานต่อรอบถัดไป

### Privacy
- รายงาน debug **ไม่มีทาง** มี webhook URL, ข้อมูล item/NBT หรือรายชื่อผู้เล่นจาก recovery — มี unit test คุมไว้ (`DebugReportTest`)

## [2.4.1] - 2026-10-26

### Added
- **Ground expiry บน Folia สมบูรณ์แล้ว** — `scan_world_items` บน Folia ใช้ per-entity tracking แทนการเดินสแกนทุก world: ไอเทมที่หล่น (ItemSpawnEvent + ChunkLoadEvent ตอน chunk โหลด) และของใน item frame / บน armor stand (ตรวจตอน chunk โหลด + ตอนผู้เล่นคลิก) ที่มี timer จะถูกตั้ง repeating check บน **entity scheduler ของมันเอง** — รันบน region thread ของเจ้าของถูกต้อง และ task ยกเลิกตัวเองเมื่อ entity หายไปหรือไม่มี timed item แล้ว

### Changed
- บน Folia การหมดอายุบนพื้น/เฟรม/stand จะไม่มีเสียง/particle ให้ผู้เล่นใกล้เคียง (อ่านตำแหน่งผู้เล่นข้าม region ไม่ปลอดภัย) — recovery log, event และ `expiry_commands` ทำงานปกติ

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
