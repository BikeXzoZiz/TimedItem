# TimedItem 2.2

Give items a lifetime — they expire (and disappear) when the timer runs out.

เวอร์ชัน 2.x คือ refactor ใหม่ทั้งหมดจากปลั๊กอินต้นฉบับปี 2019 เพื่อรองรับ Minecraft ยุคปัจจุบัน

Licensed under the [MIT License](LICENSE).

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

ได้ไฟล์ `target/TimedItem-2.2.0.jar` (unit tests 18 ตัวรันอัตโนมัติระหว่าง build)

**CI:** GitHub Actions build ให้อัตโนมัติ — ทุก push/PR ได้ artifact (workflow *Build*) และทุกครั้งที่ push tag `v*` จะ build jar แนบเข้า Release ให้เอง (workflow *Release*)

## คำสั่ง (permission: `timeditem.admin`, default: op)

| คำสั่ง | คำอธิบาย |
|---|---|
| `/ti set <เวลา>` | ประทับเวลาบนไอเทมที่ถือ เช่น `/ti set 1d12h30m` หรือแบบเก่า `/ti set 120 s` |
| `/ti add <เวลา>` | ต่อเวลา — ทำงานกับทั้งไอเทมที่เริ่มนับแล้ว, ยังไม่เริ่ม และ paused |
| `/ti pause` | หยุดเวลาชั่วคราว — ไอเทมไม่หมดอายุระหว่างหยุด |
| `/ti resume` | เดินเวลาต่อจากเดิม |
| `/ti remove` | ถอด timer ออกจากไอเทมที่ถือ |
| `/ti info` | ดูเวลาที่เหลือ / lifetime / เวลาที่หยุดไว้ |
| `/ti menu` | เปิด GUI — กด preset เวลา / pause-resume / remove ทำงานกับไอเทมที่ถือ |
| `/ti reload` | โหลด config ใหม่ (task ต่าง ๆ เริ่มใหม่ตาม config ด้วย) |

หน่วยเวลา (ปรับได้ใน config): `s` วินาที, `m` นาที, `h` ชั่วโมง, `d` วัน — ใส่รวมหลายหน่วยได้ เช่น `2d6h`

## ไอเทมจะหายเมื่อไหร่?

ตรวจจาก: periodic check ทุก 10 วิ (ปรับ/ปิดได้) · คลิกใน inventory · ใช้/คลิกขวา · เข้าเซิร์ฟ · โดนดาเมจ (เกราะ) · **หยิบไอเทมจากพื้น** · และเลือกได้ว่าให้สแกนไอเทมบนพื้น/ใน item frame/บน armor stand เป็นรอบด้วย (`Settings.expire_check.scan_world_items`, default ปิดเพราะแพง)

เมื่อหมดอายุ: ลบไอเทม → เสียง → particle → ข้อความ → รัน `expiry_commands` (ถ้ามี) และถ้าเป็นเกราะเพิ่ม max health จะรีเซ็ตเป็น 20 (เฉพาะเมื่อค่าปัจจุบันเกิน 20)

**เตือนล่วงหน้า:** ข้อความ + เสียง 1 ครั้งเมื่อเข้าสู่หน้าต่างเตือน (default 60 วิสุดท้าย, ต้องเปิด expire_check)

## การจัดเก็บข้อมูล timer (สำคัญ)

- **1.14+**: เก็บใน **PersistentDataContainer (NBT ของไอเทม)** เป็นตัวจริง — แก้ lore ยังไง timer ก็ไม่หาย และเป็น epoch millis จึงไม่กระทบ timezone
- **lore** ยังถูกเขียนพร้อมกันเสมอเพื่อแสดงผล และยังเป็นที่เก็บจริงบนเซิร์ฟ 1.13 / ไอเทมเก่า (v1/v2.0 ใช้ต่อได้ทันที)

## การตั้งค่า (`config.yml`)

