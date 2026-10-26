# TimedItem — ระบบทดสอบ (Test Infrastructure)

เครื่องมือทดสอบ TimedItem แบ่งเป็น 3 ชั้น ทั้งหมดรันบนเครื่อง local (ไม่มี CI):

## ภาพรวมชั้นการทดสอบ

| ชั้น | อยู่ที่ | วิธีรัน | ทดสอบอะไร |
|---|---|---|---|
| 1. Unit tests | `src/test/java` | `mvn test` | ตรรกะ pure (parse, storage, token, reflection helpers) — ไม่ต้องมี server |
| 2. In-game harness | `testing/harness/` | ผ่านตารางที่ 3 | พฤติกรรมจริงบน server จริง (expiry, transform, hologram, hooks) |
| 3. Test matrix | `testing/run-matrix.sh` | `bash testing/run-matrix.sh <lane>` | บูต server จริง 4 แพลตฟอร์ม + รัน harness + สรุปผล |

## 1. Unit tests

```bash
export JAVA_HOME="/c/Users/teerachot.a.OBCJLL0067/AppData/Local/Programs/Eclipse Adoptium/jdk-21.0.12.101-hotspot"
C:/Users/Teerachot.a/Documents/.tools/maven/apache-maven-3.9.9/bin/mvn test
```

ครอบคลุม: DurationParser, TimeLore.analyzeLore, HookManager.parseToken,
HookReflection (หัวใจของ hook layer), VisualStages.parse, TrackedItems,
SchedulerCompat (ตรวจว่า JVM ปกติต้องไม่ใช่ Folia), DiscordWebhook,
DebugLog/DebugReport, RecoveryFormat, UpdateChecker, Warnings,
LanguageFiles, storage (FlatFile + Sqlite)

## 2. ปลั๊กอินทดสอบในเกม (TimedItemTestHarness)

ปลั๊กอินเล็ก ๆ ที่ `depend: [TimedItem]` ให้คำสั่ง `/titest run <scenario|all>`
(จาก console) แล้วพิมพ์ผลลง log ในรูปแบบที่ grep ได้:

```
[TITEST] PASS ground_expiry - expired ground item removed, ItemExpireEvent(player=null) seen
[TITEST] FAIL <name> - <เหตุผล>
[TITEST] SKIP <name> - <เหตุผล>   (เช่น scenario สำหรับ Folia บน server ปกติ)
[TITEST] SUITE COMPLETE passed=x failed=y skipped=z total=n
```

### Scenarios

| Scenario | ทดสอบ | เงื่อนไข |
|---|---|---|
| `boot` | TimedItem enable, config/hook/expiry service พร้อม, ค่า flags ทุกอย่างถูก lane config | ทุกแพลตฟอร์ม |
| `lore_roundtrip` | set expiry → isTimed/type/remaining/PDC uid → pause/resume → lifetime → clear | ทุกแพลตฟอร์ม |
| `api_facade` | blacklist (BEDROCK) ต้อง throw, วินาที ≤ 0 ต้อง throw, ของเปล่าต้องอ่านเป็น null | ทุกแพลตฟอร์ม |
| `ground_expiry` | ทิ้งของหมดอายุ → pipeline เก็บ + ItemExpireEvent(player=null) | ทุกแพลตฟอร์ม (Folia ใช้ FoliaGroundExpiry) |
| `transform` | expiry_transform profile "ash" → ของกลายเป็น GUNPOWDER บนพื้น + event แนบ replacement | ทุกแพลตฟอร์ม |
| `container_scan` | chest ใส่ของหมดอายุ → periodic scan กินหาย | classic เท่านั้น (Folia ปิดตามดีไซน์) |
| `holograms` | TextDisplay เกิดเหนือของที่ตก + หายเมื่อ timer หมด | ไม่ใช่ Folia + 1.19.4+ |
| `folia_gates` | Folia: ground expiry ทำงาน + hologram ต้องไม่เกิด | Folia เท่านั้น |
| `hooks_stub` | ทุก adapter ที่เปิดใช้: build(token) → itemId() roundtrip + describe(), CrazyCrates build-only | ต้องมี FakeHooks |
| `auto_timer` | fake Shopkeepers/CrazyCrates event → ประทับเวลาตาม config, ของที่มี timer อยู่แล้ว/blacklist ต้องไม่โดนแตะ | ต้องมี FakeHooks + auto_timer ใน config |

ข้อจำกัดตรงไปตรงมา: flow ที่ต้องมี client จริง (คลิก GUI AdminMenu,
actionbar บนหน้าจอ, ContainerGuard ตอนคลิก, VisualStages ใน inventory
ผู้เล่น, grace period ตอนใช้ของ, renewal token ตอนคลิกขวา) ไม่ automate
ได้จาก console — ทดสอบด้วยมือตาม checklist ใน CHANGELOG ของแต่ละรุ่น

## 3. Test matrix

```bash
bash testing/run-matrix.sh paper      # Paper 1.21.x ล่าสุด
bash testing/run-matrix.sh folia      # Folia ล่าสุด
bash testing/run-matrix.sh purpur     # Purpur ล่าสุด
bash testing/run-matrix.sh paper120   # Paper 1.20.x (legacy)
bash testing/run-matrix.sh all        # ทั้ง 4 lanes (~25 นาที)
bash testing/run-matrix.sh hooks-lane # paper + jar จริงจาก testing/real-hooks/
```

