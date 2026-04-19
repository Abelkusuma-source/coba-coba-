# 🎯 PROJECT COBA - Quick Feature Summary

## 📱 Main Screens (8 Total)

```
┌─────────────────────────────────────────────────────┐
│  SPLASH SCREEN                                      │
│  (Loading DataStore)                                │
└────────────┬────────────────────────────────────────┘
             │
             ├──→ NO AUTH TOKEN
             │    │
             │    └──→ LOGIN SCREEN
             │         ├─ Email input
             │         ├─ Password input (toggle show/hide)
             │         └─ OTP input (jika 2FA required)
             │              └─ 6 digit fields dengan auto-focus
             │
             ├──→ NO USER AGREEMENT
             │    │
             │    └──→ USER AGREEMENT SCREEN
             │         ├─ Scrollable liability text
             │         ├─ "I Agree" button
             │         └─ "I Disagree" button
             │
             ├──→ NO PERMISSIONS SHOWN
             │    │
             │    └──→ PERMISSION SCREEN
             │         ├─ Notification permission
             │         ├─ Battery optimization
             │         ├─ App restrictions
             │         └─ "Continue Anyway" button
             │
             └──→ MAIN APP (Bottom Navigation)
                  │
                  ├─ TRADE SCREEN ⭐
                  │  ├─ Main WebSocket status
                  │  ├─ Asset Socket status
                  │  ├─ Real-time price (Z-CRY/IDX)
                  │  ├─ START/STOP CONNECTION button
                  │  └─ Color-coded status badges
                  │
                  ├─ HISTORY SCREEN
                  │  ├─ Filter by status (Won/Lost/Tie)
                  │  ├─ Filter by pair
                  │  ├─ Filter by account mode
                  │  ├─ History card list
                  │  └─ Click → Detail bottom sheet
                  │
                  ├─ WEB SCREEN
                  │  ├─ WebView dengan stockity.id
                  │  ├─ Chrome version check
                  │  └─ Fallback UI jika too old
                  │
                  ├─ PROFILE SCREEN
                  │  ├─ User profile icon
                  │  ├─ Theme selector (Light/Dark/System)
                  │  └─ Save preference
                  │
                  └─ DEBUG SCREEN (via bug icon)
                     └─ Display semua DataStore values
```

---

## 🔄 Data Flow

```
┌──────────────────────────────────────────────────────────┐
│                    LOGIN FLOW                            │
└──────────────────────────────────────────────────────────┘

1. User input email + password
   ↓
2. Send POST /passport/v2/sign_in
   ↓
3. Server response:
   ├─ authtoken → Save ke DataStore.auth_token
   ├─ Set-Cookie → CookieJar.saveFromResponse()
   │              → Save ke DataStore.cookies
   ├─ is_2fa_enabled flag
   └─ [If 2FA needed]
      ├─ Trigger 2FA screen
      ├─ User input 6 digit OTP
      └─ Re-login dengan OTP token
   ↓
4. User agreement check
   ├─ Not agreed → Show UserAgreement screen
   └─ Agreed → Continue
   ↓
5. Permission check
   ├─ Not shown → Show Permission screen
   └─ Shown → Continue
   ↓
6. Main app ready
   ├─ Can access Trade/History/Web/Profile
   └─ WebSocket akan gunakan stored cookies + token

┌──────────────────────────────────────────────────────────┐
│              WEBSOCKET CONNECTION FLOW                   │
└──────────────────────────────────────────────────────────┘

User click "START CONNECTION"
   ↓
WebSocketManager.connect()
   ├─ Load device_id dari DataStore
   ├─ Load auth_token dari DataStore
   ├─ Load cookies dari DataStore ← SHOULD NOT BE EMPTY
   └─ Build headers:
      ├─ Device-Id: [value]
      ├─ Device-Type: web
      ├─ User-Agent: Chrome UA
      ├─ Authorization-Token: [token]
      └─ Cookie: [session cookies]
   ↓
Subscribe ke topics (connection, bo, marathon, user, account, tournament, etc)
   ↓
Heartbeat every 60s
Ping every 30s
   ↓
Listen untuk messages
   ↓
AssetSocketManager.connect()
   ├─ Same header setup
   └─ Listen untuk price ticks
      ├─ Parse rate
      ├─ Update AssetTick
      └─ Build candles (5s per candle)

┌──────────────────────────────────────────────────────────┐
│                   THEME FLOW                             │
└──────────────────────────────────────────────────────────┘

User select theme di Profile
   ↓
ProfileViewModel.onThemeSelected(mode)
   ↓
DataStoreManager.setThemeMode(mode)
   ├─ Save ke DataStore.theme_mode
   └─ ThemeMode flow emit
   ↓
MainActivity observe theme flow
   ↓
Apply CobaTheme(darkTheme = ...)
   └─ Dynamic or fixed color scheme
```

