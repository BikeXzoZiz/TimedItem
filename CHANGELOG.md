# Changelog

รูปแบบอ้างอิง [Keep a Changelog](https://keepachangelog.com/) — รายละเอียดการใช้งานดูที่ [README.md](README.md)

## [Unreleased]

### Added
- **ระบบทดสอบครบชุด 3 ชั้น** (โฟลเดอร์ `testing/` + `testing/README.md`) — unit tests เพิ่ม 5 ไฟล์ (VisualStages, TrackedItems, HookReflection, SchedulerCompat + ขยาย HookToken) รวม 18 ไฟล์ · ปลั๊กอินทดสอบในเกม `TimedItemTestHarness` รัน 10 scenarios จาก console แล้วพ่อ `[TITEST] PASS/FAIL` ลง log (boot, lore roundtrip + PDC, API guard rails, ground expiry, transform, container scan, holograms, Folia gates, hooks roundtrip, auto-timer) · **test matrix `testing/run-matrix.sh`** บูต server จริง 4 แพลตฟอร์ม (Paper 1.21.x / Folia / Purpur / Paper 1.20.x) headless พร้อม **FakeHooks stubs จำลอง API ทุก integration** (MythicLib, MMOItems, ItemsAdder, Oraxen, Nexo, ExecutableItems, Slimefun, EcoItems, Shopkeepers, CrazyCrates) แล้วรัน harness ทุก lane สรุปตารางผ่าน/พัง · lane `hooks-lane` รันซ้ำกับ jar ปลั๊กอินจริงจาก `testing/real-hooks/`

## [3.1.4] - 2026-10-27

### Changed (ประสิทธิภาพ)
- **แคช reflection ครั้งเดียวใน hot path ทั้ง 6 จุด** — จุดที่ถูกเรียกถี่ที่สุดของทุก hook เคยสแกน `getMethods()` ทั้งชุดใหม่ทุกครั้งที่ถูกเรียก: identify ของ ItemsAdder/Slimefun (`getId`/`getNamespace`), **EcoItems ที่เดิน registry ทั้งชุดต่อ 1 identify (จุดแพงสุด)**, auto-timer ของ Shopkeepers/CrazyCrates ที่สแกน 2-3 ครั้งต่อ event, และ `ParticleCompat` ที่ `Class.forName` + `getField` ต่อ particle ที่ปล่อยทุกครั้งที่ expire — ตอนนี้ resolve ครั้งเดียว (ตอน construct / ตอน event แรก / แคชต่อชื่อพร้อม negative cache สำหรับชื่อที่ผิด) พฤติกรรม degrade เมื่อ hook ไม่ตรงเวอร์ชันเหมือนเดิมครบ
- **FlatFile storage เปลี่ยนเป็น write-behind** — เดิมทุกไอเทมที่ expire เขียน `recovery.log` + `stats.yml` ใหม่ทั้งไฟล์ และทุก audit event เขียน `audit.log` ใหม่ทั้งไฟล์ — ตอนนี้การเปลี่ยนแปลง mark dirty ในหน่วยความจำก่อน แล้ว flush ลงดิสก์เร็วสุด 1 ครั้ง/วินาที (+ flush ก่อนปิดปลั๊กอิน) ตัด disk churn ตอน expire เป็นชุดใหญ่; ข้อแลกเปลี่ยน: ถ้าเซิร์ฟเวอร์ crash แบบ hard อาจเสียข้อมูลวินาทีสุดท้าย (ปิดเซิร์ฟแบบปกติไม่เสีย); `stats.yml` เขียนแบบ atomic (temp + rename) เหมือนไฟล์ `.log` แล้ว ปิดช่องไฟล์ torn ตอนตายกลางเขียน; `/ti reload` จะ flush ของค้างก่อนโหลดใหม่ ไม่มีข้อมูล in-memory หาย
- **SQL/MongoDB: trim ตาราง audit ทุก 64 inserts แทนทุก insert** — เดิม INSERT หนึ่งครั้งตามด้วย `DELETE` เต็มตาราง (SQL) หรือ `countDocuments` + border query + `deleteMany` สูงสุด 3 round trips (Mongo) ทุกครั้ง — ตอนนี้ครอบด้วย counter ทุก 64 inserts ตารางเกิน cap ได้ไม่เกิน 64 แถวชั่วคราว ซึ่ง `/ti audit` ไม่มีทางเห็น (อ่าน newest-first พร้อม LIMIT)
- **NBT encode ของ recovery ย้ายไป writer thread** — ตอนไอเทมหมดอายุ serialize ไอเทมเป็น NBT (งานหนักสุดใน pipeline โดยเฉพาะ shulker) เคยรันบน main/region thread — ตอนนี้ expiry thread แค่ clone สำเนาจาง ๆ แล้ว encode + บันทึกบน background thread (entry โผล่ใน restore GUI ช้าลงไม่กี่มิลลิวินาที)

### Technical
- `Storage` เพิ่ม `flush()` (default no-op — SQL/Mongo เขียนทันทีอยู่แล้ว); `StorageWriter` เพิ่ม flusher thread ส่ง flush เข้าคิว writer ได้มากสุด 1 ครั้ง/วินาที (CAS guard — burst ทั้งชุดรวมเป็น flush เดียว)
- unit tests 116 ชุดผ่าน (เพิ่ม ParticleCompat resolve/cache, FlatFile flush semantics + close-flush, SQLite audit trim amortize) · matrix paper lane ผ่าน

## [3.1.3] - 2026-10-27

### Fixed
- **รองรับเซิร์ฟเวอร์ 1.21+ ที่ `InventoryView` กลายเป็น interface** — anti-hide และ container open-scan เรียก `getTopInventory()/getBottomInventory()` ตรง ๆ โดยคอมไพล์กับ API 1.13 (ยุค class) → บน 1.21+ ที่ API เปลี่ยนเป็น interface จะโดน `IncompatibleClassChangeError` ทันทีที่ event เกิด (ฟีเจอร์ทั้งสองใช้ไม่ได้บนเซิร์ฟใหม่) — เปลี่ยนเป็น reflective accessor แคชครั้งเดียว รองรับทั้งสองยุค
- **Visual decay stages ไม่พังเมื่อ tag เสีย** — `timeditem:stage` ใน PDC ที่ถูกแก้มือ/เสียหาย (`Integer.parseInt` ไม่ผ่าน) เคยโยน NumberFormatException ใส่รอบสแกน — ตอนนี้จับได้ ลบ tag ทิ้งแล้ว re-stage ใหม่เอง + log verbose
- **เขียนกลับ inventory ทุกครั้งที่ stage/lifetime/นับถอยหลังเปลี่ยน** — write-back เดิมผูกกับเคส expire/lifetime เท่านั้น และเคส playtime/held เขียนกลับสำเนาเก่า (`inv.getItem(slot)`) — บนแพลตฟอร์มที่ให้สำเนาของ ItemStack สี stage/glow กับเวลาที่นับแล้วเคยหายเงียบ ๆ
- **StorageWriter ไม่โยน RejectedExecutionException ตอนปิดเซิร์ฟ** — race ระหว่าง flag check กับ `shutdown()` ตอน disable (expiry บน region thread แตะคิวพอดีจังหวะ)
- **ล้าง state ค้างตอนผู้เล่นออก/ปิดปลั๊กอิน** — cooldown แจ้งเตือน (`lastNotice`) ไม่เคยล้างตอน quit (รั่ว 1 แถว/คน); filter map ของ `/ti admin` เป็น dead state เขียนแต่ไม่เคยอ่าน — ลบทิ้ง
- **Folia: period ของ ground-expiry tracking ข้าม thread แบบ non-volatile** — เขียนจาก reload thread อ่านบน region threads ตอน startTracking มองไม่เห็นค่าใหม่หลัง reload ได้
- **Actionbar ดับเงียบเมื่อ API fail** — ตอน bungee-chat actionbar โยน error ครั้งแรก ปลั๊กอินปิดฟีเจอร์ไปเงียบ ๆ ไม่ log อะไรเลย — ตอนนี้ flag เป็น volatile + log WARNING ครั้งเดียว และรวม flag ไว้ที่ `ActionbarCompat` ที่เดียว (ลบ flag ซ้ำใน `ActionbarTask` ที่ไม่มีวันถูกเซ็ต)
- **Discord webhook flush ก่อนปิด** — `close()` เดิมแค่ `shutdown()` ไม่รอ คิวแจ้งเตือนที่ค้างตอน stop เซิร์ฟอาจหาย (ตอนนี้ `awaitTermination` 10s เหมือน StorageWriter)
- **UpdateChecker ปิด connection ทุก path** — เดิม early-return ตอน status ไม่ใช่ 200 กับเคส exception ปล่อย reader/connection ไม่ปิด (รั่ว socket จนกว่า GC)
- **CrazyCrates: prize probe ไม่เจอไม่ NPE** — `prize.getClass()` เมื่อ prize เป็น null เคยกลืน NPE ลง catch เป็น verbose log เท่านั้น
- **test matrix (`testing/run-matrix.sh`) อ่านเวอร์ชันจาก pom.xml เอง** — เดิม hardcode 3.0.0 ทำให้รันเทสต์ด้วย jar เก่าโดยไม่รู้ตัว

## [3.1.2] - 2026-10-27

### Added
- **รองรับ HuskSync (WiIIiam278, 3.x/4.x)** — hook ตรวจจับอัตโนมัติ (reflection, ปิดได้ที่ `Hooks.husksync: false`): หลัง HuskSync ใส่ inventory ที่ sync มาให้ผู้เล่น (`BukkitSyncCompleteEvent` — ครอบคลุม join หลังสลับเซิร์ฟ, admin rollback, /invsee edit และ API update) จะ re-scan inventory ทันที (defer 1 tick ผ่าน SchedulerCompat จึง Folia-safe) ปิดช่องที่ join scan วิ่งก่อน sync เสร็จ (ผู้เล่นยังถูก lock อยู่) เหลือหน้าต่าง ~1 tick
  - timer ทุกชนิดอยู่รอดข้ามเซิร์ฟเองเพราะ HuskSync เก็บไอเทมเป็น raw NBT (PDC + lore ไม่หาย) — แบบ expire เป็น epoch นับถอยหลังตอนออฟไลน์ได้, playtime/held นับตอนออนไลน์บนเซิร์ฟที่ไอเทมอยู่ จึงไม่ต้องตั้งค่าอะไรฝั่ง HuskSync
  - hook นี้ passive (ไม่เคย cancel อะไร) — getter probe 3 ชั้น: `getPlayer()` บน event → `getPlayer()` บน OnlineUser → `getUuid()` + `Bukkit.getPlayer(uuid)` (เพราะ `getPlayer()` อยู่บน BukkitUser ที่เป็น subclass ของ OnlineUser ใน common)

## [3.1.1] - 2026-10-27

### Added
- **รองรับ zShop (GroupeZ)** — hook บล็อกขายตัวที่ 4: ยกเลิก `ZShopSellAllEvent` เมื่อของที่จะขาย (`/zshop sellall` และตอนปิดหน้า sell-inventory) มี timed item ปนอยู่ — event นี้เป็นตัวเดียวของ zShop ที่แบก ItemStack จริงของผู้เล่น (`ShopAction.getItemStack()`) และถูกยิงก่อนลบของทุกเส้นทาง (GUI flow คืนของที่เหลือให้ผู้เล่นเอง จึงไม่มีของหาย); ขายเดี่ยว/sell hand ไม่ต้องบล็อกเพราะ zShop จับคู่ของด้วย `isSimilar` กับของที่ config ไว้ในร้าน ซึ่ง timed item (PDC + lore countdown) ไม่มีทางตรงอยู่แล้ว บล็อกตรงนั้นจะโดนของปกติแทน; ปิดรายตัวได้ที่ `Hooks.zshop: false`

### Fixed
- **รองรับ zAuctionHouse V4** — V4 (4.x) เขียนใหม่ทั้งหมดและย้าย event ขายจาก `ZSellEvent` (V3) ไปที่ `fr.maxlego08.zauctionhouse.api.event.events.sell.AuctionPreSellEvent` — hook บล็อกขายของ 3.1.0 จับไม่เจอบน V4; เพิ่ม candidate ใหม่ลำดับแรกพร้อมเก็บ candidates V3 เดิมไว้ครบ รองรับทั้งสองเวอร์ชัน (probe ตัวไหนเจอก่อนใช้ตัวนั้น); getter probe เดิมตรงกับ V4 ทุกตัว (`getItemStack()` / `getPlayer()` / `setCancelled`) ไม่ต้องแก้ hook

## [3.1.0] - 2026-10-27

### Added
- **สแกนข้างใน shulker ซ้อน** (3.1+) — ตู้เก็บของ/การค้นหา/anti-hide มองเข้าไปใน shulker box ได้อีก 1 ชั้น (vanilla ซ้อนกันไม่ได้อยู่แล้ว):
  - ของหมดอายุใน shulker ที่อยู่ในตู้ → expire ตาม container scan ทั้งแบบ open (replacement เข้า inventory คนเปิด) และ periodic (replacement ดรอปที่ตำแหน่งตู้) — แก้ข้างในด้วย BlockStateMeta API สะอาด ไม่แตะ NBT ดิบ
  - shulker ใน inventory ผู้เล่นก็โดนตรวจตามรอบ · `/ti find` รายงานตำแหน่ง shulker ที่ข้างในมีของตรงเงื่อนไข
  - **ปิดช่องโหว่ anti-hide**: ยัด shulker ที่ข้างในมีของใกล้หมดอายุลงตู้ = ถูกบล็อกเหมือนยัดของตรงๆ
- **Restore ข้ามเซิร์ฟ (network mode)** — `Settings.restore.network: true` + ฐานข้อมูลแชร์ (mysql/mongodb): `/ti restore` อ่านไอเทมที่หมดอายุของผู้เล่นจาก **ทุกเซิร์ฟเวอร์** ใน network, `/ti restore <ผู้เล่น>` ให้แอดมินดู/คืนของผู้เล่นคนนั้นข้ามเซิร์ฟ, แจ้งเตือนตอน join นับจาก DB — ลบด้วย id จึงปลอดภัยกับหลายเซิร์ฟพร้อมกัน (local mode ทำงานเหมือนเดิมทุกอย่าง)
- **Auto-return ให้เจ้าของ (ของเช่า)** — `Settings.return_to_owner`: `/ti give` ประทับ **ผู้ส่ง** เป็นเจ้าของใน NBT (`/ti info` แสดง Owner) — เมื่อไอเทมหมดอายุ ถ้าเจ้าของออนไลน์จะได้ของกลับเข้า inventory ทันที (ตัด timer ให้เช่าต่อได้ + แจ้งเจ้าของ); เจ้าของออฟไลน์ = ลง recovery ตามปกติ; ถ้าตั้ง transform profile ไว้ transform มีสิทธิ์ก่อน
- **บล็อกขาย timed item ในร้านค้า/ประมูล** — QuickShop (รวม Hikari fork), zAuctionHouse และ CrazyAuctions: event สร้างร้าน/ลงขายของที่มี timer จะถูก cancel พร้อมแจ้งผู้เล่น — ปิดรายตัวได้ที่ `Hooks.quickshop|zauctionhouse|crazyauctions: false`; ปลั๊กอินไหนไม่ได้ติดตั้ง/เวอร์ชัน event ไม่ตรง = ปิดตัวเองอย่างปลอดภัย (reflection ล้วน ตามแพทเทิร์นเดิม)

### Technical
- `Shulkers` helper (BlockStateMeta → live ShulkerBox inventory → setBlockState write-back) — entry gated ที่ material `*SHULKER_BOX` เท่านั้น กัน overhead
- `Storage` interface + 3 backends เพิ่ม `loadRecoveryFor(uuid, limit)` / `countRecoveryFor(uuid)` (SQL: `WHERE player_uuid = ? ORDER BY time_millis DESC LIMIT ?`); RestoreMenu มี 2 source (local memory / async DB) ในคลาสเดียว
- PDC key ใหม่ `timeditem:owner` ("uuid|name"); `ItemExpireEvent` pipeline เพิ่มสาย return-to-owner ก่อน recovery
- `hook/ShopSellBlockHook` — hook กลางตัวเดียว probe event class หลาย candidate + getter probe (getItem/getItemStack/getStack) + cancel แบบ reflective
- plugin.yml softdepend เพิ่ม QuickShop, zAuctionHouse, CrazyAuctions; unit tests 80 ชุดผ่าน; smoke test บน Paper 1.21 ผ่าน

## [3.0.0] - 2026-10-27

### Added
- **Visual decay stages** (`Settings.visual_stages`) — ไอเทมใกล้หมดอายุ "เน่าเปื่อย" ให้เห็น: เวลาที่เหลือต่ำกว่าเกณฑ์ → ชื่อไอเทมเปลี่ยนสี และ/หรือ มี enchant glint (ของ vanilla เปลี่ยนสีชื่อ, ของ custom จาก integration ใช้แค่ glow กันทับชื่อของปลั๊กอินต้นทาง); กลับสี/เอา glow ออกเองเมื่อต่อเวลา; เก็บสถานะ stage ใน NBT จึงเขียนไอเทมเฉพาะตอน stage เปลี่ยนจริง
- **Hologram countdown** (`Settings.hologram`) — ตัวเลขนับถอยหลังลอยเหนือไอเทมที่ตกพื้น (TextDisplay, 1.19.4+) หายเมื่อถูกหยิบ/despawn/หมดอายุ — ปิดบน Folia (ข้อจำกัด threading) และเซิร์ฟเก่ากว่า 1.19.4
- **Live lore countdown** (`Settings.live_lore`) — บรรทัด lore ของไอเทมที่ถืออัปเดตเวลาที่เหลือสดทุกวินาที (1.14+)
- **Admin GUI** (`/ti admin [หน้า] [filter]`) — ภาพรวมไอเทมจับเวลาทั้งเซิร์ฟจากการสแกนล่าสุด: เจ้าของ, ประเภท, เวลาที่เหลือ, item id — คลิกซ้าย = ทำให้หมดอายุทันที, คลิกขวา = ลบตัวจับเวลา (action รันบน region thread ของเจ้าของ ปลอดภัยบน Folia)
- **`/ti check <ผู้เล่น>`** — รายการไอเทมจับเวลาทั้งหมดที่ผู้เล่นถือ (ช่อง, ชนิด, ประเภท timer, เวลาที่เหลือ) — ผู้เล่นออนไลน์เท่านั้น (อ่าน inventory ออฟไลน์ไม่รองรับโดยการออกแบบ)
- **Container Scanning** (`Settings.container_scan`) — ตรวจไอเทมหมดอายุในตู้เก็บของ: `open` = ตรวจตอนผู้เล่นเปิดตู้ (เบา, ใช้ได้ทุกเซิร์ฟรวม Folia) · `periodic` = เดินตรวจตู้ทุก chunk ที่โหลดตามรอบ (ครอบคลุมตู้ที่ไม่มีใครเปิด, เฉพาะ non-Folia) · `both`
- **Anti-hide** (`Settings.anti_hide`) — กันของใกล้หมดอายุถูก "ยัดทิ้ง" ในตู้ที่ไม่มีใครเปิด: ย้ายของที่เหลือน้อยกว่า `block_below_seconds` ลงตู้ไม่ได้ (ครอบคลุม cursor place + shift-click) — คู่หูที่จำเป็นของ container scan
- Tab completion ใหม่สำหรับ admin/check

### Technical
- `TrackedItems` — snapshot หน่วยความจำของไอเทมที่การสแกนแต่ละรอบเห็น (feed `/ti admin`); เคลียร์-เติมใหม่ทุกรอบ ใช้หน่วยความจำคงที่
- `HologramCompat` — spawn/update TextDisplay แบบ reflection ตามแพทเทิร์น compat อื่นๆ (คอมไพล์กับ 1.13 แต่รันได้ถึง 26.x, เซิร์ฟเก่า = ฟีเจอร์ปิดเอง)
- `VisualStages.apply` ผูกเข้ากับรอบ scan เดิม — เขียน display name/เอนแชนต์เฉพาะเมื่อ stage เปลี่ยน (จำ original name ไว้ใน PDC เพื่อ restore); `PdcCompat` เพิ่ม generic string key API (`timeditem:stage`, `timeditem:visname`)
- Live lore เขียน lore line ให้ยัง parse ได้ (prefix + วินาที) และ require PDC (1.13 ที่ lore เป็นที่เก็บจริงจะไม่เปิดฟีเจอร์นี้)
- Unit tests 80 ชุดผ่าน · smoke test บน Paper 1.21: boot สะอาดทั้ง default และเปิดฟีเจอร์ Phase D ครบทุกตัว

## [2.9.0] - 2026-10-26

### Added
- **ประเภทตัวจับเวลาใหม่ 2 แบบ** (ต่อยอดจาก expire / lifetime / paused):
  - **PLAYTIME — อายุการเล่น** (`/ti set playtime 10h`) — นับถอยหลัง **เฉพาะตอนเจ้าของออนไลน์** — ของไม่หมดอายุระหว่างผู้เล่นออฟไลน์ เหมาะกับกิลด์เกียร์/ของเช่ารายชั่วโมงเล่น
  - **HELD — เวลาถือ** (`/ti set held 30m`) — นับเฉพาะตอน **ถือในมือ, มือรอง หรือสวมใส่** — วางใน chest ก็หยุดเอง เหมาะกับอาวุธผลาญเวลาการใช้งาน
- **Give ได้ทั้ง 2 แบบทันที**: `/ti give <player> playtime:<time> [amount]` · `/ti give <player> held:<time> [amount]` — ใช้กับของ custom ได้ด้วย (`/ti give Steve mmoitems:SWORD:X playtime:5h`)
- `/ti add` ต่อเวลาได้ทุกประเภท (รวม playtime/held) · `/ti info` + `/ti menu` แสดงประเภทและเวลาที่เหลือถูกต้อง · **Renewal token ต่ออายุ playtime/held ได้** · actionbar countdown และ PlaceholderAPI (`%timeditem_remaining%`, `%timeditem_type%` คืน `playtime`/`held`) รองรับเต็มรูปแบบ
- Tab completion: `/ti set` เสนอ `playtime`/`held`, give เสนอ `playtime:`/`held:` tokens

### Technical
- `TimeLore.Type` เพิ่ม `PLAYTIME`/`HELD` — PDC เก็บ "วินาทีคงเหลือ" (เหมือน paused), lore prefix ใหม่ `DateFormat.playtime` / `DateFormat.held` (ปรับได้ทั้ง 2 ภาษา, ไฟล์ lore เก่าไม่ได้รับผลกระทบ)
- **Tick logic ใหม่ใน ExpiryCheckTask** (`ExpiryListener.tickCountdownTimers`) — ลดวินาที **รอบละครั้งเท่านั้น** (ตาม `expire_check.interval_seconds`, default 10 วิ) ไม่ซ้ำกับ event path; ครบ 0 = expire ผ่าน pipeline เดิม (recovery + transform + webhook + สถิติ ทำงานครบ); ต้องเปิด `expire_check.enabled` (default เปิดอยู่แล้ว); creative skip ตามเดิม
- ข้อจำกัด: `/ti pause` ใช้ได้เฉพาะไอเทมแบบวันที่ (expire) เหมือนเดิม — playtime/held หยุดเองตามเงื่อนไข
- Unit tests: `TimeLoreTest` เพิ่ม case parse lore playtime/held (รวม 80 ชุด)

## [2.8.0] - 2026-10-26

### Added
- **แปลงร่างเมื่อหมดอายุ (Expiry Transformation)** — ไอเทมหมดอายุแล้ว "กลายเป็น" อีกชิ้นแทนการหายไปเฉยๆ:
  - นิยาม profile ได้หลายตัวที่ `Settings.transforms.<key>` — ผลลัพธ์เป็น vanilla material หรือ **custom item จาก integration** (`custom_id: "mmoitems:SWORD:X"`, CrazyCrates ฯลฯ) พร้อมแก้ชื่อ/lore/จำนวน และรัน console commands เพิ่มตอนแปลง (`{player}` ใช้ได้)
  - เลือก profile ต่อไอเทมด้วย `transform:<key>` — `/ti set 7d transform:ash` หรือ `/ti give Steve 7d transform:ash` (แท็กอยู่ใน NBT ติดไปกับไอเทม); ไม่แท็ก = ใช้ `expiry_transform.default` ถ้าตั้งไว้
  - ครอบคลุมทุก context อัตโนมัติ: ในตัวผู้เล่น (เข้า inventory เอง) และบนพื้น/เฟรม (ดรอปที่ตำแหน่งนั้น)
  - Developer API: `ItemExpireEvent` เพิ่ม `getReplacement()` / `setReplacement()` — แก้หรือยกเลิกการแปลงได้จากปลั๊กอินอื่น
- **Grace period** (`Settings.grace`) — ช่วงวินาทีสุดท้าย (default 30 วิ เปิดได้) ไอเทมใช้งานไม่ได้ (คลิกขวาถูก cancel + แจ้งเตือน) แต่จะหายจริงเมื่อเวลาถึงศูนย์เท่านั้น — เตือนแรงก่อนของหาย
- **Renewal token (ตัวต่ออายุ)** (`Settings.renewal`) — คลิกขวาถือ token มือหนึ่ง + ไอเทมจับเวลาอีกมือหนึ่ง = ต่ออายุตาม `add_time` และหัก token 1 ชิ้น; token เป็น vanilla material หรือ custom item ก็ได้ (เช่นแจกจาก CrazyCrates); กันคลิกซ้ำด้วย cooldown; บันทึก audit อัตโนมัติ
- **Restore reminder ตอน join** — แอดมินที่มีไอเทมของตัวเองค้างใน recovery log จะเห็น "มีไอเทมหมดอายุ N ชิ้นรอกู้คืน" ทันทีที่เข้าเซิร์ฟ
- **Discord webhook แบบ embed** (`discord_webhook.use_embed`) — ส่งเป็น rich embed (title / description / สี / timestamp) แทนข้อความธรรมดา

### Technical
- `PdcCompat` เพิ่ม key `timeditem:transform` (STRING) เก็บ profile key ต่อไอเทม (ถูกลบพร้อม timer เมื่อ remove)
- คำสั่ง `/ti set|give` แยก token `transform:<key>` ออกจากพารามิเตอร์เวลาก่อน parse — วางตำแหน่งไหนก็ได้หลัง subcommand; key ไม่มีจริง = แจ้งเตือนทันที
- Renewal ทำงานกับ timer ทุกประเภท (expire ต่อจากเวลาที่เหลือ, lifetime ยังไม่เริ่ม +, paused +)
- Unit tests ใหม่: `TransformsTest` (parse profile, ข้าม key พัง, case-insensitive), `DiscordWebhookTest` เพิ่ม embed JSON (title/description/color/timestamp) — รวม 78 ชุด
- Smoke test บน Paper จริง: boot สะอาด + config self-heal ครบทุก section + fresh install ได้ config-version 10

## [2.7.0] - 2026-10-26

### Added
- **ระบบฐานข้อมูลหลายตัว** (`Settings.storage.type` — เลือกได้ 4 แบบ, เปลี่ยนแล้วรีสตาร์ท):
  - **`flat`** (default) — ไฟล์ใน `plugins/TimedItem/` เหมือนเดิมทุกอย่าง
  - **`sqlite`** — ไฟล์เดียว `database.db` ไม่ต้องติดตั้งเซิร์ฟเวอร์
  - **`mysql`** — MariaDB/MySQL ผ่าน connection pool (HikariCP) สำหรับ network หลายเซิร์ฟ
  - **`mongodb`** — ต่อด้วย connection string มาตรฐาน
  - ทุก backend รองรับข้อมูล 3 กลุ่ม: **recovery** (กู้คืนไอเทม), **audit log**, **สถิติการหมดอายุรายวัน**
  - บูตครั้งแรกด้วยฐานข้อมูล = import `recovery.log` เดิมอัตโนมัติ (ไฟล์เดิมกลายเป็น `recovery.log.imported`); ต่อ DB ไม่ได้ = fallback กลับเป็นไฟล์พร้อม warn — ปลั๊กอินไม่มีวาย
- **Item UID** — ไอเทมจับเวลาทุกชิ้น (1.14+) ได้ UUID ฝังใน NBT ตอนตั้งเวลา ใช้ติดตามใน audit และการค้นหา; entry ใน recovery เก็บ **UUID ผู้เล่น** ด้วย (เดิมเก็บแต่ชื่อ) เตรียมระบบ restore ข้ามเซิร์ฟ
- **`/ti find <material|plugin:id|ชื่อ>`** — ค้นหาไอเทมจับเวลาทั้งเซิร์ฟ: ในตัวผู้เล่นออนไลน์, ตู้เก็บของที่โหลดอยู่ (chest/barrel/shulker/เตา ฯลฯ), บนพื้น, ใน item frame, บน armor stand — รายงานพร้อมพิกัด (บน Folia ค้นเฉพาะผู้เล่นออนไลน์เพราะข้อจำกัด threading)
- **`/ti stats [วัน]`** — สถิติไอเทมที่หมดอายุแยกตามชนิด (นับจากทุก context: ผู้เล่น/พื้น/เฟรม)
- **`/ti audit [หน้า]`** — ประวัติการกระทำที่ผ่าน `/ti` ทั้งหมด (set / add / pause / resume / remove / give / restore) พร้อมผู้กระทำ (console รวม), เป้าหมาย, uid ของไอเทม และรายละเอียด — เก็บสูงสุด `Settings.storage.audit_history` รายการ (default 5000)
- Tab completion ใหม่สำหรับ find/stats/audit; รายงาน `/ti debug export` แสดง storage backend ที่ใช้อยู่

### Technical
- **ชั้นเก็บข้อมูลใหม่** `me.albert.timeditem.storage`: interface `Storage` + `StorageWriter` (single background writer thread — ทุกการเขียนเป็น fire-and-forget ปลอดภัยกับ region thread ของ Folia); 4 backends: `FlatFileStorage` (คงพฤติกรรม 2.6 ทุกด้าน), `SqliteStorage` / `MySqlStorage` (แชร์ฐาน `SqlStorage` + ตาราง `timeditem_recovery` / `timeditem_audit` / `timeditem_stats`), `MongoStorage` (collections เดียวกัน)
- **`recovery.log` ฟอร์แมต v3** — เพิ่ม id (UUID) และ player UUID ขึ้นหน้าบรรทัด; บรรทัด v1/v2 เก่าโหลดต่อได้ (ได้ id สร้างใหม่อัตโนมัติ); ทั้ง flat และ DB re-encode ข้อมูลเป็นฟอร์แมตเซิร์ฟปัจจุบันเองตอนโหลดเหมือนเดิม
- Dependencies ใหม่ทั้งหมด **shade + relocate** คงสัญญา jar เดียว Java 8 bytecode: HikariCP 4.0.3, sqlite-jdbc, mariadb-java-client (Apache 2.0, พูดโปรโตคอล MySQL), mongodb-driver-sync 4.11 — ขนาด jar โตจาก ~0.2MB เป็น ~17MB จาก driver ฐานข้อมูล
- ปักหมุด `slf4j-api` 2.0.13 + `slf4j-nop` (HikariCP ดึง slf4j 2.0.0-alpha1 มาให้ — แก้ให้ console สะอาด ไม่มี SLF4J warning)
- `/ti reload` **ไม่** เปลี่ยน storage backend (ต้องรีสตาร์ทโดยการออกแบบ — กันข้อมูลครึ่งไฟล์ครึ่ง DB)
- Unit tests ใหม่: `RecoveryFormatTest` (ฟอร์แมต v3 + ย้อนหลัง v1/v2), `FlatFileStorageTest` (recovery/audit/stats ครบ + escape อักขระพิเศษ), `SqliteStorageTest` (รัน SQLite จริง — คลอบ path SQL เดียวกับ MySQL รวม dialect upsert)

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