- ดาวน์โหลด server jar ครั้งแรกครั้งเดียว (cache ที่ `testing/servers/jars/`)
  จาก PaperMC Fill API และ Purpur API
- แต่ละ lane ได้ server dir สะอาดที่ `testing/servers/runs/<lane>/`
  (eula, flat world, spawn-chunk-radius=16, พอร์ตไม่ซ้ำ, `timeditem reload`
  ก่อนรัน suite) — console.log เก็บไว้ตรวจย้อนหลัง
- ผลสรุปอยู่ที่ `testing/servers/runs/summary.txt`, exit code ไม่ใช่ 0 เมื่อมี FAIL
- ปรับเวลาด้วย env: `BOOT_WAIT`, `RUN_WAIT`, `KILL_AFTER`

### ความต่างของ lane

- **Folia**: `container_scan` + `holograms` จะ SKIP ตามดีไซน์ และ `folia_gates`
  จะรันแทน — ถ้า Folia รัน container_scan หรือ hologram ได้ = บั๊ก
- **Paper 1.20.x**: ยืนยันว่า jar เดียว (Java 8 bytecode) ยังบูตและทำงานครบ
- **hooks-lane**: วาง jar ปลั๊กอินจริง (ตัวฟรี: Slimefun, Oraxen,
  ExecutableItems, Shopkeepers, CrazyCrates, PlaceholderAPI) ไว้ใน
  `testing/real-hooks/` แล้วรัน — runner จะตัด FakeHooks ที่ชื่อชนกับ jar จริงออกเอง
  ตัวเสียเงิน (MMOItems, ItemsAdder, Nexo) ใช้ stub แทนเสมอ

### สิ่งที่ matrix ค้นพบ (2026-10-27, ปลั๊กอิน 3.1.3)

- **Folia + expiry_transform บนของบนพื้น**: `ItemExpireEvent` แนบ replacement
  ถูกต้อง (GUNPOWDER) แต่ของที่ `dropItemNaturally` ทิ้ง **ไม่ปรากฏในโลก**
  (มองหาไม่เจอใน chunk เจ้าของ region ตลอด 40 วินาที) — บน Paper/Purpur ทำงานครบ
  → scenario `transform` จะ FAIL บน Folia พร้อมข้อความชี้เฉพาะ — ควรตรวจ
  deliverReplacement ฝั่ง Folia (spawn จาก region thread หลัง removal.run())
  ก่อนปิดจุดนี้ใน matrix

## FakeHooks stubs (`testing/fakehooks/`)

ปลั๊กอินจำลอง 10 ตัว ชื่อ plugin.yml ตรงกับของจริง (MythicLib, MMOItems,
ItemsAdder, Oraxen, Nexo, ExecutableItems, Slimefun, EcoItems, Shopkeepers,
CrazyCrates) — จำลองเฉพาะ class/method ที่ reflection ของ TimedItem ตามหา
ของแต่ละตัวติด marker ที่ display name แล้ว identify ด้วยชื่อ

```bash
cd testing/fakehooks && bash build.sh   # ต้อง build โปรเจกต์หลักก่อน 1 ครั้ง (ต้องมี spigot-api ใน ~/.m2)
```

ข้อควรรู้เรื่อง fake env:
- manifest ใส่ `paperweight-mappings-namespace: mojang` เพื่อให้ Paper
  **ไม่** remap jar (ไม่งั้น remapper จะเขียนชื่อ class ใหม่ตาม mapping ของ
  plugin จริง เช่น Slimefun main เคยถูกย้ายตำแหน่งจนโหลดไม่ได้)
- FakeShopkeepers/FakeCrazyCrates รับคำสั่ง `fakesh fire` / `fakecc fire`
  แล้วยิง event ของจริง (ตามชื่อ class) — TimedItem hook เข้า event พวกนั้นได้เต็ม ๆ
- ทดสอบแล้ว adapter ของ **MMOItems ไม่ activate บน Paper 1.21 ใน fake env**:
  `Class.forName("io.lumine...")` ตอน TimedItem onEnable ล้มเหลวเสมอ
  (package `io.lumine` ต่างจาก package อื่น ๆ ทุกตัวที่ผ่าน) แต่ค้นหาเจอ
  เมื่อเรียกหลังบูตเสร็จ — harness จะรายงานเป็น "inactive in this environment"
  ไม่นับเป็น FAIL ถ้านำไปรันกับ jar จริงใน hooks-lane แล้ว mmoitems (off)
  ให้รายงานเป็น bug ของปลั๊กอินทันที

## เพิ่ม scenario ใหม่

1. สร้าง class ใน `testing/harness/src/.../harness/` implement `Scenario`
   (`name()`, `skipReason()`, `start(ctx)`, `poll(ctx)` เมื่อต้องรอ)
2. เพิ่มใน `Suite.all()`
3. ถ้าต้องตั้งค่า config เพิ่ม — แก้ `lane_config()` ใน `testing/run-matrix.sh`
4. rebuild: `mvn -f testing/harness/pom.xml package` (ต้อง `mvn install`
   โปรเจกต์หลักก่อนเมื่อ API ฝั่ง TimedItem เปลี่ยน)

## ไฟล์ที่ไม่ commit

`testing/servers/`, `testing/fakehooks/out|build/`, `testing/real-hooks/`,
`testing/harness/target/` — ดู `.gitignore`
