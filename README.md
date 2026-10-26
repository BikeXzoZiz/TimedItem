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
| ⏱️ | **Timer 3 แบบ** | หมดอายุตามวัน (expire) · อายุการใช้งานเริ่มนับเมื่อใช้ครั้งแรก (lifetime) · หยุดเวลาชั่วคราว (pause) |
| 💾 | **เก็บใน NBT** | ตัวจับเวลาอยู่ใน PersistentDataContainer ของไอเทม — แก้ lore ยังไง timer ก็ไม่หาย |
| 🎨 | **GUI ครบ** | `/ti menu` ตั้งเวลา/หยุด-เดินเวลา · `/ti restore` กู้คืนไอเทมที่หมดอายุแล้ว |
| 🗑️ | **หมดอายุทุกที่** | ในตัว ในมือ บนพื้น ใน item frame บน armor stand — ตรวจแบบ event และเป็นรอบ |
| 🔔 | **เตือนล่วงหน้า** | ข้อความ + เสียง + actionbar countdown หลายระยะ เช่น 10 นาที / 1 นาที / 10 วินาที |
| 💬 | **Discord webhook ในตัว** | แจ้ง Discord ทุกครั้งที่ไอเทมหมดอายุ ไม่ต้องติดตั้งปลั๊กอินอื่น |
| 🌐 | **หลายภาษา** | อังกฤษ / ไทย พร้อมใช้ หรือวางไฟล์ภาษาของคุณเอง — GUI ก็เปลี่ยนตาม |
| 🧩 | **Integration 9 ปลั๊กอิน** | MMOItems · ItemsAdder · Oraxen · Nexo · ExecutableItems · Slimefun · EcoItems · Shopkeepers · CrazyCrates — สร้างของ custom เป็น timed item ได้โดยตรง (ตั้งแต่ 2.6.0) |
| 🍃 | **รองรับ Folia** | ทำงานถูกต้องบน threading แบบ region (ตั้งแต่ 2.4.0) |
| 🔌 | **Developer API** | Events, Facade, PlaceholderAPI — ผสานกับปลั๊กอินของคุณได้ทันที |
| ♻️ | **กู้คืนได้** | ทุกไอเทมที่หมดอายุถูกบันทึกไว้ แอดมินคืนให้ผู้เล่นได้ด้วยคลิกเดียว |

## 🚀 เริ่มใช้ใน 1 นาที