---

## 🔌 Networking

### **API Service**
```kotlin
interface ApiService {
    @POST("/passport/v2/sign_in?locale=id")
    suspend fun login(@Body request: LoginRequest): LoginResponse
}
```

### **Interceptors**
```
Request → AuthInterceptor → Add headers → Chain
          (Device-Id, Device-Type, Auth-Token, Cookie)

Response ← CookieJar ← Capture Set-Cookie
           (Save to DataStore)

Request/Response → OkHttp Logging (debug only)
```

### **WebSocket Servers**
```
Main WS:    wss://ws.stockity.id/?v=2&vsn=2.0.0
Asset WS:   wss://as.stockity.id/
```

---

## 💾 DataStore Schema

```
┌─────────────────────────────────────┐
│         DataStore Preferences       │
├─────────────────────────────────────┤
│ Key                   Type   Value   │
├─────────────────────────────────────┤
│ device_id            String UUID    │
│ auth_token           String Token   │
│ cookies              String "abc.." │ ⚠️ EMPTY!
│ is_2fa_enabled       Boolean false  │
│ user_agreed          Boolean true   │
│ permissions_shown    Boolean true   │
│ two_fa_token         String null    │
│ theme_mode           Int    0       │
└─────────────────────────────────────┘
```

---

## ✅ Working Features

| # | Feature | Status | Notes |
|---|---------|--------|-------|
| 1 | Splash Screen | ✅ | DataStore loading |
| 2 | Login Screen | ✅ | Email + password |
| 3 | 2FA OTP Input | ✅ | 6 digit dengan auto-focus |
| 4 | User Agreement | ✅ | Scrollable, can skip |
| 5 | Permission Request | ✅ | Real-time tracking |
| 6 | Bottom Navigation | ✅ | 4 main tabs |
| 7 | History Screen | ✅ | Mock data, filterable |
| 8 | WebView | ✅ | Chrome version check |
| 9 | Theme System | ✅ | Light/Dark/System |
| 10 | Debug Screen | ✅ | Shows DataStore |
| 11 | Animations | ✅ | Slide + fade transitions |

---

## ⚠️ Known Issues

| Issue | Status | Impact | Root Cause |
|-------|--------|--------|-----------|
| WebSocket 401 | 🔴 Blocked | Can't get real-time data | Cookies not persisted |
| Cookie Empty | 🔴 Blocked | No session | CookieJar not working |
| Real-time Price | 🔴 Blocked | Display empty | WebSocket fails |

---

## 📦 Build Info

```
App ID:           com.at.coba
Version:          6.6.6
Min SDK:          26
Target SDK:       36
Compile SDK:      36
Language:         Kotlin
UI Framework:     Jetpack Compose
Architecture:     MVVM
```

---

## 🎯 Next Steps to Fix

1. **Identify why CookieJar not capturing Set-Cookie**
   - Check server response headers
   - Verify Set-Cookie is present
   
2. **Fix cookie persistence**
   - Ensure cookies saved to DataStore
   - Verify cookies loaded back
   
3. **Test WebSocket connection**
   - Check if cookies included in request
   - Verify server accepts the headers
   
4. **Enable real-time price display**
   - Should work once WebSocket connects

---

**File Generated:** 2026-04-19 (untuk audit & reference)

bni