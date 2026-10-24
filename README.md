# TimedItem 2.1

Give items a lifetime — they expire (and disappear) when the timer runs out.

เวอร์ชัน 2.x คือ refactor ใหม่ทั้งหมดจากปลั๊กอินต้นฉบับปี 2019 เพื่อรองรับ Minecraft ยุคปัจจุบัน

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

ได้ไฟล์ `target/TimedItem-2.1.0.jar` นำไปวางในโฟลเดอร์ `plugins/` ของเซิร์ฟเวอร์ได้เลย

**CI:** GitHub Actions build ให้อัตโนมัติ — ทุก push/PR จะได้ artifact (workflow *Build*) และทุกครั้งที่ push tag `v*` จะ build jar แล้วแนบเข้า Release ให้เอง (workflow *Release*)

## คำสั่ง (permission: `timeditem.admin`, default: op)

| คำสั่ง | คำอธิบาย |
|---|---|
| `/ti set <เวลา>` | ประทับเวลาบนไอเทมที่ถือ เช่น `/ti set 1d12h30m` หรือแบบเก่า `/ti set 120 s` |
| `/ti add <เวลา>` | ต่อเวลา — ไอเทมที่เริ่มนับแล้วบวกเข้าเวลาที่เหลือ, ยังไม่เริ่มบวกเข้า lifetime, หยุดเวลาอยู่ก็ต่อได้ |
| `/ti pause` | หยุดเวลาชั่วคราว — ไอเทมจะไม่หมดอายุระหว่างหยุด (lore เปลี่ยนเป็น Paused) |
| `/ti resume` | เดินเวลาต่อจากเดิมที่หยุดไว้ |
| `/ti remove` | ถอด timer ออกจากไอเทมที่ถือ |
| `/ti info` | ดูเวลาที่เหลือ / lifetime / เวลาที่หยุดไว้ |
| `/ti reload` | โหลด config ใหม่ (task ต่าง ๆ เริ่มใหม่ตาม config ด้วย) |

หน่วยเวลา (ปรับได้ใน config): `s` วินาที, `m` นาที, `h` ชั่วโมง, `d` วัน — ใส่รวมหลายหน่วยได้ เช่น `2d6h`

## ไอเทมจะหายเมื่อไหร่?

ปลั๊กอินตรวจสอบไอเทมหมดอายุจาก:
- ตรวจอัตโนมัติเป็นรอบทุก 10 วินาที (ปรับ/ปิดได้ที่ `Settings.expire_check`)
- ผู้เล่นคลิกไอเทมใน inventory
- ผู้เล่นใช้/คลิกขวาไอเทม
- ผู้เล่นเข้าเซิร์ฟเวอร์ (สแกนทั้ง inventory)
- ผู้เล่นโดนดาเมจ (เกราะที่สวมอยู่)

เมื่อหมดอายุ: ไอเทมถูกลบ เล่นเสียง ปล่อย particle (ปรับได้) ส่งข้อความ รันคำสั่งที่ตั้งไว้ (ถ้ามี)
หากเป็นเกราะที่เพิ่ม max health และเปิด `reset_player_health_if_armor` จะรีเซ็ต max health กลับเป็น 20 ด้วย

**เตือนล่วงหน้า:** เมื่อเวลาที่เหลือเข้าสู่หน้าต่างเตือน (default 60 วินาทีสุดท้าย) ผู้เล่นจะได้รับข้อความ + เสียงเตือนหนึ่งครั้ง (ต้องเปิด `expire_check` ไว้)

## การตั้งค่า (`config.yml`)

- `Settings.start_date_calculate_on_click` — `true` = เริ่มนับเมื่อคลิก/ใช้ครั้งแรก, `false` = เริ่มนับทันทีตอน `/ti set`
- `Settings.expire_check.enabled` / `interval_seconds` — การตรวจหมดอายุอัตโนมัติ
- `Settings.warning.*` — การเตือนล่วงหน้า (เปิด/ปิด, กี่วินาทีสุดท้าย, เสียง)
- `Settings.particles.*` — particle ตอนไอเทมหมดอายุ (ชื่อไม่รู้จักจะ fallback เป็น FLAME)
- `Settings.expiry_commands` — คำสั่ง console ที่รันเมื่อไอเทมหมดอายุ (มี `{player}`)
- `Settings.actionbar.enabled` / `format` — countdown บน actionbar ตอนถือไอเทม
- `Settings.sound.*` — เสียงตอนหมดอายุ (ใช้ sound key เช่น `entity.blaze.death`, ดูได้ที่ [Sounds.json](https://minecraft.wiki/w/Sounds.json))
- `Settings.metrics` — เปิด/ปิด bStats anonymous statistics
- `Settings.reset_player_health_if_armor` — รีเซ็ต max health เมื่อเกราะหมดอายุ
- `Messages.*` — ข้อความทั้งหมด (ใช้รหัสสี `&`)
- `DateFormat.*` — ข้อความ lore บนไอเทมและชื่อหน่วยเวลา

**ระวัง:** ห้ามแก้ `DateFormat.expires_at` / `useful_life` / `paused` หลังจากมีไอเทมที่ประทับเวลาไว้แล้ว
เพราะจะทำให้ปลั๊กอินจำไอเทมเดิมไม่ได้ และหลีกเลี่ยงการลงท้าย prefix ด้วย `&s`

**bStats:** ถ้าจะเปิดสถิติจริงจัง ให้ลงทะเบียนปลั๊กอินที่ [bStats.org](https://bstats.org/) แล้วเปลี่ยน `BSTATS_PLUGIN_ID` ใน `TimedItemPlugin.java` เป็น ID จริง (ตอนนี้เป็น placeholder)

## ทุกเวอร์ชัน 1.x/2.0 → 2.1

- ไอเทมที่ประทับเวลาไว้ด้วย v1/v2.0 ยังทำงานถูกต้อง (รูปแบบ lore และวันที่ `yyyy-MM-dd HH:mm:ss` ไม่เปลี่ยน)
- config เก่าใช้ต่อได้ — key ที่ขาดจะถูกเติมค่า default ให้อัตโนมัติ
- v2.1 เพิ่ม: `/ti add|pause|resume`, เตือนล่วงหน้า, particle ตอนหมดอายุ, คำสั่งตอนหมดอายุ, actionbar countdown, bStats

## เทคนิค (สำหรับคนดูแลโค้ด)

- compile กับ `spigot-api 1.13.2` (API ต่ำสุด) แล้วประกาศ `api-version: '1.13'` → jar เดียวโหลดได้ 1.13 ถึง 26.x
- ไม่มีการแตะ NMS/CraftBukkit internals เลย
- เสียงเล่นผ่าน `playSound(String key)` จึงไม่สนใจว่า `Sound` เปลี่ยนจาก enum เป็น interface ใน 1.21.3+
- `HealthCompat` / `ParticleCompat` resolve ค่าคงที่ผ่าน reflection (รองรับทั้งชื่อเก่า/ใหม่และ enum/interface ทั้งคู่)
- Actionbar ใช้ legacy Spigot API แบบ self-disabling (เซิร์ฟไหนไม่รองรับจะปิดเงียบ ๆ)
- วันที่ใช้ `java.time` แทน `java.util.Date`
- bStats ถูก shade + relocate เข้า jar (`me.albert.timeditem.libs.bstats`)