- `Settings.start_date_calculate_on_click` — `true` = เริ่มนับเมื่อคลิก/ใช้ครั้งแรก, `false` = เริ่มทันทีตอน set
- `Settings.expire_check.enabled` / `interval_seconds` / `scan_world_items`
- `Settings.warning.*` — เตือนล่วงหน้า (เปิด/ปิด, กี่วินาที, เสียง)
- `Settings.particles.*` — particle ตอนหมดอายุ (ชื่อไม่รู้จัก fallback เป็น FLAME)
- `Settings.expiry_commands` — คำสั่ง console ตอนหมดอายุ (มี `{player}`)
- `Settings.actionbar.*` — countdown บน actionbar ตอนถือไอเทม
- `Settings.sound.*` — เสียงตอนหมดอายุ (sound key, ดู [Sounds.json](https://minecraft.wiki/w/Sounds.json))
- `Settings.metrics` — bStats anonymous statistics
- `Messages.*` / `DateFormat.*` — ข้อความและ lore (ห้ามแก้ DateFormat หลังมีไอเทมอยู่, หลีกเลี่ยง `&s` ท้าย prefix)

**bStats:** ลงทะเบียนที่ [bStats.org](https://bstats.org/) แล้วเปลี่ยน `BSTATS_PLUGIN_ID` ใน `TimedItemPlugin.java` (ตอนนี้เป็น placeholder)

## Developer API

**Events** (package `me.albert.timeditem.api`):

```java
@EventHandler
public void onExpire(ItemExpireEvent event) {
    Player p = event.getPlayer();      // null = หมดอายุนอก inventory เช่น บนพื้น
    ItemStack item = event.getItem();
    if (shouldRefund(p)) {
        event.setCancelled();          // กันการลบ / ทำ refund
    }
}

@EventHandler
public void onStart(ItemTimerStartEvent event) { ... }  // lifetime เริ่มนับ
```

**Facade** — `TimedItemAPI` (เรียกจาก main thread):

```java
TimedItemAPI.isTimed(item)          // มี timer ไหม
TimedItemAPI.getType(item)          // "expire" | "lifetime" | "paused" | null
TimedItemAPI.getRemainingSeconds(item)
TimedItemAPI.setLifetime(item, 3600)
TimedItemAPI.setExpiryFromNow(item, 600)
TimedItemAPI.pause(item) / resume(item) / clear(item)
```

**PlaceholderAPI** (soft-depend — ไม่มี PAPI ก็รันปกติ): `%timeditem_remaining%` (raw วินาที), `%timeditem_time%` (pretty), `%timeditem_type%`, `%timeditem_date%` — อ้างจากไอเทมในมือหลัก

**Discord แจ้งเตือน** ผ่าน [DiscordSRV](https://github.com/DiscordSRV/DiscordSRV) โดยไม่ต้องแตะโค้ด:

```yaml
expiry_commands:
  - "discord message timed-items {player}'s timed item expired!"
```
(ชื่อ channel ตาม config ของ DiscordSRV — วิธีนี้ยังใช้กับ plugin อื่นที่มี console command ได้ด้วย)

## ทุกเวอร์ชันเก่า → 2.2

- ไอเทม v1/v2.0/v2.1 ยังทำงาน (อ่านจาก lore อัตโนมัติ, ครั้งถัดไปที่แก้ไขจะย้ายเข้า PDC เอง)
- config เก่าอัปเกรดอัตโนมัติ (key ขาดเติม default ให้)
- v2.2 เพิ่ม: PDC storage, GUI menu, event API + facade, PlaceholderAPI, หมดอายุบนพื้น/เฟรม, unit tests, MIT License

## เทคนิค (สำหรับคนดูแลโค้ด)

- compile กับ `spigot-api 1.13.2` + `api-version: '1.13'` → jar เดียว 1.13 → 26.x, ไม่แตะ internals
- `PdcCompat` / `HealthCompat` / `ParticleCompat` resolve ผ่าน reflection (รองรับ enum→interface ทุกจุด: Sound, Attribute, Particle)
- Actionbar ใช้ legacy Spigot API แบบ self-disabling fallback
- `TimeLore.analyze` อ่านข้อมูลครั้งเดียวต่อ stack (TimerInfo) — hot path ไม่ copy ItemMeta ซ้ำ
- bStats shaded + relocated (`me.albert.timeditem.libs.bstats`); tests: JUnit 5 ผ่าน surefire
