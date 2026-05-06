# History Feature — Flow Lengkap

## Arsitektur Umum

Fitur History menggabungkan **dua sumber data** menjadi satu list di UI:

1. **Server (REST API)** — riwayat deal dari Stockity, disimpan di Room `trade_deals` (`coba.db`)
2. **Bot Lokal (WebSocket)** — deal yang dikirim oleh TradingEngine, disimpan di Room `bot_deals` (`bot.db`)

```
┌─────────────────────────────────────────────────────────────────────┐
│                     HISTORY FEATURE OVERVIEW                        │
├─────────────────────────────────────────────────────────────────────┤
│                                                                     │
│  ┌──────────────┐     ┌──────────────┐                              │
│  │  REST API     │     │  WebSocket   │                              │
│  │  /bo-deals-   │     │  phx_reply   │                              │
│  │  history/v3   │     │  (bo/create) │                              │
│  └──────┬───────┘     └──────┬───────┘                              │
│         │                    │                                       │
│         ▼                    ▼                                       │
│  ┌──────────────┐     ┌──────────────┐                              │
│  │  coba.db      │     │  bot.db      │                              │
│  │  trade_deals  │     │  bot_deals   │                              │
│  └──────┬───────┘     └──────┬───────┘                              │
│         │                    │                                       │
│         └────────┬───────────┘                                       │
│                  ▼                                                    │
│         ┌──────────────────┐                                         │
│         │  HistoryViewModel │                                        │
│         │  combine() +      │                                        │
│         │  deduplikasi UUID │                                        │
│         └────────┬─────────┘                                         │
│                  ▼                                                    │
│         ┌──────────────────┐                                         │
│         │  HistoryScreen    │                                        │
│         │  Filter + List +  │                                        │
│         │  Detail Sheet     │                                        │
│         └──────────────────┘                                         │
└─────────────────────────────────────────────────────────────────────┘
```

---

## File yang Terlibat

| Role | File Path |
|------|-----------|
| **UI** | `ui/screens/HistoryScreen.kt` |
| **ViewModel** | `ui/screens/HistoryViewModel.kt` |
| **Repository** | `data/repository/TradeHistoryRepository.kt` |
| **API** | `data/network/ApiService.kt` |
| **Room: server deals** | `data/local/TradeDealEntity.kt`, `TradeDealDao.kt`, `TradeDealMappers.kt` |
| **Room: bot deals** | `data/local/BotDealEntity.kt`, `BotDealDao.kt` |
| **Database** | `data/local/CobaDatabase.kt` (v3), `BotDatabase.kt` (v1) |
| **Provider** | `data/local/DatabaseProvider.kt`, `BotDatabaseProvider.kt` |
| **Migrasi** | `data/local/CobaDatabaseMigrations.kt` |
| **Presentasi bot** | `util/BotDealPresentation.kt` |
| **Bot insert + WS korelasi** | `data/TradingEngine.kt` |
| **Sync timestamp** | `data/DataStoreManager.kt` (`HISTORY_LAST_SYNCED_AT_KEY`) |
| **Navigasi** | `ui/Navigation.kt`, `MainActivity.kt` |
| **Pair list merge** | `data/repository/AssetsRepository.kt` (`mergeSortedRicLists`) |

---

## Tahap 1: Data Masuk dari Server (REST API)

### Endpoint

```
GET /bo-deals-history/v3/deals/trade?type=real|demo&locale=id
```

Didefinisikan di `ApiService.kt`:

```kotlin
@GET("/bo-deals-history/v3/deals/trade")
suspend fun getTradeDealsRaw(
    @Query("type") type: String,
    @Query("locale") locale: String = "id"
): ResponseBody
```

### Parsing (`TradeHistoryRepository.kt`)

Response JSON di-parse secara fleksibel:

```
Response bisa berupa:
  - Root array: [ {...}, {...} ]
  - Object wrapper: { "data": { "standard_trade_deals": [...] } }
  - Object langsung: { "records": [...] } atau { "trades": [...] }
```

Setiap deal object di-mapping ke `HistoryItem` dengan normalisasi:

| Field | Sumber JSON (prioritas) | Normalisasi |
|-------|-------------------------|-------------|
| **id** | `id`, `deal_id` | Long, wajib |
| **pair** | `asset_ric`, `ric`, `symbol`, `pair`, `instrument`, `asset_name`, `name` | String, default `"—"` |
| **status** | `status`, `result`, `outcome`, `deal_status` | `win/won` → `"won"`, `lose/lost` → `"lost"`, `tie/draw/equal` → `"tie"` |
| **type** | `trend`, `direction`, `side` | `call/buy/up` → `"BUY"`, `put/sell/down` → `"SELL"` |
| **accountMode** | `deal_type`, atau dari query param `type` | `"Demo"` / `"Real"` |
| **currency** | `currency`, `wallet_currency`, `iso_currency`, `ccy` | Default `"IDR"` |
| **amount** | `amount`, `investment`, `sum`, `volume`, `stake` | Dibagi 100 (minor → display) |
| **profit** | `win/payment/payout` − amount, atau `profit`, `profit_amount`, `gain` | Dibagi 100 |
| **createdAt** | `closed_at`, `finished_at`, `completed_at`, ..., `created_at` | Prioritas waktu tutup, epoch ms |
| **serverUuid** | `uuid`, `deal_uuid`, `dealUuid`, `guid`, `external_uuid` | Untuk korelasi dengan bot deals |

### Persistensi ke Room

Disimpan via `TradeDealDao.mergeAccountDeals()`:

```
mergeAccountDeals(accountMode, items):
  1. Ambil dealId yang sudah ada di DB untuk accountMode
  2. Item baru (dealId belum ada) → INSERT
  3. Item existing (dealId sudah ada) → REPLACE (update)
  4. Item lama di DB yang tidak ada di response → TIDAK DIHAPUS
```

Strategi merge ini memastikan data offline tetap tersedia.

---

## Tahap 2: Data Masuk dari Bot (WebSocket)

### Insert Saat Order Dikirim

`TradingEngine.sendBoTurboDeal()` → insert `BotDealEntity`:

```
BotDealEntity:
  ├── ric, trend, dealType, amountMinor, durationSeconds
  ├── strategy (storageKey: "macd_rsi", "bollinger", "price_action")
  ├── Snapshot indikator: rsi, macd, macdSignal, histogram
  ├── Snapshot indikator: bbUpper, bbMiddle, bbLower
  ├── priceActionNote
  ├── wsRef = Phoenix ref string (untuk korelasi reply)
  ├── serverDealUuid = null (belum tahu)
  ├── wsReplyStatus = "pending"
  ├── wsReplyMessage = null
  └── createdAt = epoch ms
```

### Update Saat WS Reply Diterima

`TradingEngine.startBoResultCollectorIfNeeded()` mendengarkan `boCreateResults`:

```
Phoenix phx_reply:
  ├── status: "ok"
  │   └── updateReplyStatus(id, "ok", uuid, null)
  │       → serverDealUuid = uuid dari server
  │
  └── status: error
      └── updateReplyStatus(id, "error", null, errorMessage)
```

---

## Tahap 3: Merge di HistoryViewModel

### Flow Reaktif

```kotlin
combine(serverFlow, botFlow) { serverEntities, botEntities ->
    // ...merge logic...
}
```

### Logika Merge (Step-by-Step)