1. ดาวน์โหลด `TimedItem-2.6.0.jar` จาก [Releases](https://github.com/BikeXzoZiz/TimedItem/releases) แล้ววางในโฟลเดอร์ `plugins/`
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
| `/ti set <เวลา>` | ประทับเวลาบนไอเทมที่ถือ เช่น `/ti set 1d12h30m` หรือแบบเก่า `/ti set 120 s` |
| `/ti add <เวลา>` | ต่อเวลา — ใช้ได้ทั้งไอเทมที่เริ่มนับแล้ว, ยังไม่เริ่ม และ paused |
| `/ti give <ผู้เล่น> <เวลา> [จำนวน]` | หยิบไอเทมในมือ ตั้งเวลา แล้วส่งให้ผู้เล่นเป้าหมายทันที (default 1 ชิ้น, หักจากมือผู้ส่ง) |
| `/ti give <ผู้เล่น> <plugin:id> <เวลา> [จำนวน]` | สร้างของ custom จากปลั๊กอินที่ integrate เป็น timed item ทันที เช่น `/ti give Steve mmoitems:SWORD:CUTLASS 30d` — console ใช้ได้ (ดู [Integration](#-integration-ปลั๊กอินอื่น)) |
| `/ti pause` | หยุดเวลาชั่วคราว — ไอเทมไม่หมดอายุระหว่างหยุด |
| `/ti resume` | เดินเวลาต่อจากเดิม |
| `/ti remove` | ถอด timer ออกจากไอเทมที่ถือ |
| `/ti info` | ดูเวลาที่เหลือ / lifetime / เวลาที่หยุดไว้ |
| `/ti menu` | เปิด GUI — preset เวลา / pause-resume / remove / สถานะสด |
| `/ti restore [หน้า]` | เปิด GUI กู้คืนไอเทมที่หมดอายุไปแล้วล่าสุด — คลิกเพื่อรับคืน |
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
<summary>📄 <b>ตัวอย่าง config.yml ครบทุก key (ค่า default ล่าสุด)</b> — คลิกขยาย</summary>

```yaml
# TimedItem config.yml — ทุก key มี default ในตัว: config ขาด/เก่าแค่ไหน
# ก็ self-heal เติมให้เอง ไม่มีวันพัง
# ข้อความทั้งหมด (Messages/DateFormat/Gui) อยู่ที่ lang/messages_<ภาษา>.yml
config-version: 8

Settings:
  language: en                         # en / th / ไฟล์ lang/messages_<x>.yml ของคุณ
  start_date_calculate_on_click: true  # true = lifetime เริ่มนับเมื่อใช้ครั้งแรก · false = เริ่มทันทีตอน set
  reset_player_health_if_armor: true   # เกราะที่เพิ่ม max health ถูกรีเซ็ตเป็น 20 เมื่อหมดอายุ (กัน exploit)
  metrics: true                        # bStats anonymous statistics
  debug: false                         # verbose [Debug] log — สลับได้ทันที: /ti debug on|off

  # การตรวจของหมดอายุเป็นรอบ (นอกเหนือจาก event เช่น คลิก/ใช้/หยิบ)
  expire_check:
    enabled: true
    interval_seconds: 10               # สแกน inventory ทุกกี่วินาที
    scan_world_items: false            # สแกนของบนพื้น/ใน item frame/บน armor stand (แพง — เปิดเมื่อจำเป็น)
    ignore_creative: true              # ของ timed ในมือ creative ไม่หมดอายุและไม่มี countdown

  # แจ้ง Discord ทุกครั้งที่ของหมดอายุ (webhook ในตัว ไม่ต้องติดตั้งอะไรเพิ่ม)
  discord_webhook:
    enabled: false
    url: ""
    username: "TimedItem"
    message: "{player}'s timed item expired: {item} ({world})"

  # เตือนผู้เล่นล่วงหน้าหลายระยะ — แต่ละค่า = ครั้งเดียวตอนเหลือเท่านั้น
  warning:
    enabled: true
    seconds: 60                        # fallback ถ้า stages ว่าง
    stages: [60]                       # แนะนำ: [600, 60, 10] = เหลือ 10 นาที / 1 นาที / 10 วิ
    sound:
      name: block.note_block.pling
      volume: 1.0
      pitch: 1.5

  particles:
    enabled: true
    name: FLAME                        # ชื่อ particle — ใส่ผิด fallback เป็น FLAME
    count: 30

  expiry_commands: []                  # คำสั่ง console ตอนของหมดอายุ เช่น "discord message timed {player}'s item expired!"

  actionbar:
    enabled: true
    format: "&e⌛ {time}"              # countdown บน actionbar ตอนถือของ timed

  sound:
    enabled: true
    name: entity.blaze.death           # เสียงตอนของหมดอายุ
    volume: 10.0
    pitch: 1.2

  update_check:
    enabled: true
    repository: "BikeXzoZiz/TimedItem" # ที่ไว้เช็คเวอร์ชันใหม่ (GitHub repo)

  # กู้คืนของที่หมดอายุ (/ti restore)
  recovery:
    enabled: true
    max_items: 500                     # เก็บได้สูงสุดกี่รายการ
    keep_days: 7                       # เก็บไว้กี่วัน

  blacklist:
    materials: []                      # เช่น [TOTEM_OF_UNDYING] — material ที่ห้ามมี timer เด็ดขาด

# Integration กับปลั๊กอินอื่น (2.6.0+) — ตรวจจับอัตโนมัติ, ปิดรายตัวได้
Hooks:
  mmoitems: true
  itemsadder: true
  oraxen: true
  nexo: true
  executableitems: true
  slimefun: true
  ecoitems: true
  crazycrates:
    enabled: true
    auto_timer: ""                     # เช่น "7d" = ของจาก crate หมดอายุอัตโนมัติ ("" = ปิด)
  shopkeepers:
    enabled: true
    auto_timer: ""                     # เช่น "30d" = ของที่ซื้อจากร้านหมดอายุอัตโนมัติ ("" = ปิด)
```

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
```

</details>

> **หมายเหตุ**
> - Hook ทำงานผ่าน **reflection ทั้งหมด** — ไม่เพิ่ม dependency, jar เดียวเหมือนเดิม; เวอร์ชัน API ที่ออกแบบกับ: MMOItems 6.10.x · ItemsAdder 4.x · Oraxen 2.x · Nexo 1.x · ExecutableItems ล่าสุด (SCore) · Slimefun RC-38 · Shopkeepers 2.20+ (ต้อง ≥ 2.19 สำหรับ auto-timer) · CrazyCrates ล่าสุด ถ้าเวอร์ชัน API ไม่ตรง hook ตัวนั้นจะปิดตัวเองอย่างปลอดภัย (ดูได้ที่ `/ti debug export`)
> - **MythicMobs ยังไม่รองรับ** — item ของ MythicMobs เป็น vanilla stack ไม่มี tag ระบุตัวตน จึงไม่มีทางจำ/สร้างที่เชื่อถือได้ (ใช้ `/ti give` ธรรมดากับของที่สร้างไว้แทน)
> - ของ custom ที่ติด timer จะถูกเก็บ NBT เต็มตอนหมดอายุ (`recovery.log`) — กู้คืนแล้วของยังเป็นของปลั๊กอินเดิมครบทุกอย่าง

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
TimedItemAPI.getType(item)                // "expire" | "lifetime" | "paused" | null
TimedItemAPI.getRemainingSeconds(item)
TimedItemAPI.setLifetime(item, 3600)      // throw IllegalArgumentException ถ้า material อยู่ใน blacklist
TimedItemAPI.setExpiryFromNow(item, 600)  // เช่นเดียวกัน
TimedItemAPI.pause(item) / resume(item) / clear(item)
```

**PlaceholderAPI** (soft-depend — ไม่มี PAPI ก็รันปกติ): `%timeditem_remaining%` (raw วินาที) · `%timeditem_time%` (pretty) · `%timeditem_type%` · `%timeditem_date%` · `%timeditem_itemid%` (2.6.0+: `plugin:id` ของ item custom หรือชื่อ material) — อ้างจากไอเทมในมือหลัก

## 💾 การจัดเก็บข้อมูล

- **1.14+**: เก็บใน **PersistentDataContainer (NBT ของไอเทม)** เป็นตัวจริง — เป็น epoch millis จึงไม่กระทบ timezone
- **lore** ถูกเขียนพร้อมกันเสมอเพื่อแสดงผล และเป็นที่เก็บจริงบนเซิร์ฟ 1.13 / ไอเทมเก่า (v1/v2.0 ใช้ต่อได้ทันที — ครั้งถัดไปที่แก้ไขจะย้ายเข้า PDC เอง)
- **recovery.log** — ทุกไอเทมที่หมดอายุถูกบันทึกที่ `plugins/TimedItem/recovery.log` (เขียนบน background thread, snapshot + atomic move) เปิดด้วย `/ti restore` เพื่อคืนของแบบคลิกเดียว; ไฟล์ migrate เป็นฟอร์แมตของเซิร์ฟเวอร์ปัจจุบันเองตอนโหลด จึงย้ายเวอร์ชัน MC ได้

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
| **v2.6.0** | Integration 9 ปลั๊กอิน — `/ti give <player> <plugin:id> <time>` (MMOItems/ItemsAdder/Oraxen/Nexo/ExecutableItems/Slimefun/EcoItems/CrazyCrates) · auto-timer ร้านค้า Shopkeepers + crate CrazyCrates (`Hooks.*`) · `{item}` ใน webhook + `%timeditem_itemid%` โชว์ของ custom จริง |
| **v2.5.0** | ระบบ debug & diagnostics — `/ti debug on\|off\|export` · จับ log 500 แถว · verbose `[Debug]` log · รายงานบั๊ก (ไม่มีข้อมูล webhook URL/item/player) |
| **v2.4.1** | Ground expiry บน Folia สมบูรณ์ (per-entity scheduler tracking — spawn/chunk load/interact) |
| **v2.4.0** | Folia support · Discord webhook ในตัว · multi-language (`Settings.language` + `lang/`) · recovery.log เขียนบน background thread · PDC reflection cache · GUI ตามภาษา |
| **v2.3.1** | รวม expire pipeline เดียว (`ExpiryService`) · recovery.log ข้ามเวอร์ชัน MC ได้ (format tag + auto-migrate) · `ignore_creative` · tab hint `1d12h`/`30d` |
| **v2.3.0** | Tab completion · `/ti give` · recovery + restore GUI · warning stages · material blacklist · update checker |
| **v2.2.0** | PDC storage · GUI menu · event API + facade · PlaceholderAPI · หมดอายุบนพื้น/เฟรม · unit tests · MIT License |
| **v2.1.0** | bStats · CI · pause/resume · เตือนล่วงหน้า · particle/เสียง/actionbar · expiry_commands |

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

---

<div align="center">

**TimedItem 2.x** — refactor ใหม่ทั้งหมดจากปลั๊กอินต้นฉบับปี 2019 เพื่อรองรับ Minecraft ยุคปัจจุบัน

Licensed under the [MIT License](LICENSE) · Made by [BikeXzoZiz](https://github.com/BikeXzoZiz)

</div>
