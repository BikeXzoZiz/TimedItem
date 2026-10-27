<div align="center">

# ⏳ TimedItem

**Give items a lifetime — they expire and disappear when the timer runs out.**

[![Release](https://img.shields.io/github/v/release/BikeXzoZiz/TimedItem)](https://github.com/BikeXzoZiz/TimedItem/releases)
[![Minecraft](https://img.shields.io/badge/MC-1.13%E2%80%9326.x-green)](#-folia--ความเข้ากันได้)
[![Java](https://img.shields.io/badge/Java-8%2B-orange)](#-folia--ความเข้ากันได้)
[![License](https://img.shields.io/github/license/BikeXzoZiz/TimedItem)](LICENSE)
[![bStats](https://img.shields.io/bstats/servers/34946)](https://bstats.org/plugin/bukkit/TimedItemRewrite/34946)

*Spigot / Paper / Purpur / Pufferfish / Leaf / Folia — jar เดียว ใช้ได้ทุกเวอร์ชัน*

[เริ่มใช้](#-เริ่มใช้ใน-1-นาที) · [ใช้จริงทำอะไรได้](#-ใช้จริงทำอะไรได้บ้าง) · [คำสั่ง](#-คำสั่ง) · [Integration](#-integration-ปลั๊กอินอื่น) · [การตั้งค่า](#️-การตั้งค่า) · [Developer API](#-developer-api) · [FAQ](#-faq) · [Changelog](CHANGELOG.md)

</div>

---

## 📋 สารบัญ

- [✨ จุดเด่น](#-จุดเด่น)
- [🚀 เริ่มใช้ใน 1 นาที](#-เริ่มใช้ใน-1-นาที)
- [🎯 ใช้จริงทำอะไรได้บ้าง](#-ใช้จริงทำอะไรได้บ้าง)
- [📖 คำสั่ง](#-คำสั่ง)
- [⏱️ กลไกหมดอายุ](#️-กลไกหมดอายุ)
- [⚙️ การตั้งค่า](#️-การตั้งค่า)
- [🌐 หลายภาษา](#-หลายภาษา)
- [💬 Discord](#-discord)
- [🧩 Integration ปลั๊กอินอื่น](#-integration-ปลั๊กอินอื่น)
- [🔌 Developer API](#-developer-api)
- [💾 การจัดเก็บข้อมูล](#-การจัดเก็บข้อมูล)
- [🍃 Folia & ความเข้ากันได้](#-folia--ความเข้ากันได้)
- [⬆️ Migration](#️-migration)
- [❓ FAQ](#-faq)
- [🛠 เทคนิคสำหรับผู้ดูแลโค้ด](#-เทคนิคสำหรับผู้ดูแลโค้ด)

---

## ✨ จุดเด่น

| | ฟีเจอร์ | รายละเอียด |
|---|---|---|
| ⏱️ | **Timer 5 แบบ** | หมดอายุตามวัน (expire) · เริ่มนับเมื่อใช้ครั้งแรก (lifetime) · หยุดชั่วคราว (pause) · **นับตอนออนไลน์ (playtime)** · **นับตอนถือ/สวม (held)** |
| 🔥 | **แปลงร่างเมื่อหมดอายุ** | ของหมดอายุกลายเป็น "เถ้าถ่าน"/ของอื่นแทนการหาย — vanilla หรือ custom item ก็ได้ (ตั้งแต่ 2.8.0) |
| 🎟️ | **ตัวต่ออายุ + Grace period** | แจก token ต่ออายุ (คลิกขวา) · ช่วงท้ายใช้งานไม่ได้ก่อนหายจริง (ตั้งแต่ 2.8.0) |
| 📉 | **Visual decay + Hologram** | ใกล้หมดอายุชื่อเปลี่ยนสี/มี glint · ตัวเลขนับถอยหลังลอยเหนือของบนพื้น · lore อัปเดตเวลาสด (3.0.0) |
| 📦 | **สแกนตู้เก็บของ + Anti-hide** | ตรวจของหมดอายุในตู้ (ตอนเปิด / เป็นรอบ) · บล็อกการยัดของใกล้หมดอายุลงตู้ · มองเข้าไปใน **shulker ซ้อน** ด้วย (3.1.0) |
| 🌍 | **Restore ข้ามเซิร์ฟ** | network mode กับฐานข้อมูลแชร์: /ti restore เห็นไอเทมที่หมดอายุจากทุกเซิร์ฟ (3.1.0) |
| 🎮 | **ของเช่าคืนอัตโนมัติ** | /ti give ฝังเจ้าของใน NBT — หมดอายุแล้วของกลับเข้า inventory เจ้าของเอง (3.1.0) |
| 🚫 | **บล็อกขายในร้าน** | QuickShop · zAuctionHouse · CrazyAuctions · zShop: ลงขายของจับเวลาไม่ได้ (3.1.0, zShop 3.1.1) |
| 💾 | **เก็บใน NBT** | ตัวจับเวลาอยู่ใน PersistentDataContainer ของไอเทม — แก้ lore ยังไง timer ก็ไม่หาย |
| 🎨 | **GUI ครบ** | `/ti menu` ตั้งเวลา/หยุด-เดินเวลา · `/ti restore` กู้คืนไอเทมที่หมดอายุแล้ว |
| 🗑️ | **หมดอายุทุกที่** | ในตัว ในมือ บนพื้น ใน item frame บน armor stand — ตรวจแบบ event และเป็นรอบ |
| 🔔 | **เตือนล่วงหน้า** | ข้อความ + เสียง + actionbar countdown หลายระยะ เช่น 10 นาที / 1 นาที / 10 วินาที |
| 💬 | **Discord webhook ในตัว** | แจ้ง Discord ทุกครั้งที่ไอเทมหมดอายุ ไม่ต้องติดตั้งปลั๊กอินอื่น |
| 🌐 | **หลายภาษา** | อังกฤษ / ไทย พร้อมใช้ หรือวางไฟล์ภาษาของคุณเอง — GUI ก็เปลี่ยนตาม |
| 🧩 | **Integration 9 ปลั๊กอิน** | MMOItems · ItemsAdder · Oraxen · Nexo · ExecutableItems · Slimefun · EcoItems · Shopkeepers · CrazyCrates — สร้างของ custom เป็น timed item ได้โดยตรง (ตั้งแต่ 2.6.0) |
| 🗄️ | **ฐานข้อมูลหลายตัว** | เลือกที่เก็บ recovery / audit / สถิติได้: ไฟล์ (default) · SQLite · MySQL/MariaDB · MongoDB (ตั้งแต่ 2.7.0) |
| 🔍 | **เครื่องมือแอดมิน** | `/ti find` ค้นหาไอเทมจับเวลาทั้งเซิร์ฟ · `/ti stats` สถิติการหมดอายุ · `/ti audit` ประวัติการกระทำทั้งหมด (ตั้งแต่ 2.7.0) |
| 🍃 | **รองรับ Folia** | ทำงานถูกต้องบน threading แบบ region (ตั้งแต่ 2.4.0) |
| 🔌 | **Developer API** | Events, Facade, PlaceholderAPI — ผสานกับปลั๊กอินของคุณได้ทันที |
| ♻️ | **กู้คืนได้** | ทุกไอเทมที่หมดอายุถูกบันทึกไว้ แอดมินคืนให้ผู้เล่นได้ด้วยคลิกเดียว |

## 🚀 เริ่มใช้ใน 1 นาที

1. ดาวน์โหลด `TimedItem-3.2.0.jar` จาก [Releases](https://github.com/BikeXzoZiz/TimedItem/releases) แล้ววางในโฟลเดอร์ `plugins/`
2. รีสตาร์ทเซิร์ฟเวอร์ — เสร็จ! (ค่า default ใช้ได้ทันที)
3. ลองเลย:

```yaml
# ถือไอเทมที่ต้องการแล้วพิมพ์
/ti set 1d12h30m     # ให้ไอเทมหมดอายุใน 1 วัน 12 ชั่วโมง 30 นาที
/ti menu             # เปิด GUI ตั้งค่าแบบคลิก
/ti give Steve 30m   # ส่งไอเทม timed ให้ผู้เล่น
/ti give Steve mmoitems:SWORD:CUTLASS 30d   # สร้างของจาก MMOItems เป็น timed item โดยตรง
```

> **Build จากซอร์ส:** ต้องมี JDK 8+ (แนะนำ 17/21) และ Maven → `mvn package` ได้ไฟล์ที่ `target/` (unit tests รันอัตโนมัติระหว่าง build)
> **การเปิด Release:** สร้าง release ที่ tag `v*` แล้วแนบ jar จาก `target/` ด้วยตนเอง

## 🎯 ใช้จริงทำอะไรได้บ้าง

| สถานการณ์ | วิธีทำด้วย TimedItem |
|---|---|
| 🏰 **เช่าอุปกรณ์ในดัน/ผจญภัย** | ร้าน Shopkeepers + `auto_timer: "2h"` — ผู้เล่นกดซื้อเช่า หมดเวลาของหายเอง ไม่ต้องไล่เก็บคืน |
| 🎁 **รางวัล crate มีอายุ** | CrazyCrates + `auto_timer: "7d"` — ของที่ได้ต้องรีบใช้ กระตุ้นให้ผู้เล่นกลับมาเข้าเล่น |
| 🗡️ **แจกของ custom แบบมีเวลา** | `/ti give Steve mmoitems:SWORD:CUTLASS 30d` — ของจาก MMOItems / ItemsAdder / Oraxen / Slimefun ฯลฯ สร้างเป็นของมีอายุทันที (console ก็ได้) |
| 🎪 **ของ event เริ่มนับตอนใช้จริง** | ใช้ **lifetime** (`start_date_calculate_on_click: true`) — แจกวันนี้ได้เลย เวลาเริ่มนับตอนผู้เล่นใช้ครั้งแรกเท่านั้น |
| ⏸️ **ผู้เล่นลาไปงาน/ทหารไปค่าย** | `/ti pause` หยุดเวลาไว้ก่อน กลับมา `/ti resume` — ของไม่หมดอายุระหว่างที่ไม่ได้เล่น |
| 🛡️ **กันของสะสมตลอดกาล (no-OP gear)** | `/ti set 30d` + เตือนล่วงหน้าหลายระยะ (`stages: [600, 60, 10]`) + Discord webhook แจ้งแอดมินทุกครั้งที่ของหมดอายุ |
| 📅 **สวัสดิ์รายเดือน / VIP ชั่วคราว** | แจกของพร้อม `/ti set 30d` ทุกเดือน — ของเก่าหมดอายุเอง วงจรรายเดือนเดินเอง |
| 🧪 **ของทดลองใช้ก่อนซื้อ (trial)** | `/ti give Steve ei:LEGENDARY_BOW 24h` — ให้ลองของก่อนตัดสินใจซื้อของจริง หมดเวลาหายเอง |
| 🗓️ **รางวัลภารกิจรายวัน/รายสัปดาห์** | ของ reward จากภารกิจติด timer (`/ti set 24h` / `7d`) — รางวัลเก่าหมดอายุเอง ไม่สะสมเป็นภูเขา |
| ⚔️ **ของช่วง PVP / Raid event** | อาวุธพิเศษ event จำกัดเวลา `/ti give <player> <plugin:id> 6h` — จบ event ของหายเอง ไอเทมเทพไม่หลุดไปอยู่ในคลังคนอื่น |
| 🌱 **Starter kit ผู้เล่นใหม่** | เกราะ + เครื่องมือพร้อมใช้ 7 วัน — ช่วยช่วงเริ่มต้นแต่บังคับให้เดินหน้า ไม่ใช่ของตลอดชีพ |
| 💾 **กลัวของผู้เล่นหายฟ้องเซิร์ฟ** | ทุกของที่หมดอายุถูกสแนปช็อตลง recovery log ก่อนลบ — `/ti restore` คลิกคืนให้ได้ภายใน 7 วัน |

## 📖 คำสั่ง

Permission ทั้งหมด: `timeditem.admin` (default: op) — ทุกคำสั่งมี **Tab completion** (ชื่อคำสั่ง, ตัวอย่างเวลา, ผู้เล่นออนไลน์, เลขหน้า)

| คำสั่ง | คำอธิบาย |
|---|---|
| `/ti set <เวลา>` | ประทับเวลาบนไอเทมที่ถือ เช่น `/ti set 1d12h30m` หรือแบบเก่า `/ti set 120 s` — เติม `transform:<key>` เพื่อระบุผลแปลงร่างตอนหมดอายุ เช่น `/ti set 7d transform:ash` |
| `/ti set playtime <เวลา>` / `/ti set held <เวลา>` | ตัวจับเวลาแบบนับเฉพาะตอน **ออนไลน์** (playtime) หรือตอน **ถือ/สวม** (held) — เช่น `/ti set playtime 10h` |
| `/ti add <เวลา>` | ต่อเวลา — ใช้ได้ทั้งไอเทมที่เริ่มนับแล้ว, ยังไม่เริ่ม และ paused |
| `/ti give <ผู้เล่น> <เวลา> [จำนวน]` | หยิบไอเทมในมือ ตั้งเวลา แล้วส่งให้ผู้เล่นเป้าหมายทันที (default 1 ชิ้น, หักจากมือผู้ส่ง) — รองรับ `transform:<key>` เหมือนกัน |
| `/ti give <ผู้เล่น> playtime:<เวลา> [จำนวน]` / `held:<เวลา>` | แจกไอเทมพร้อมตัวจับเวลาแบบ playtime/held ทันที — ใช้กับของ custom ได้: `/ti give Steve mmoitems:SWORD:X playtime:5h` |
| `/ti give <ผู้เล่น> <plugin:id> <เวลา> [จำนวน]` | สร้างของ custom จากปลั๊กอินที่ integrate เป็น timed item ทันที เช่น `/ti give Steve mmoitems:SWORD:CUTLASS 30d` — console ใช้ได้ (ดู [Integration](#-integration-ปลั๊กอินอื่น)) |
| `/ti pause` | หยุดเวลาชั่วคราว — ไอเทมไม่หมดอายุระหว่างหยุด |
| `/ti resume` | เดินเวลาต่อจากเดิม |
| `/ti remove` | ถอด timer ออกจากไอเทมที่ถือ |
| `/ti info` | ดูเวลาที่เหลือ / lifetime / เวลาที่หยุดไว้ |
| `/ti menu` | เปิด GUI — preset เวลา / pause-resume / remove / สถานะสด |
| `/ti restore [หน้า\|ผู้เล่น]` | เปิด GUI กู้คืนไอเทมที่หมดอายุ — network mode กับฐานข้อมูลแชร์: ดูของของผู้เล่นจากทุกเซิร์ฟได้ (`/ti restore <ผู้เล่น>`) |
| `/ti find <material\|plugin:id\|ชื่อ>` | ค้นหาไอเทมจับเวลาทั้งเซิร์ฟ — ในตัวผู้เล่น, ตู้เก็บของที่โหลดอยู่, บนพื้น, ใน item frame และ armor stand พร้อมพิกัด (บน Folia ค้นเฉพาะผู้เล่นออนไลน์) |
| `/ti stats [วัน]` | สถิติไอเทมที่หมดอายุ แยกตามชนิด (default 7 วันล่าสุด, สูงสุด 90) |
| `/ti admin [หน้า] [filter]` | **GUI ภาพรวมฝั่งแอดมิน** — ไอเทมจับเวลาทั้งเซิร์ฟจากการสแกนล่าสุด (เจ้าของ/ประเภท/เวลาที่เหลือ) คลิกซ้าย = expire ทันที, คลิกขวา = ลบตัวจับเวลา |
| `/ti check <ผู้เล่น>` | รายการไอเทมจับเวลาทั้งหมดที่ผู้เล่นถือ พร้อมช่อง/ประเภท/เวลาที่เหลือ (ออนไลน์เท่านั้น) |
| `/ti audit [หน้า]` | ประวัติการกระทำที่ผ่าน `/ti` ทั้งหมด (set/add/pause/resume/remove/give/restore) พร้อมผู้กระทำ, เป้าหมาย และไอเทม |
| `/ti debug [on\|off\|export]` | เครื่องมือไล่บั๊ก — ดูสถานะ, เปิด/ปิด verbose logging ของ session, หรือเขียนรายงานบั๊กเป็นไฟล์ `plugins/TimedItem/debug/` (ตั้ง default ที่ `Settings.debug`) |
| `/ti reload` | โหลด config + ไฟล์ภาษาใหม่ (task เริ่มใหม่ตาม config) |

หน่วยเวลา (ปรับได้): `s` วินาที · `m` นาที · `h` ชั่วโมง · `d` วัน — รวมหลายหน่วยได้ เช่น `2d6h`

## ⏱️ กลไกหมดอายุ

**ตรวจจาก:** periodic check ทุก 10 วิ (ปรับ/ปิดได้) · คลิกใน inventory · ใช้/คลิกขวา · เข้าเซิร์ฟ · โดนดาเมจ (เกราะ) · หยิบจากพื้น · และเลือกได้ว่าให้สแกนของบนพื้น/เฟรม/armor stand เป็นรอบด้วย (`scan_world_items`, default ปิดเพราะแพง)

**เมื่อหมดอายุ:**

```
ลบไอเทม → บันทึก recovery log → เสียง → particle → ข้อความ → expiry_commands → Discord webhook
```

- เกราะที่เพิ่ม max health จะถูกรีเซ็ตเป็น 20 ด้วย (กัน exploit)
- บนพื้น/เฟรม: คำสั่งที่มี `{player}` จะถูกข้าม (ไม่มีผู้เล่นอ้างอิง)
- **เตือนล่วงหน้าหลายระยะ:** `Settings.warning.stages` เช่น `[600, 60, 10]` = เตือนตอนเหลือ 10 นาที, 1 นาที, 10 วินาที (ครั้งละหนึ่งขั้น)
- **ผู้เล่น creative ถูกข้ามโดยดีฟอลต์** — ของ timed ในมือ creative ไม่หมดอายุและไม่มี countdown (`expire_check.ignore_creative`, default `true` เหมือนทุกเวอร์ชันก่อน 2.3.1)

<details>
<summary>🖼️ หน้าตา lore บนไอเทม (สำหรับอ้างอิง / เขียนเครื่องมือเสริม)</summary>

```yaml
# แบบ expire — นับอยู่ จะหมดอายุวันที่กำหนด
&cExpires: 2026-11-25 18:00:00

# แบบ lifetime — ยังไม่เริ่มนับ (วินาทีรวม + ป้ายหน่วย)
&7Lifetime: 2592000 30d

# แบบ paused — หยุดเวลาไว้ชั่วคราว
&7Paused: 3600 1h
```

> บรรทัดจริงมีตัวคั่นภายใน `§s` ฝังอยู่ระหว่างค่า เพื่อให้ parser อ่านกลับได้แม้ผู้เล่นเปลี่ยนชื่อ item — ทุกบรรทัดอ่าน/เขียนผ่าน `TimeLore` เท่านั้น และ **ห้ามแก้ prefix** ใน `DateFormat` ของไฟล์ภาษาหลังมีไอเทมอยู่แล้ว (prefix คือตัวระบุบรรทัด) · บน 1.14+ ค่าจริงเก็บใน PDC ของ item อยู่แล้ว lore เป็นแค่จอแสดงผล

</details>

## ⚙️ การตั้งค่า

ทุก key มี default ในตัว — config ขาด/เก่าแค่ไหนก็ self-heal ไม่มีวันพัง

| Key | คำอธิบาย |
|---|---|
| `Settings.language` | ไฟล์ข้อความ: `en`, `th` หรือไฟล์ `lang/messages_<x>.yml` ของคุณ |
| `Settings.start_date_calculate_on_click` | `true` = เริ่มนับเมื่อคลิก/ใช้ครั้งแรก · `false` = เริ่มทันทีตอน set |
| `Settings.expire_check.enabled` / `interval_seconds` / `scan_world_items` / `ignore_creative` | การสแกนเป็นรอบ + ข้าม creative |
| `Settings.warning.*` | เตือนล่วงหน้า: `enabled`, `stages`, `seconds` (fallback เมื่อ stages ว่าง), เสียง |
| `Settings.particles.*` | particle ตอนหมดอายุ (ชื่อไม่รู้จัก fallback เป็น FLAME) |
| `Settings.sound.*` | เสียงตอนหมดอายุ (sound key ดู [Sounds.json](https://minecraft.wiki/w/Sounds.json)) |
| `Settings.actionbar.*` | countdown บน actionbar ตอนถือไอเทม (ครอบคลุมทุกประเภท timer) |
| `Settings.expiry_commands` | คำสั่ง console ตอนหมดอายุ มี `{player}` ให้ใช้ |
| `Settings.discord_webhook.*` | แจ้ง Discord ทุกครั้งที่หมดอายุ (ดู[ด้านล่าง](#-discord)) |
| `Settings.recovery.*` | บันทึกของที่หมดอายุไว้กู้คืน (เปิด default, 500 รายการ / 7 วัน) |
| `Settings.blacklist.materials` | material ที่ห้ามมี timer เช่น `[TOTEM_OF_UNDYING]` — บังคับทุกทางรวม API |
| `Hooks.*` | เปิด/ปิด integration รายปลั๊กอิน + auto-timer ของ Shopkeepers/CrazyCrates (ดู[ด้านล่าง](#-integration-ปลั๊กอินอื่น)) |
| `Settings.update_check.*` | เช็คเวอร์ชันใหม่จาก GitHub Releases |
| `Settings.metrics` | bStats anonymous statistics |
| `Settings.debug` | verbose `[Debug]` log ในคอนโซล (default `false`) — สลับได้ทันทีด้วย `/ti debug on\|off` |
| `Messages.*` / `DateFormat.*` / `Gui.*` | อยู่ใน `lang/messages_<language>.yml` ตั้งแต่ 2.4.0 |

> ⚠️ **ห้ามแก้ `DateFormat` หลังมีไอเทมอยู่แล้ว** — prefix เหล่านี้เป็นตัวระบุบรรทัด lore ตอนอ่านค่า และหน่วย `s/m/h/d` ต้องเหมือนกันทุกไฟล์ภาษา

**ตัวอย่างเปิดแจ้งเตือน Discord:**

```yaml
Settings:
  discord_webhook:
    enabled: true
    url: "https://discord.com/api/webhooks/..."
    username: "TimedItem"
    message: "{player}'s timed item expired: {item} ({world})"
```

**bStats:** ลงทะเบียนแล้วเป็น [TimedItemRewrite (ID 34946)](https://bstats.org/plugin/bukkit/TimedItemRewrite/34946) — ID อยู่ที่ `BSTATS_PLUGIN_ID` ใน `TimedItemPlugin.java`, ปิดสถิติได้ด้วย `Settings.metrics: false`

<details>
<summary>📄 <b>ตัวอย่าง config.yml ครบทุก key (ค่า default ล่าสุด)</b> — คลิกขยาย · <sub>บล็อกนี้ sync อัตโนมัติจาก <code>src/main/resources/config.yml</code> โดย <code>scripts/sync-readme.py</code></sub></summary>

<!-- BEGIN:sync-readme:config -->
```yaml
# TimedItem configuration
# Every key has a built-in default - if a key is missing the default is
# used and written back to this file, so a partial config never crashes.
# Messages live in lang/messages_<language>.yml (see Settings.language).

config-version: 13

Settings:
  # Language file to load messages from: lang/messages_en.yml,
  # lang/messages_th.yml, or drop in your own lang/messages_<x>.yml.
  language: "en"
  # true  = the timer starts counting on the item's first click/use
  # false = the timer starts immediately when /ti set is used
  start_date_calculate_on_click: true
  # Reset the player's max health to 20 when expired armor is removed
  # (anti-exploit for health-boosting timed armor).
  reset_player_health_if_armor: true
  # Anonymous usage statistics via bStats (https://bstats.org)
  metrics: true
  # Verbose diagnostics for hunting bugs: extra [Debug] lines in the
  # console (silent fallbacks, every expiry, recovery writes). Toggle it
  # for one session with /ti debug on|off - that does not write back
  # here, reload/restart returns to this value. /ti debug export writes
  # a bug report file to plugins/TimedItem/debug/ (the Discord webhook
  # URL is never included in reports).
  debug: false
  expire_check:
    # Also check every online player's inventory periodically, instead of
    # only on inventory click / item use / join / damage events.
    enabled: true
    interval_seconds: 10
    # Also scan ground items, item frames and armor stands for expired
    # items every interval. Off by default - it walks every loaded world.
    # (On Folia this uses per-entity tracking instead of a world scan.)
    scan_world_items: false
    # true  = creative players are skipped entirely: their timed items
    #         neither expire nor show a countdown/warnings.
    # false = creative players are processed like any other gamemode.
    ignore_creative: true
  warning:
    # Message + sound when an item enters its final seconds.
    # Requires expire_check to be enabled.
    enabled: true
    # Single-stage fallback used when "stages" is empty.
    seconds: 60
    # One warning per threshold (seconds of remaining time), fired as the
    # item counts down through each. Example for warnings at 10m, 1m and 10s:
    # stages: [600, 60, 10]
    stages: [60]
    sound:
      name: "block.note_block.pling"
      volume: 1.0
      pitch: 1.5
  particles:
    # Effect played at the player when an item expires.
    enabled: true
    # Particle name, e.g. FLAME, CRIT, SMOKE, HEART (unknown names fall back to FLAME)
    name: "FLAME"
    count: 30
  expiry_commands:
    # Console commands run when a player's item expires, {player} available. e.g.:
    # - "say {player}'s timed item expired!"
    # - "give {player} diamond 1"
    # Ground/frame expiry has no player: commands containing {player} are
    # skipped, the rest still run.
    []
  expiry_transform:
    # Instead of vanishing, expired items turn into another item
    # (2.8+). Profiles are defined under Settings.transforms; pick one
    # per item with /ti set 7d transform:ash (or ... give Steve 7d
    # transform:ash), leave "default" empty to transform only tagged
    # items.
    enabled: false
    # Profile key for items without their own transform tag ("" = none).
    default: ""
  transforms:
    # One profile per key. Result item: "material" (vanilla) or
    # "custom_id" (plugin:id from an integration, wins over material).
    # name/lore are optional overrides, amount clamps to the stack size,
    # commands run on the console at the moment of the transformation.
    # This example is inactive until expiry_transform.enabled: true:
    #  ash:
    #    material: "GUNPOWDER"
    #    name: "&8Ash"
    #    lore: ["&7Remains of an expired item"]
    #    amount: 1
    #    commands: []
    []
  grace:
    # In the final <seconds> the item can no longer be used (right
    # click / consume is cancelled with a warning) but it still only
    # disappears when the timer reaches zero.
    enabled: false
    seconds: 30
  renewal:
    # Renewal token (2.8+): right-click with the token in one hand and
    # a timed item in the other adds <add_time> and consumes one token.
    # Hand the tokens out with crates (CrazyCrates integration) or /give.
    enabled: false
    add_time: "7d"
    token:
      # Vanilla material OR custom_id ("plugin:id") of the token.
      material: "CLOCK"
      custom_id: ""
      # When set, only tokens with this exact display name count.
      name: ""
  # Countdown timer kinds (2.9+): /ti set playtime <time> counts down only
  # while the owner is online; /ti set held <time> only while held/worn.
  # Give forms: /ti give <player> playtime:<time> [amount] / held:<time>.
  visual_stages:
    # Visual decay (3.0+): when the remaining time drops below a
    # threshold, the item's name gets a color and/or an enchant glint.
    # Vanilla items are recolored, custom items only glow. Order does
    # not matter; list them most severe last for readability. Empty = off.
    #  - at_seconds: 3600
    #    name_color: "&6"
    #    glow: true
    #  - at_seconds: 300
    #    name_color: "&c"
    #    glow: true
    []
  hologram:
    # Floating countdown above dropped timed items (3.0+). Needs 1.19.4+
    # (silently off on older servers) and is not available on Folia.
    enabled: false
    format: "&e⌛ {time}"
    height: 0.5
  live_lore:
    # Rewrite the timer lore line of the held item every second so it
    # shows a live countdown (3.0+). Needs 1.14+; slightly more item
    # writes than the actionbar alone, so it is off by default.
    enabled: false
    format: "&e⌛ {time}"
  container_scan:
    # Scan containers for expired items (3.0+):
    # "open"     = check a container's contents when a player opens it
    #              (cheap, works everywhere including Folia)
    # "periodic" = walk every loaded chunk's containers on a timer
    #              (thorough, classic servers only, heavier)
    # "both"     = both of the above
    enabled: false
    mode: "open"
    interval_seconds: 60
  anti_hide:
    # With container scanning on, players could park items about to
    # expire inside chests nobody opens. This blocks moving items with
    # less than <block_below_seconds> left into containers (3.0+).
    enabled: false
    block_below_seconds: 60
  # Administrative lookups (no player-facing effect):
  # /ti find <material|plugin:id|name> locates timed items in inventories,
  # containers and on the ground; /ti stats [days] shows expiry counts;
  # /ti audit [page] lists recent timer actions.
  sound:
    # Played at the player when an item expires. The name is a sound key
    # which works on every 1.13+ server:
    # https://minecraft.wiki/w/Sounds.json
    enabled: true
    name: "entity.blaze.death"
    volume: 10.0
    pitch: 1.2
  actionbar:
    # Shows the remaining time in the actionbar while holding a timed item
    # (counting down, paused and not-yet-started lifetimes).
    enabled: true
    format: "&e⌛ {time}"
  update_check:
    # Check GitHub Releases for a newer TimedItem version and notify the
    # console and admins on join.
    enabled: true
    # GitHub repository as owner/name used for the release check.
    repository: "BikeXzoZiz/TimedItem"
  recovery:
    # Save every expired item so admins can hand it back with /ti restore.
    enabled: true
    # Maximum entries kept in the log (oldest dropped first).
    max_items: 500
    # Entries older than this many days are pruned on startup.
    keep_days: 7
    # Network restore (3.1+): with a shared mysql/mongodb backend,
    # /ti restore reads one player's expired items from EVERY server of
    # the network instead of local memory. Needs a restart after changing.
    network: false
  return_to_owner:
    # Auto-return (3.1+): /ti give stamps the sender as owner - when the
    # item expires it goes straight back to them (if online) instead of
    # the recovery log. A transformation profile, when set, wins.
    enabled: false
    # Strip the timer before returning, so the item can be rented again.
    remove_timer_on_return: true
  storage:
    # Where recovery entries, the audit log and the expiry statistics
    # live: "flat" (plugins/TimedItem/ files, default), "sqlite",
    # "mysql" (MariaDB/MySQL, for networks sharing one database) or
    # "mongodb". Changing this needs a restart; the first start with a
    # database imports the existing recovery.log automatically
    # (recovery.log -> recovery.log.imported).
    type: "flat"
    mysql:
      host: "localhost"
      port: 3306
      database: "timeditem"
      user: "timeditem"
      password: ""
      use_ssl: false
      pool_size: 4
    sqlite:
      # File name inside plugins/TimedItem/.
      file: "database.db"
    mongodb:
      # Standard MongoDB connection string; the database in the URI is
      # used unless mongodb.database overrides it.
      uri: "mongodb://localhost:27017"
      database: "timeditem"
    # Maximum audit entries kept (/ti audit; timer actions like
    # set/add/pause/give/restore).
    audit_history: 5000
  discord_webhook:
    # Post a message to a Discord channel whenever an item expires
    # (players and ground items). Create the webhook in Discord:
    # channel settings -> Integrations -> Webhooks -> copy URL.
    enabled: false
    url: ""
    # Name shown as the message author on Discord.
    username: "TimedItem"
    # Message template. Placeholders: {player} (- for ground expiry),
    # {item} (material name), {world}.
    message: "{player}'s timed item expired: {item} ({world})"
    # Send as a rich embed instead of a plain message (2.8+). The
    # message template above becomes the embed description.
    use_embed: false
    embed_title: "TimedItem"
    # Embed color, hex without '#'.
    embed_color: "ff5555"
  blacklist:
    # Materials that may never receive a timer via /ti set, /ti add,
    # /ti give or the menu. Items already carrying a timer are unaffected.
    # Example: [TOTEM_OF_UNDYING, NETHERITE_CHESTPLATE]
    materials: []

# Hooks (self-healing): every integration can be disabled with
# Hooks.<id>: false. 3.1+ adds QuickShop, zAuctionHouse,
# CrazyAuctions and zShop - when installed, their sell/list events
# are cancelled for timed items (set the hook to false to allow
# selling there).
```
<!-- END:sync-readme:config -->

</details>

## 🌐 หลายภาษา

ตั้ง `Settings.language` ใน config.yml แล้วข้อความทั้งหมด — แชท, lore, GUI — เปลี่ยนตาม:

| ภาษา | ไฟล์ |
|---|---|
| 🇬🇧 อังกฤษ | `lang/messages_en.yml` (default) |
| 🇹🇭 ไทย | `lang/messages_th.yml` |
| ภาษาอื่น | วางไฟล์ `lang/messages_<code>.yml` ของคุณเอง (key ขาดเติมจาก default อังกฤษให้เอง) |

เคยแก้ข้อความไว้ใน config.yml เวอร์ชันเก่า? อัปเกรดครั้งแรกปลั๊กอินจะย้ายค่าที่คุณแก้เข้าไฟล์ภาษาให้โดยอัตโนมัติ

## 💬 Discord

**วิธีที่ 1 — webhook ในตัว (2.4.0+):** สร้าง webhook ที่ Channel settings → Integrations → Webhooks → คัดลอก URL → วางใน `Settings.discord_webhook.url` เท่านี้

**วิธีที่ 2 — DiscordSRV** ผ่าน `expiry_commands` โดยไม่ต้องแตะโค้ด (ใช้ได้กับปลั๊กอินอื่นที่มี console command ด้วย):

```yaml
expiry_commands:
  - "discord message timed-items {player}'s timed item expired!"
```

## 🧩 Integration ปลั๊กอินอื่น

ตั้งแต่ 2.6.0 TimedItem ตรวจจับปลั๊กอินเหล่านี้อัตโนมัติตอนสตาร์ท (ทั้งหมดเป็น **optional** — ไม่มีติดตั้งก็รันปกติ, ปิดรายตัวได้ที่ `Hooks.<plugin>: false`):

| ปลั๊กอิน | ทำอะไรได้ |
|---|---|
| **MMOItems** | จำไอเทมได้ (type:id จาก MythicLib NBT) + สร้างของเป็น timed item ได้ |
| **ItemsAdder** | จำไอเทมได้ (namespace:id) + สร้างของเป็น timed item ได้ |
| **Oraxen / Nexo** | จำไอเทมได้ (item id) + สร้างของเป็น timed item ได้ |
| **ExecutableItems** | จำไอเทมได้ (executable item id) + สร้างของเป็น timed item ได้ |
| **Slimefun** | จำไอเทมได้ (Slimefun id) + สร้างของเป็น timed item ได้ |
| **EcoItems** | สร้างของจาก id เป็น timed item ได้ (จำจากไอเทมในมือเป็นแบบ best-effort) |
| **CrazyCrates** | สร้าง prize จาก crate เป็น timed item ได้ + auto-timer ตอนเปิด crate |
| **Shopkeepers** | auto-timer — ของที่ผู้เล่นซื้อจากร้านจะมีเวลาหมดอายุอัตโนมัติ |

**1) สร้างของ custom เป็น timed item โดยตรง** — ไม่ต้องหยิบของในมือ, console ใช้ได้:

```
/ti give Steve mmoitems:SWORD:CUTLASS 30d        # MMOItems <TYPE>:<ID>
/ti give Steve itemsadder:iasurvival:ruby_sword 7d
/ti give Steve oraxen:amethyst_sword 7d
/ti give Steve nexo:amethyst_sword 7d
/ti give Steve ei:GKIT 24h                       # ExecutableItems (alias ei)
/ti give Steve slimefun:DAEMON_SWORD 12h
/ti give Steve cc:Classic:GKITS 3d               # CrazyCrates <crate>:<prize> (alias cc)
```

รูปแบบ `<plugin>:<id>` — ตำแหน่งที่ 3 ของคำสั่งถ้ามี `:` จะถือว่าเป็นของ custom ทันที (syntax เดิมที่ใช้ของในมือยังทำงานเหมือนเดิม) พร้อม `%timeditem_itemid%` (PlaceholderAPI) และ webhook Discord `{item}` จะโชว์ชื่อจริงของของ custom แทนชื่อ material

**2) Auto-timer** — ของที่แจกออกจากระบบอื่นจะได้เวลาหมดอายุอัตโนมัติ (default **ปิด**, เติมเวลาเพื่อเปิด):

```yaml
Hooks:
  shopkeepers:
    enabled: true
    auto_timer: "30d"     # ของที่ซื้อจากร้านหมดอายุใน 30 วัน ("" = ปิด)
  crazycrates:
    enabled: true
    auto_timer: "7d"      # ของที่ได้จาก crate หมดอายุใน 7 วัน
```

<details>
<summary>ดูค่า <b>Hooks</b> ครบทุก key (ค่า default)</summary>

```yaml
Hooks:
  mmoitems: true          # จำได้ว่าของเป็นของ MMOItems + /ti give mmoitems:TYPE:ID
  itemsadder: true        # + /ti give itemsadder:namespace:id (alias: ia)
  oraxen: true            # + /ti give oraxen:id
  nexo: true              # + /ti give nexo:id
  executableitems: true   # + /ti give ei:<id> (alias: ei)
  slimefun: true          # + /ti give slimefun:<ID>
  ecoitems: true          # + /ti give ecoitems:<id> (จำจากของเป็นแบบ best-effort)
  crazycrates:
    enabled: true         # /ti give cc:<crate>:<prize> (alias: cc, crate)
    auto_timer: ""        # ของจากการเปิด crate หมดอายุอัตโนมัติ
  shopkeepers:
    enabled: true         # auto-timer ของที่ซื้อจากร้าน (ต้อง Shopkeepers ≥ 2.19)
    auto_timer: ""        # "" = ปิด
  quickshop: true         # บล็อกสร้างร้านขาย timed item (รวม Hikari fork)
  zauctionhouse: true     # บล็อกลงประมูล timed item (V3 + V4)
  crazyauctions: true     # บล็อกลงประมูล timed item
  zshop: true             # บล็อกขาย timed item ใน zShop (sell-all + ปิด sell inventory)
  husksync: true          # re-scan inventory หลัง HuskSync apply ข้อมูล (sync ข้ามเซิร์ฟ)
```

</details>

> **หมายเหตุ**
> - Hook ทำงานผ่าน **reflection ทั้งหมด** — ไม่เพิ่ม dependency, jar เดียวเหมือนเดิม; เวอร์ชัน API ที่ออกแบบกับ: MMOItems 6.10.x · ItemsAdder 4.x · Oraxen 2.x · Nexo 1.x · ExecutableItems ล่าสุด (SCore) · Slimefun RC-38 · Shopkeepers 2.20+ (ต้อง ≥ 2.19 สำหรับ auto-timer) · CrazyCrates ล่าสุด · QuickShop-Hikari 6.x · zAuctionHouse 3.x/4.x · CrazyAuctions ล่าสุด · zShop 3.3.x · HuskSync 3.x/4.x (API v3) ถ้าเวอร์ชัน API ไม่ตรง hook ตัวนั้นจะปิดตัวเองอย่างปลอดภัย (ดูได้ที่ `/ti debug export`)
> - **MythicMobs ยังไม่รองรับ** — item ของ MythicMobs เป็น vanilla stack ไม่มี tag ระบุตัวตน จึงไม่มีทางจำ/สร้างที่เชื่อถือได้ (ใช้ `/ti give` ธรรมดากับของที่สร้างไว้แทน)
> - ของ custom ที่ติด timer จะถูกเก็บ NBT เต็มตอนหมดอายุ (`recovery.log`) — กู้คืนแล้วของยังเป็นของปลั๊กอินเดิมครบทุกอย่าง

### HuskSync (sync ข้ามเซิร์ฟ — 3.1.2+)

ใช้ร่วมกับ [HuskSync](https://github.com/WiIIiam278/HuskSync) (3.x / 4.x) ได้ โดยตรวจจับอัตโนมัติและไม่ต้องตั้งค่าอะไรฝั่ง HuskSync:

- **Timer เดินทางกับไอเทม** — HuskSync sync inventory เป็น raw NBT ทั้งไอเทม PDC + lore countdown จึงไม่หาย ไม่ว่าผู้เล่นจะสลับเซิร์ฟกี่ครั้ง
- **แบบ expire** — เก็บเป็น epoch สัมบูรณ์ นับถอยหลังแม้ผู้เล่นออฟไลน์/อยู่เซิร์ฟอื่น ของจะถูกลบพร้อมบันทึก recovery ทันทีที่ scan เจอหลังเข้าเซิร์ฟ
- **แบบ playtime / held** — นับเฉพาะตอนผู้เล่นออนไลน์ (ไอเทมอยู่กับผู้เล่นตลอด) จึงถูกต้องทุกเซิร์ฟโดยไม่ต้องพึ่งการ sync สถิติของ HuskSync
- **Re-scan หลัง sync** — join scan ของ TimedItem วิ่ง *ก่อน* ที่ HuskSync จะใส่ inventory ที่ sync มา hook จึง re-scan อีกครั้งหลัง sync เสร็จทันที (`BukkitSyncCompleteEvent` — ครอบคลุม join หลังสลับเซิร์ฟ, admin rollback และ /invsee edit ด้วย)
- **Network restore ไม่ชนกัน** — `/ti restore` โหมด network (แชร์ DB) ทำงานต่างหากจาก snapshot ของ HuskSync ได้ตามปกติ

> **Caveat:** ถ้า admin restore snapshot เก่า (ก่อนหมดอายุ) ผ่าน `/userdata` ของ HuskSync ไอเทมจะกลับมาพร้อมเวลาเดิม — เป็น edge case ฝั่ง admin action เท่านั้น ส่วนไอเทมที่ sync กลับมาในสถานะหมดอายุแล้ว จะโดน expire ใหม่ทันทีพร้อมบันทึก recovery ตามปกติ

## 🔌 Developer API

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
TimedItemAPI.isTimed(item)                // มี timer ไหม
TimedItemAPI.getType(item)                // "expire" | "lifetime" | "paused" | "playtime" | "held" | null
TimedItemAPI.getRemainingSeconds(item)
TimedItemAPI.setLifetime(item, 3600)      // throw IllegalArgumentException ถ้า material อยู่ใน blacklist
TimedItemAPI.setExpiryFromNow(item, 600)  // เช่นเดียวกัน
TimedItemAPI.pause(item) / resume(item) / clear(item)
```

**PlaceholderAPI** (soft-depend — ไม่มี PAPI ก็รันปกติ): `%timeditem_remaining%` (raw วินาที, alias `%timeditem_seconds%`) · `%timeditem_time%` (pretty) · `%timeditem_type%` · `%timeditem_date%` · `%timeditem_itemid%` (2.6.0+: `plugin:id` ของ item custom หรือชื่อ material) — อ้างจากไอเทมในมือหลัก

## 💾 การจัดเก็บข้อมูล

- **1.14+**: เก็บใน **PersistentDataContainer (NBT ของไอเทม)** เป็นตัวจริง — เป็น epoch millis จึงไม่กระทบ timezone; ตั้งแต่ 2.7.0 ไอเทมจับเวลาทุกชิ้นได้ **uid (UUID)** ฝังใน NBT ด้วย ใช้ติดตามไอเทมใน audit log และ `/ti find`
- **lore** ถูกเขียนพร้อมกันเสมอเพื่อแสดงผล และเป็นที่เก็บจริงบนเซิร์ฟ 1.13 / ไอเทมเก่า (v1/v2.0 ใช้ต่อได้ทันที — ครั้งถัดไปที่แก้ไขจะย้ายเข้า PDC เอง)
- **ที่เก็บข้อมูลปลั๊กอิน** (recovery / audit / สถิติ) เลือกได้ที่ `Settings.storage.type` — เปลี่ยนแล้วต้องรีสตาร์ท:
  - `flat` (default) — ไฟล์ใน `plugins/TimedItem/`: `recovery.log` · `audit.log` · `stats.yml` (เขียนบน background thread, snapshot + atomic move)
  - `sqlite` — ไฟล์เดียว `database.db` ไม่ต้องติดตั้งอะไรเพิ่ม
  - `mysql` — MariaDB/MySQL ผ่าน connection pool (HikariCP) เหมาะกับ network หลายเซิร์ฟแชร์ฐานข้อมูลเดียว
  - `mongodb` — ต่อด้วย connection string มาตรฐาน
  - บูตครั้งแรกด้วยฐานข้อมูล จะ import `recovery.log` เดิมเข้าฐานข้อมูลให้เอง (ไฟล์เดิมเปลี่ยนชื่อเป็น `recovery.log.imported` — ข้อมูลไม่หาย); ต่อ DB ไม่สำเร็จ = กลับไปใช้ไฟล์อัตโนมัติพร้อม warn
- **กู้คืนได้** — ทุกไอเทมที่หมดอายุถูกบันทึกไว้ก่อนลบ เปิดด้วย `/ti restore` เพื่อคืนแบบคลิกเดียว; ข้อมูล migrate เป็นฟอร์แมตของเซิร์ฟเวอร์ปัจจุบันเองตอนโหลด จึงย้ายเวอร์ชัน MC ได้

## 🍃 Folia & ความเข้ากันได้

| รองรับ | รายละเอียด |
|---|---|
| Minecraft **1.13 → 26.3+ (ล่าสุด)** | jar เดียว ใช้ได้ทุกเวอร์ชันในช่วงนี้ |
| Spigot / Paper / **Purpur** / Pufferfish / Leaf และ forks อื่น ๆ | ทุก fork ที่ใช้ Bukkit API |
| **Folia** | ✅ ตั้งแต่ 2.4.0 — expire ของผู้เล่นรันบน region thread ของเจ้าของ ถูกต้องตาม threading model (ground expiry ครบตั้งแต่ 2.4.1 ผ่าน per-entity tracking) |
| Java 8 – 25 (ฝั่งเซิร์ฟเวอร์) | compile เป็น Java 8 bytecode |

> **บน Folia:** `scan_world_items` ใช้ per-entity tracking แทนการเดินสแกนทุก world — ไอเทมที่หล่น/ของในเฟรม/บน stand ที่มี timer จะถูกตั้ง repeating check บน entity scheduler ของมันเอง (auto-retire เมื่อของหาย) ข้อจำกัดเดียว: การหมดอายุบนพื้นบน Folia ไม่มีเสียง/particle ให้ผู้เล่นใกล้เคียง (อ่านตำแหน่งข้าม region ไม่ปลอดภัย) — recovery, event และ `expiry_commands` ทำงานปกติ

## ⬆️ Migration

- **ไอเทม v1/v2.0/v2.1 ยังทำงาน** (อ่านจาก lore อัตโนมัติ)
- **config เก่าอัปเกรดอัตโนมัติ** (key ขาดเติม default ให้) — `warning.seconds` ยังใช้ได้ถ้าไม่ตั้ง `stages`
- **ข้อความที่เคยแก้ใน config.yml** จะถูกย้ายเข้าไฟล์ภาษาอัตโนมัติครั้งแรกหลังอัปเกรดเป็น 2.4.0

| เวอร์ชัน | สรุป |
|---|---|
| **v3.2.0** | `/ti reload` ทำงานแบบ async — โหลด/parse config + ไฟล์ภาษาบน async thread แล้วสลับ snapshot ใหม่บน main thread (I/O ไม่แตะ tick loop; โหลดไม่ผ่าน = คง config เดิม) · refactor ภายในแบบไม่เปลี่ยนพฤติกรรม: แบ่ง PluginConfig (immutable snapshot)/TimedItemCommand (subcommand แยกคลาส)/ExpiryListener (renewal/grace แยกคลาส) · drop-in ทั้งเวอร์ชัน (ไฟล์แปลภาษาที่แก้มือควรเพิ่ม `Messages.reload_in_progress` + `Messages.reload_failed` มี default ครอบให้) |
| **v3.1.4** | ประสิทธิภาพล้วน (drop-in) — แคช reflection ครั้งเดียวใน hot path ทั้ง 6 จุด (identify hooks / auto-timer / particle) · FlatFile เขียนไฟล์แบบ write-behind เร็วสุด 1 ครั้ง/วินาที + `stats.yml` atomic (**ข้อแลกเปลี่ยน**: crash hard อาจเสียข้อมูล recovery/audit/stats วินาทีสุดท้าย — ปิดเซิร์ฟปกติไม่เสีย) · SQL/Mongo trim ตาราง audit ทุก 64 inserts แทนทุก insert · NBT encode ของ recovery ย้ายไป background thread |
| **v3.1.3** | แก้บั๊กล้วน — anti-hide/container open-scan ใช้งานได้บน 1.21+ (`InventoryView` class→interface) · visual stage ไม่พังเมื่อ PDC tag เสีย · write-back สี stage/นับถอยหลังไม่หายบนแพลตฟอร์มที่ให้สำเนา ItemStack · ปิด leak (`lastNotice`, webhook flush ตอนปิด, UpdateChecker socket) และแก้ race ของ StorageWriter ตอน disable |
| **v3.1.2** | HuskSync compatibility — re-scan inventory ทันทีหลัง sync ข้ามเซิร์ฟ (`BukkitSyncCompleteEvent`) ปิดช่อง join scan วิ่งก่อน sync เสร็จ · timer ทุกชนิดอยู่รอดข้ามเซิร์ฟเองจาก raw NBT · ปิดได้ `Hooks.husksync: false` |
| **v3.1.1** | zShop integration — บล็อกขายผ่าน `ZShopSellAllEvent` (`/zshop sellall` + sell GUI) · zAuctionHouse V4 — รองรับ `AuctionPreSellEvent` คู่กับ V3 candidates เดิม |
| **v3.1.0** | สแกนเข้า shulker ซ้อนได้ 1 ชั้น (container scan · `/ti find` · anti-hide) · restore ข้ามเซิร์ฟ network mode (`/ti restore <player>` อ่านจาก DB ทุกเซิร์ฟ) · auto-return ให้เจ้าของ (`/ti give` ประทับ owner ใน NBT) · บล็อกขาย timed item บน QuickShop / zAuctionHouse / CrazyAuctions |
| **v3.0.0** | Visual decay stages — ชื่อไอเทมเปลี่ยนสี + glow ตามเวลาที่เหลือ · hologram countdown เหนือไอเทมบนพื้น (1.19.4+, ปิดบน Folia) · live lore countdown ทุกวินาที (1.14+) · Admin GUI `/ti admin` · `/ti check <player>` · container scan (`open`/`periodic`/`both`) + anti-hide กันยัดของใกล้หมดอายุลงตู้ |
| **v2.9.0** | ประเภท timer ใหม่ 2 แบบ — **playtime** นับเฉพาะตอนเจ้าของออนไลน์ (`/ti set playtime 10h`) และ **held** นับเฉพาะตอนถือ/สวมใส่ (`/ti set held 30m`) · give tokens `playtime:`/`held:` · renewal token ต่ออายุได้ทุกประเภท |
| **v2.8.0** | Expiry transformation — ของหมดอายุกลายเป็นอีกชิ้น (`transform:<key>`, ผลลัพธ์ vanilla หรือ custom ก็ได้, API `getReplacement`) · grace period ช่วงสุดท้ายใช้ของไม่ได้ · renewal token ต่ออายุ · restore reminder ตอน join · Discord webhook แบบ embed |
| **v2.7.0** | ฐานข้อมูล 4 แบบ (`flat`/`sqlite`/`mysql`/`mongodb`) พร้อม import recovery.log อัตโนมัติ · item UID ใน NBT · `/ti find` ค้นทั้งเซิร์ฟ · `/ti stats` สถิติรายวัน · `/ti audit` ประวัติการกระทำ |
| **v2.6.0** | Integration 9 ปลั๊กอิน — `/ti give <player> <plugin:id> <time>` (MMOItems/ItemsAdder/Oraxen/Nexo/ExecutableItems/Slimefun/EcoItems/CrazyCrates) · auto-timer ร้านค้า Shopkeepers + crate CrazyCrates (`Hooks.*`) · `{item}` ใน webhook + `%timeditem_itemid%` โชว์ของ custom จริง |
| **v2.5.0** | ระบบ debug & diagnostics — `/ti debug on\|off\|export` · จับ log 500 แถว · verbose `[Debug]` log · รายงานบั๊ก (ไม่มีข้อมูล webhook URL/item/player) |
| **v2.4.1** | Ground expiry บน Folia สมบูรณ์ (per-entity scheduler tracking — spawn/chunk load/interact) |
| **v2.4.0** | Folia support · Discord webhook ในตัว · multi-language (`Settings.language` + `lang/`) · recovery.log เขียนบน background thread · PDC reflection cache · GUI ตามภาษา |
| **v2.3.1** | รวม expire pipeline เดียว (`ExpiryService`) · recovery.log ข้ามเวอร์ชัน MC ได้ (format tag + auto-migrate) · `ignore_creative` · tab hint `1d12h`/`30d` |
| **v2.3.0** | Tab completion · `/ti give` · recovery + restore GUI · warning stages · material blacklist · update checker |
| **v2.2.0** | PDC storage · GUI menu · event API + facade · PlaceholderAPI · หมดอายุบนพื้น/เฟรม · unit tests · MIT License |
| **v2.1.0** | bStats · CI · pause/resume · เตือนล่วงหน้า · particle/เสียง/actionbar · expiry_commands |
| **v2.0.0** | Refactor ใหม่ทั้งหมดจากปลั๊กอินต้นฉบับปี 2019 — jar เดียว Java 8 bytecode, รองรับ MC 1.13 ถึงปัจจุบัน, compat ผ่าน reflection shims |

ดูรายละเอียดทั้งหมดได้ที่ [CHANGELOG.md](CHANGELOG.md)

## ❓ FAQ

**ถอนการติดตั้ง TimedItem แล้ว ของที่ติด timer จะหายไหม?**

ไม่หาย — timer เก็บอยู่ใน NBT/lore ของไอเทมเอง ถอนปลั๊กอินแล้วของกลายเป็นของปกติ ติดตั้งกลับมาเมื่อไหร่ตัวจับเวลานับต่อจากเดิมทันที (เก็บเป็นเวลาจริง ไม่ผูกกับ uptime ของเซิร์ฟเวอร์)

**เซิร์ฟเวอร์ล่ม / รีสตาร์ทบ่อย เวลาจะคลาดเคลื่อนไหม?**

ไม่ — ค่า expiry เป็น epoch millis ตามนาฬิกาจริง รีสตาร์ทกี่รอบ / เปลี่ยน timezone / ย้ายเครื่อง เวลาก็ยังตรง

**ใช้กับของจากปลั๊กอินอื่นได้ไหม?**

ได้ — 9 เจ้าถูกจำตัวตนอัตโนมัติ (MMOItems, ItemsAdder, Oraxen, Nexo, ExecutableItems, Slimefun, EcoItems, CrazyCrates, Shopkeepers — ดู [Integration](#-integration-ปลั๊กอินอื่น)) ส่วนปลั๊กอินที่สร้าง ItemStack vanilla ปกติ (เช่น item ของ MythicMobs, ของจาก /give) ใช้ได้ทันทีอยู่แล้วเพราะ TimedItem ทำงานกับทุก ItemStack

**ผู้เล่นลบ/แก้ lore timer ออกเองได้ไหม?**

ทำไม่ได้ด้วยวิธีปกติในเกม (ทั่ง/ตู้เสบียงแก้ lore ไม่ได้) — และแม้ lore โดนแตะ บน 1.14+ ค่าจริงเก็บใน PersistentDataContainer ของ item ที่ผู้เล่นแตะไม่ถึง ย้ายของข้าม chest/ender chest/โยนพื้น/เข้า hopper timer ก็ไม่หาย

**ของหมดอายุไปแล้ว ผู้เล่นขอคืนได้ไหม?**

ได้ถ้าแอดมินยินยิม — ทุกของที่หมดอายุถูกสแนปช็อตลง `recovery.log` ก่อนลบเสมอ (เก็บ NBT ครบทั้งก้อน รวมของ custom จาก plugin อื่น) แอดมินใช้ `/ti restore` คลิกคืนให้ได้ภายใน `recovery.keep_days`

**เซิร์ฟเวอร์เก่า (1.13) ใช้ได้จริงเหรอ?**

จริง — compile เป็น Java 8 bytecode ใช้ได้ตั้งแต่ 1.13 (บน 1.13 เก็บใน lore แทน PDC) ถ้าวันหนึ่งอัปเกรดเซิร์ฟเวอร์ item เดิมจะถูกย้ายเข้า PDC เองตอนแตะครั้งแรก ไม่ต้อง migrate มือ

## 🛠 เทคนิคสำหรับผู้ดูแลโค้ด

- compile กับ `spigot-api 1.13.2` + `api-version: '1.13'` → jar เดียว 1.13 → 26.x, ไม่แตะ internals
- `PdcCompat` / `HealthCompat` / `ParticleCompat` resolve ผ่าน reflection (รองรับ enum→interface ทุกจุด: Sound, Attribute, Particle) — Methods ถูก cache ตอน init
- `hook/` — integration กับ plugin อื่น **ไม่คอมไพล์เทียบ API ภายนอกเลย**: adapter ทุกตัว resolve ผ่าน `HookReflection` (probe ชื่อ class หลาย candidate ต่อเวอร์ชัน), hook class โหลดเฉพาะเมื่อ plugin เป้าหมาย enabled (pattern เดียวกับ PlaceholderAPI) และ event hooks (Shopkeepers/CrazyCrates) ลงทะเบียนผ่าน `EventExecutor` ดิบ + `HandlerList` เพื่อไม่ผูกกับ event class ของปลั๊กอินอื่นตอน compile — API เปลี่ยน = hook ปิดตัวเองเงียบๆ ไม่มีวัน crash
- **Folia:** ตรวจจับผ่าน `RegionizedServer` class — scheduler ทั้งหมดผ่าน `SchedulerCompat` (global region / entity / async scheduler ด้วย reflection); periodic scan กับ actionbar enqueue ต่อผู้เล่นด้วย entity scheduler; ground expiry ใช้ `FoliaGroundExpiry` — entity แต่ละตัวถือ repeating check บน scheduler ของตัวเอง (tracked จาก spawn/chunk load/interact, self-cancel เมื่อไม่มี timed item); recovery store ใช้ lock + snapshot จึงไหวกับหลาย region thread
- ทุกกรณีหมดอายุ (ผู้เล่น / พื้น-เฟรม-stand) ผ่าน `ExpiryService` เส้นเดียวกัน — event, recovery, เอฟเฟกต์ และ `expiry_commands` สม่ำเสมอทั้งสองฝั่ง
- `TimeLore.analyze` อ่านข้อมูลครั้งเดียวต่อ stack (TimerInfo) — hot path ไม่ copy ItemMeta ซ้ำ
- recovery.log: line format อยู่ที่ `RecoveryFormat` (pure, unit tested) — decode อยู่ที่ `ItemSerializer.encode/fromBase64`
- ข้อความ/GUI โหลดจาก `lang/messages_<language>.yml` (self-heal จาก default, `LanguageFilesTest` กัน key หาย/หน่วยไม่ตรงกัน)
- bStats shaded + relocated (`me.albert.timeditem.libs.bstats`); tests: JUnit 5 ผ่าน surefire

## 🧪 การทดสอบ

ทดสอบ 3 ชั้น (รายละเอียดเต็มที่ [testing/README.md](testing/README.md)):

1. **Unit tests** — `mvn test`: ตรรกะ pure 18 ไฟล์ (DurationParser, TimeLore, HookToken, HookReflection, VisualStages, TrackedItems, SchedulerCompat, storage, language files ฯลฯ)
2. **In-game harness** — ปลั๊กอิน `TimedItemTestHarness` รัน 10 scenarios บน server จริงจาก console (`/titest run all`): boot, lore roundtrip + PDC, API guard rails, ground expiry, transform, container scan, holograms, Folia gates, hooks roundtrip, auto-timer — พ่อผลเป็น `[TITEST] PASS/FAIL` ลง log
3. **Test matrix** — `bash testing/run-matrix.sh all`: บูต server จริง **4 แพลตฟอร์ม** (Paper 1.21.x / Folia / Purpur / Paper 1.20.x) headless พร้อม **FakeHooks stubs ครบทุก integration** (MythicLib, MMOItems, ItemsAdder, Oraxen, Nexo, ExecutableItems, Slimefun, EcoItems, Shopkeepers, CrazyCrates) รัน harness ทุก lane แล้วสรุปตารางผ่าน/พัง — lane `hooks-lane` รันซ้ำกับ jar ปลั๊กอินจริงที่วางใน `testing/real-hooks/` ได้

---

<div align="center">

**TimedItem 2.x** — refactor ใหม่ทั้งหมดจากปลั๊กอินต้นฉบับปี 2019 เพื่อรองรับ Minecraft ยุคปัจจุบัน

Licensed under the [MIT License](LICENSE) · Made by [BikeXzoZiz](https://github.com/BikeXzoZiz)

</div>