```
1. Buat UUID index dari server deals:
   uuidIndex = { "abc-123": TradeDealEntity, "def-456": TradeDealEntity, ... }

2. Kumpulkan UUID yang dimiliki bot deals:
   botUuidsWithRow = { "abc-123", "ghi-789", ... }

3. Filter SERVER rows:
   - TAMPILKAN jika serverUuid kosong (bukan dari bot)
   - TAMPILKAN jika serverUuid TIDAK ada di botUuidsWithRow
   - SEMBUNYIKAN jika serverUuid ADA di botUuidsWithRow
     (supaya tidak duplikat — bot row yang ditampilkan)

4. Convert BOT rows ke HistoryItem:
   Untuk setiap bot deal:
   ├── Cari matchedServer = uuidIndex[bot.serverDealUuid]
   ├── Status = resolveBotStatus(bot, matchedServer)
   ├── Profit = matchedServer?.profit ?: 0.0
   ├── Currency = matchedServer?.currency ?: "IDR"
   └── id = -bot.id (negatif, supaya tidak clash dengan server dealId)

5. Gabung & sort:
   (serverRows + botRows).sortedByDescending { it.createdAt }
```

### Visualisasi Deduplikasi

```
Contoh: Bot kirim deal → server reply uuid "abc-123"
        History sync → server punya deal dengan uuid "abc-123"

Tanpa deduplikasi:
  ┌──────────────────┐  ┌──────────────────┐
  │ Server Row       │  │ Bot Row          │  ← DUPLIKAT!
  │ uuid: abc-123    │  │ uuid: abc-123    │
  │ profit: +50      │  │ profit: +50      │
  └──────────────────┘  └──────────────────┘

Dengan deduplikasi:
  ┌──────────────────┐
  │ Bot Row          │  ← Satu baris saja
  │ uuid: abc-123    │     (dengan badge "Bot" + info strategi)
  │ profit: +50      │     (profit dari matchedServer)
  └──────────────────┘
```

---

## Tahap 4: Status Lifecycle Bot Deal

### State Machine

```
                    ┌──────────┐
                    │ PENDING  │  wsReplyStatus = "pending"
                    │ (WS)    │  WS belum reply
                    └────┬─────┘
                         │
              ┌──────────┼──────────┐
              ▼                     ▼
       ┌──────────┐          ┌──────────┐
       │ FAILED   │          │ OPEN     │  wsReplyStatus = "ok"
       │          │          │          │  now < expireAt
       └──────────┘          └────┬─────┘
       wsReplyStatus              │
       = "error"                  │ waktu expire
                                  ▼
                           ┌──────────┐
                           │ AWAITING │  wsReplyStatus = "ok"
                           │ SYNC     │  now >= expireAt
                           └────┬─────┘  tapi belum ada match
                                │        di trade_deals
                                │
                                │ history refresh (pull / auto)
                                │ matchedServer ditemukan
                                ▼
                    ┌───────────────────────┐
                    │  WON / LOST / TIE     │
                    │  (dari matchedServer   │
                    │   .status)             │
                    └───────────────────────┘
```

### Kode `resolveBotStatus()`

```kotlin
fun resolveBotStatus(deal: BotDealEntity, matchedServer: TradeDealEntity?): String = when {
    deal.wsReplyStatus == "pending"                          -> "pending"
    deal.wsReplyStatus == "error"                            -> "failed"
    matchedServer != null                                    -> matchedServer.status.lowercase()
    deal.wsReplyStatus == "ok" && uuid tidak kosong -> {
        if (now < expireAtEpochMs(deal)) "open"
        else "awaiting"
    }
    else                                                     -> "pending"
}
```

---

## Tahap 5: UI (HistoryScreen)

### Filter

```
┌───────────────────────────────────────────┐
│  STATUS    │  PAIR          │  ACCOUNT    │
│  ▼ All     │  ▼ All (search)│  ▼ All     │
│  · Won     │  · Z-CRY/IDX  │  · Real    │
│  · Lost    │  · EUR/USD     │  · Demo    │
│  · Tie     │  · ...         │             │
└───────────────────────────────────────────┘
```

