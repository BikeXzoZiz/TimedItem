# TimedItem 2.0

Give items a lifetime — they expire (and disappear) when the timer runs out.

เวอร์ชัน 2.0 คือ refactor ใหม่ทั้งหมดจากปลั๊กอินต้นฉบับปี 2019 เพื่อรองรับ Minecraft ยุคปัจจุบัน

## เวอร์ชันที่รองรับ

| รองรับ | รายละเอียด |
|---|---|
| Minecraft **1.13 → 26.3+ (ล่าสุด)** | jar เดียว ใช้ได้ทุกเวอร์ชันในช่วงนี้ |
| Spigot / Paper / **Purpur** / Pufferfish / Leaf และ forks อื่น ๆ | ทุก fork ที่ใช้ Bukkit API |
| Java 8 – 25 (ฝั่งเซิร์ฟเวอร์) | compile เป็น Java 8 bytecode |
| Folia | ❌ ยังไม่รองรับ (threading ต่างโครงสร้าง) |

## Build

ต้องมี JDK 8 ขึ้นไป (แนะนำ 17/21) และ Maven:

```bash
mvn package
```

ได้ไฟล์ `target/TimedItem-2.0.0.jar` นำไปวางในโฟลเดอร์ `plugins/` ของเซิร์ฟเวอร์ได้เลย

## คำสั่ง (permission: `timeditem.admin`, default: op)

| คำสั่ง | คำอธิบาย |
|---|---|
| `/ti set <เวลา>` | ประทับเวลาบนไอเทมที่ถือ เช่น `/ti set 1d12h30m` หรือแบบเก่า `/ti set 120 s` |
| `/ti remove` | ถอด timer ออกจากไอเทมที่ถือ |
| `/ti info` | ดูเวลาที่เหลือของไอเทมที่ถือ |
| `/ti reload` | โหลด config ใหม่ |

หน่วยเวลา (ปรับได้ใน config): `s` วินาที, `m` นาที, `h` ชั่วโมง, `d` วัน — ใส่รวมหลายหน่วยได้ เช่น `2d6h`

## ไอเทมจะหายเมื่อไหร่?

ปลั๊กอินตรวจสอบไอเทมหมดอายุจาก:
- ตรวจอัตโนมัติเป็นรอบทุก 10 วินาที (ปรับ/ปิดได้ที่ `Settings.expire_check`)
- ผู้เล่นคลิกไอเทมใน inventory
- ผู้เล่นใช้/คลิกขวาไอเทม
- ผู้เล่นเข้าเซิร์ฟเวอร์ (สแกนทั้ง inventory)
- ผู้เล่นโดนดาเมจ (เกราะที่สวมอยู่)

เมื่อหมดอายุ: ไอเทมถูกลบ เล่นเสียง (ปรับได้) และส่งข้อความ หากเป็นเกราะที่เพิ่ม max health
และเปิด `reset_player_health_if_armor` จะรีเซ็ต max health กลับเป็น 20 ด้วย

## การตั้งค่า (`config.yml`)

- `Settings.start_date_calculate_on_click` — `true` = เริ่มนับเวลาเมื่อคลิก/ใช้ไอเทมครั้งแรก, `false` = เริ่มนับทันทีตอน `/ti set`
- `Settings.expire_check.enabled` / `interval_seconds` — การตรวจหมดอายุอัตโนมัติ
- `Settings.sound.enabled` / `name` / `volume` / `pitch` — เสียงตอนไอเทมหมดอายุ (ใช้ sound key เช่น `entity.blaze.death`, ดูรายชื่อได้ที่ [Sounds.json](https://minecraft.wiki/w/Sounds.json))
- `Settings.reset_player_health_if_armor` — รีเซ็ต max health เมื่อเกราะหมดอายุ
- `Messages.*` — ข้อความทั้งหมด (ใช้รหัสสี `&`)
- `DateFormat.*` — ข้อความ lore บนไอเทมและชื่อหน่วยเวลา

**ระวัง:** ห้ามแก้ `DateFormat.expires_at` / `useful_life` หลังจากมีไอเทมที่ประทับเวลาไว้แล้ว
เพราะจะทำให้ปลั๊กอินจำไอเทมเดิมไม่ได้ และหลีกเลี่ยงการลงท้าย prefix ด้วย `&s`

## ทุกเวอร์ชัน 1.x → 2.0

- ไอเทมที่ประทับเวลาไว้ด้วย v1 ยังทำงานถูกต้อง (รูปแบบ lore และวันที่ `yyyy-MM-dd HH:mm:ss` ไม่เปลี่ยน)
- config เก่าใช้ต่อได้ — key ที่ขาดจะถูกเติมค่า default ให้อัตโนมัติ (v1 จะ crash ถ้า key ขาด)
- `Settings.Sounds` (boolean เดิม) เปลี่ยนเป็นกลุ่ม `Settings.sound` — ตั้งค่าใหม่ได้เลย
- ตัด bStats telemetry ออก

## เทคนิค (สำหรับคนดูแลโค้ด)

- compile กับ `spigot-api 1.13.2` (API ต่ำสุด) แล้วประกาศ `api-version: '1.13'` → jar เดียวโหลดได้ 1.13 ถึง 26.x
- ไม่มีการแตะ NMS/CraftBukkit internals เลย
- เสียงเล่นผ่าน `playSound(String key)` จึงไม่สนใจว่า `Sound` เปลี่ยนจาก enum เป็น interface ใน 1.21.3+
- `HealthCompat` reset max health ผ่าน reflection (รองรับทั้ง `MAX_HEALTH` และชื่อเดิม `GENERIC_MAX_HEALTH`)
- วันที่ใช้ `java.time` แทน `java.util.Date`