| Filter | Scope | Catatan |
|--------|-------|---------|
| **Status** | Lokal (composable) | Case-insensitive match pada `item.status`. Bot status `pending/failed/open/awaiting` hanya muncul di `All` |
| **Pair** | Lokal (composable) | Gabungan RIC dari `asset_choices` + RIC unik dari history items, searchable dropdown |
| **Account** | ViewModel + API | Men-drive `load(filter)`: mengubah query Room DAN scope fetch API (`type=real/demo`) |

### Komponen UI

```
┌─────────────────────────────────────────┐
│  Last synced: 06 May 2026, 07:30        │  ← DataStore timestamp
├─────────────────────────────────────────┤
│  ┌─────────────────────────────────┐    │
│  │  🤖 Bot  Z-CRY/IDX             │    │  ← HistoryCard (bot, punya badge)
│  │  BUY · Won · IDR 100 · +50     │    │
│  │  30 May 2026, 14:22             │    │
│  └─────────────────────────────────┘    │
│  ┌─────────────────────────────────┐    │
│  │  EUR/USD                        │    │  ← HistoryCard (server, tanpa badge)
│  │  SELL · Lost · USD 50 · -50     │    │
│  │  30 May 2026, 14:15             │    │
│  └─────────────────────────────────┘    │
│  ...                                    │
├─────────────────────────────────────────┤
│  [Pull to refresh]                      │
└─────────────────────────────────────────┘
```

### Detail Bottom Sheet (Tap Card)

```
┌─────────────────────────────────────────┐
│  Order Detail                           │
├─────────────────────────────────────────┤
│  Pair:        Z-CRY/IDX                │
│  Type:        BUY                       │
│  Status:      Won                       │
│  Account:     Demo                      │
│  Amount:      IDR 100.00                │
│  Profit:      IDR 50.00                 │
│  Time:        06 May 2026, 14:22:30     │
│  UUID:        abc-123-def-456           │
│                                         │
│  ── Bot Info ──────────────────────     │  ← Hanya untuk source = Bot
│  Strategy:    MACD + RSI                │
│  Local ID:    42                        │
│                                         │
└─────────────────────────────────────────┘
```

### Warna Status

| Status | Warna |
|--------|-------|
| `won` | Hijau |
| `lost`, `fail`, `failed` | Merah |
| `tie`, `draw`, `equal` | Abu-abu (default variant) |
| `pending`, `open`, `awaiting` | Abu-abu (default variant) |

---

## Database Schema

### `coba.db` (versi 3) — Data Server

**Tabel: `trade_deals`**

| Kolom | Tipe | Keterangan |
|-------|------|------------|
| `dealId` | Long | PK (bersama accountMode) |
| `accountMode` | String | PK — `"Real"` / `"Demo"` |
| `pair` | String | RIC / nama aset |
| `status` | String | `won` / `lost` / `tie` |
| `type` | String | `BUY` / `SELL` |
| `currency` | String | `IDR`, `USD`, dll |
| `amount` | Double | Jumlah investasi (display unit) |
| `profit` | Double | Keuntungan/kerugian |
| `createdAt` | Long | Epoch ms |
| `serverUuid` | String? | UUID deal dari server (ditambah di migrasi v2→v3) |

**Tabel: `asset_choices`**

| Kolom | Tipe | Keterangan |
|-------|------|------------|
| `ric` | String | PK — kode aset |
| `label` | String | Nama tampilan |

**Migrasi:**
- v1 → v2: Tambah tabel `asset_choices`
- v2 → v3: Tambah kolom `serverUuid` ke `trade_deals`

### `bot.db` (versi 1) — Data Bot Lokal

**Tabel: `bot_deals`**

| Kolom | Tipe | Keterangan |
|-------|------|------------|
| `id` | Long | PK (autoGenerate) |
| `ric` | String | Kode aset |
| `trend` | String | `call` / `put` |
| `dealType` | String | `demo` / `real` |
| `amountMinor` | Long | Jumlah × 100 |
| `durationSeconds` | Int | Durasi deal |
| `strategy` | String | `macd_rsi` / `bollinger` / `price_action` |
| `rsi` | Double | Snapshot RSI saat order |
| `macd` | Double | Snapshot MACD line |
| `macdSignal` | Double | Snapshot signal line |
| `histogram` | Double | Snapshot histogram |
| `bbUpper` | Double? | Bollinger upper band |
| `bbMiddle` | Double? | Bollinger middle band |
| `bbLower` | Double? | Bollinger lower band |
| `priceActionNote` | String? | Pattern yang terdeteksi |
| `wsRef` | String? | Phoenix ref untuk korelasi reply |
| `serverDealUuid` | String? | UUID dari server (diisi setelah WS reply ok) |
| `wsReplyStatus` | String | `pending` / `ok` / `error` |
| `wsReplyMessage` | String? | Pesan error jika gagal |
| `createdAt` | Long | Epoch ms |

---

## Model Data UI

### `HistoryItem`

```kotlin
data class HistoryItem(
    val id: Long,                // dealId (server) atau -botId (bot)
    val pair: String,            // RIC aset
    val status: String,          // won/lost/tie/pending/failed/open/awaiting
    val type: String,            // BUY / SELL
    val accountMode: String,     // Real / Demo
    val currency: String,        // IDR, USD
    val amount: Double,          // Display unit
    val profit: Double,          // Display unit
    val createdAt: Long,         // Epoch ms
    val serverUuid: String?,     // Korelasi bot ↔ server
    val source: HistoryRowSource,// Server / Bot
    val botLocalId: Long?,       // Room id di bot_deals (hanya untuk bot)
    val botStrategyKey: String?, // Storage key strategi (hanya untuk bot)
)
```

### `HistoryRowSource`

```kotlin
enum class HistoryRowSource {
    Server,  // Deal dari REST API history
    Bot,     // Deal yang dikirim oleh TradingEngine
}
```

---

## Kenapa Arsitektur Ini

| Keputusan | Alasan |
|-----------|--------|
| **Dua database terpisah** | `coba.db` untuk data server (bisa dihapus/re-sync tanpa kehilangan data bot), `bot.db` untuk data lokal bot (tidak boleh hilang karena berisi snapshot indikator) |
| **Merge di ViewModel** (bukan SQL JOIN) | Dua DB berbeda tidak bisa di-JOIN langsung; `combine()` Kotlin Flow menyelesaikan ini secara reaktif |
| **UUID sebagai penghubung** | Satu-satunya cara menghubungkan "deal dikirim bot" dengan "deal tercatat di server" adalah `serverDealUuid` (dari WS reply) di-match dengan `serverUuid` (dari REST history) |
| **mergeAccountDeals (bukan replace)** | Server mungkin hanya return halaman terbaru; deal lama di lokal tidak boleh hilang |
| **Bot row menang saat duplikat** | Bot row punya info tambahan (strategi, indikator) yang server row tidak punya |
| **Status lifecycle di client** | Server tidak push status update; client harus cek via `resolveBotStatus()` + periodic history refresh |

---

## Navigasi

```
MainActivity
  └── NavHost
       ├── Screen.Trade    → TradeScreen
       ├── Screen.History  → HistoryScreen (HistoryViewModel.Factory)
       ├── Screen.Web      → WebScreen
       ├── Screen.Profile  → ProfileScreen
       ├── Screen.Debug    → DebugScreen (tabs: DataStore, Room DB, Bot DB)
       ├── Screen.DebugDb  → DebugDatabaseScreen (trade_deals + asset_choices)
       └── Screen.DebugBotDb → DebugBotDatabaseScreen (bot_deals + lifecycle)
```

History diakses via bottom navigation tab "History".

---

**Dokumen ini di-generate:** 2026-05-06
