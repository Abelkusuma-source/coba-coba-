# 📱 PROJECT COBA - Complete Feature Documentation

**Version:** 6.6.6  
**Min SDK:** 26 | **Target SDK:** 36 | **Compile SDK:** 36  
**Language:** Kotlin | **UI Framework:** Jetpack Compose

---

## 🏗️ Architecture Overview

**Pattern:** MVVM (Model-View-ViewModel)  
**Navigation:** Jetpack Navigation Compose (Single Activity)  
**State Management:** Flow + StateFlow  
**Persistence:** DataStore Preferences  
**Networking:** Retrofit + OkHttp + WebSocket

---

## 📋 Complete Feature List

### **1️⃣ AUTHENTICATION SYSTEM**

#### **1.1 Login Screen**
- **File:** `LoginScreen.kt`, `LoginViewModel.kt`
- **Features:**
  - ✅ Email + Password input
  - ✅ Show/Hide password toggle
  - ✅ Real-time UI state (Loading, Error, Success)
  - ✅ Validation (email & password required)
  - ✅ Error message display

#### **1.2 Two-Factor Authentication (2FA)**
- **File:** `LoginViewModel.kt` (line 60-116)
- **Features:**
  - ✅ Detect 2FA requirement (code: "2fa_required" dari server)
  - ✅ 6-digit OTP input UI (line 105-174 LoginScreen.kt)
  - ✅ Auto-focus between OTP fields
  - ✅ Backspace to previous field
  - ✅ OTP verification via `/passport/v2/sign_in` endpoint
  - ✅ Re-login with OTP token
  - ✅ Error handling untuk invalid OTP

#### **1.3 Authentication State Management**
- **File:** `DataStoreManager.kt`
- **Stored Data:**
  - `auth_token` - JWT token untuk API requests
  - `is_2fa_enabled` - Flag apakah user punya 2FA enabled
  - `cookies` - Session cookies dari server
  - `two_fa_token` - OTP token
  - `device_id` - UUID yang di-generate otomatis

---

### **2️⃣ NAVIGATION SYSTEM**

#### **2.1 Single Activity Architecture**
- **File:** `MainActivity.kt`
- **Features:**
  - ✅ Conditional rendering berdasarkan:
    - Splash screen saat loading DataStore
    - Login screen (jika tidak ada auth token)
    - User Agreement screen (jika belum agree)
    - Permission screen (jika belum shown)
    - Main screens (Trade, History, Web, Profile)
  - ✅ Dynamic top app bar title
  - ✅ Version display (dari packageManager)
  - ✅ Back navigation button
  - ✅ Debug icon accessible dari semua screen

#### **2.2 Navigation Routes**
- **File:** `Navigation.kt`
- **Screens:**
  1. `Login` - Email/password login
  2. `UserAgreement` - Liability disclaimer
  3. `Permissions` - Battery optimization & notification
  4. `Trade` - Trading terminal dengan WebSocket
  5. `History` - Trading history dengan filter
  6. `Web` - WebView untuk web content
  7. `Profile` - User profile & theme settings
  8. `Debug` - DataStore debug info

#### **2.3 Bottom Navigation**
- **File:** `MainActivity.kt` (line 163-190)
- **Items:**
  - Trade (Chart Icon)
  - History (Clock Icon)
  - Web (Globe Icon)
  - Profile (Person Icon)
- **Features:**
  - ✅ Save/restore state saat tab switch
  - ✅ Visual indicator untuk active tab
  - ✅ Launch single top untuk prevent duplicate

#### **2.4 Screen Transitions**
- **Animation:** Slide + Fade
  - Enter: slideInHorizontally + fadeIn
  - Exit: slideOutHorizontally + fadeOut
  - Pop: reverse animations

---

### **3️⃣ TRADING FEATURES**

#### **3.1 Trading Terminal Screen**
- **File:** `TradeScreen.kt`, `TradeViewModel.kt`
- **Features:**
  - ✅ Connection status display untuk 2 WebSocket
  - ✅ Real-time price display (Z-CRY/IDX)
  - ✅ START/STOP CONNECTION button
  - ✅ Status badges (Connected/Connecting/Error)
  - ✅ Color-coded status (Green/Yellow/Red)

#### **3.2 WebSocket Managers**

**Main WebSocket (WS)**
- **File:** `WebSocketManager.kt`
- **URL:** `wss://ws.stockity.id/?v=2&vsn=2.0.0`
- **Features:**
  - ✅ Auto-connect saat START CONNECTION
  - ✅ Subscribe ke multiple topics:
    - connection, bo, marathon, user, account
    - tournament, cfd_zero_spread, asset
    - copy_trading, asset:Z-CRY/IDX
    - range_stream:Z-CRY/IDX
  - ✅ Heartbeat setiap 60 detik
  - ✅ Ping setiap 30 detik
  - ✅ Connection status tracking (Connected/Connecting/Error)
  - ✅ Message receiving

**Asset Socket (AS)**
- **File:** `AssetSocketManager.kt`
- **URL:** `wss://as.stockity.id/`
- **Features:**
  - ✅ Real-time price ticks untuk Z-CRY/IDX
  - ✅ Auto candle builder (5 detik per candle)
  - ✅ OHLC data collection (Open, High, Low, Close)
  - ✅ Max 100 candles history
  - ✅ Tick data dengan timestamp
  - ✅ JSON parsing dari stream

#### **3.3 Data Models**
```kotlin
data class AssetTick(
    val rate: Double,      // Harga
    val time: Long         // Timestamp
)

data class CandleData(
    val open: Double,      // Harga pembukaan
    val high: Double,      // Harga tertinggi
    val low: Double,       // Harga terendah
    val close: Double,     // Harga penutupan
    val time: Long         // Timestamp
)
```

---

### **4️⃣ HISTORY & TRADING RECORDS**

#### **4.1 History Screen**
- **File:** `HistoryScreen.kt`
- **Features:**
  - ✅ List trading history dengan mock data
  - ✅ Filter by status (All/Won/Lost/Tie)
  - ✅ Filter by pair (All/ASIA/X/USD/USDT)
  - ✅ Filter by account mode (All/Real/Demo)
  - ✅ Click item → Detail modal
  - ✅ Status badge dengan color coding
  - ✅ Profit/loss display dengan warna

#### **4.2 History Data Model**
```kotlin
data class HistoryItem(
    val id: Int,
    val pair: String,          // ASIA/X, USD/USDT
    val status: String,        // won, lost, tie
    val type: String,          // BUY, SELL
    val accountMode: String,   // Real, Demo
    val currency: String,      // IDR, USD
    val amount: Double,
    val profit: Double,
    val createdAt: Long
)
```

#### **4.3 Order Detail Bottom Sheet**
- ✅ Full order details display
- ✅ Formatted date/time
- ✅ Currency formatting (IDR dengan Locale)
- ✅ Dismissible modal

---

### **5️⃣ WEB VIEW**

#### **5.1 Web Screen**
- **File:** `WebScreen.kt`
- **Features:**
  - ✅ Load URL: `https://stockity.id/not-found`
  - ✅ JavaScript enabled
  - ✅ DOM storage enabled
  - ✅ Wide viewport
  - ✅ Chrome version check (minimum 74.0.3279.185)
  - ✅ Fallback UI jika Chrome too old
  - ✅ No reload saat recomposition

---

### **6️⃣ USER PROFILE & SETTINGS**

#### **6.1 Profile Screen**
- **File:** `ProfileScreen.kt`, `ProfileViewModel.kt`
- **Features:**
  - ✅ User profile icon display
  - ✅ Theme selector dropdown
  - ✅ 3 theme options:
    - System Default (auto follow device)
    - Light Mode
    - Dark Mode
  - ✅ Theme selection persistence

#### **6.2 Theme Management**
- **File:** `ThemeMode.kt`, `Theme.kt`
- **Features:**
  - ✅ Enum-based theme mode (SYSTEM_DEFAULT=0, LIGHT=1, DARK=2)
  - ✅ Dynamic color support (Android 12+)
  - ✅ Custom color scheme
  - ✅ Material 3 typography
  - ✅ Save theme preference ke DataStore
  - ✅ Apply theme globally

---

### **7️⃣ ONBOARDING FLOWS**

#### **7.1 User Agreement Screen**
- **File:** `UserAgreementScreen.kt`
- **Content:** Liability disclaimer
- **Features:**
  - ✅ Scrollable agreement text
  - ✅ "I Understand and Agree" button
  - ✅ "I Disagree - Exit App" button
  - ✅ Agree flag saved ke DataStore
  - ✅ Can't proceed tanpa agree

#### **7.2 Permission Screen**
- **File:** `PermissionScreen.kt`
- **Features:**
  - ✅ Request notification permission (Android 13+)
  - ✅ Request battery optimization exemption
  - ✅ App restrictions settings access
  - ✅ Real-time permission status check
  - ✅ Visual indicators (checkmark jika granted)
  - ✅ "Continue Anyway" button (dapat skip)

#### **7.3 Splash Screen**
- **File:** `MainActivity.kt` (line 87-99)
- **Features:**
  - ✅ Loading indicator
  - ✅ "AT ST" branding
  - ✅ Shown saat DataStore loading

---

### **8️⃣ DEBUG & DEVELOPMENT**

#### **8.1 Debug Screen**
- **File:** `DebugScreen.kt`
- **Features:**
  - ✅ Display semua DataStore values
  - ✅ Key-value listing
  - ✅ Real-time data updates
  - ✅ Accessible via bug icon di top bar
  - ✅ Helpful untuk debug cookie/token issues

---

### **9️⃣ NETWORKING & API**

#### **9.1 API Service**
- **File:** `ApiService.kt`
- **Endpoint:**
  - `POST /passport/v2/sign_in?locale=id` - Login dengan email/password/2FA

#### **9.2 API Client Setup**
- **File:** `ApiClient.kt`
- **Features:**
  - ✅ Retrofit configuration
  - ✅ OkHttp client setup
  - ✅ CookieJar for cookie management
  - ✅ AuthInterceptor untuk add headers
  - ✅ Logging interceptor (debug builds)
  - ✅ GSON converter
  - ✅ Enhanced CookieJar logging

#### **9.3 Auth Interceptor**
- **File:** `AuthInterceptor.kt`
- **Headers Added:**
  - `Device-Id` - UUID
  - `Device-Type` - "web"
  - `User-Agent` - Custom Chrome UA
  - `Authorization-Token` - Auth token (jika ada)
  - `Cookie` - Session + device cookies
- **Features:**
  - ✅ Auto-add ke semua HTTP requests
  - ✅ Fallback cookie format (device_id + device_type)
  - ✅ Logging untuk debug

#### **9.4 Data Models**
```kotlin
data class LoginRequest(
    val email: String,
    val password: String,
    @SerializedName("2fa_token") val two_fa_token: String? = null
)

data class LoginResponse(val data: LoginData)

data class LoginData(
    val authtoken: String,
    val user_id: String,
    val is_2fa_enabled: Boolean = false
)
```

---

### **🔟 DATA PERSISTENCE**

#### **10.1 DataStore Manager**
- **File:** `DataStoreManager.kt`
- **Stored Keys:**
  - `theme_mode` (Int) - Theme preference
  - `user_agreed` (Boolean) - User agreement flag
  - `permissions_shown` (Boolean) - Permission shown flag
  - `auth_token` (String) - API auth token
  - `is_2fa_enabled` (Boolean) - 2FA enabled flag
  - `device_id` (String) - Device UUID
  - `cookies` (String) - Session cookies
  - `two_fa_token` (String) - OTP token

#### **10.2 Features**
- ✅ Flow-based reactive updates
- ✅ Suspend functions untuk write
- ✅ Auto-generate device ID jika belum ada
- ✅ Expose semua data via `allData` flow

---

## 📊 Dependencies

### **Compose & UI**
- androidx.compose.ui:ui
- androidx.compose.material3:material3
- androidx.compose.material.icons.extended
- androidx.activity.compose:activity-compose
- androidx.navigation.compose:navigation-compose

### **Lifecycle & State**
- androidx.lifecycle:lifecycle-runtime-ktx
- androidx.lifecycle:lifecycle-runtime-compose
- androidx.lifecycle:lifecycle-viewmodel-compose

### **Data Persistence**
- androidx.datastore:datastore-preferences

### **Networking**
- com.squareup.retrofit2:retrofit
- com.squareup.retrofit2:converter-gson
- com.squareup.okhttp3:okhttp
- com.squareup.okhttp3:logging-interceptor

---

## 🔌 External APIs

### **Login Endpoint**
```
POST https://api.stockity.id/passport/v2/sign_in?locale=id
```

### **WebSocket Endpoints**
```
wss://ws.stockity.id/?v=2&vsn=2.0.0     (Main WS)
wss://as.stockity.id/                    (Asset Socket)
```

---

## 🎨 UI/UX Features

### **Theme System**
- ✅ Light mode dengan Purple40 primary
- ✅ Dark mode dengan Purple80 primary
- ✅ Dynamic colors (Android 12+)
- ✅ Material 3 design system

### **Icons**
- ✅ Material Icons Extended library
- ✅ Navigation icons (ShowChart, History, Web, Person)
- ✅ Debug icon (BugReport)
- ✅ Theme icons (BrightnessAuto, LightMode, DarkMode)

### **Animations**
- ✅ Screen transitions (slide + fade)
- ✅ Bottom sheet animations
- ✅ Loading spinners
- ✅ Visibility animations (AnimatedVisibility)

---

## 📱 Permissions

### **Requested**
- `INTERNET` - API & WebSocket
- `ACCESS_NETWORK_STATE` - Network status
- `POST_NOTIFICATIONS` - Notifications (Android 13+)
- `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` - Background running
- `FOREGROUND_SERVICE` - Foreground services
- `FOREGROUND_SERVICE_DATA_SYNC` - Data sync service

---

## 🔍 Known Features Status

| Feature | Status | Notes |
|---------|--------|-------|
| Login UI | ✅ Working | Email/password input |
| 2FA Detection | ✅ Working | OTP flow implemented |
| OTP Input | ✅ Working | 6-digit auto-focus |
| Theme Selection | ✅ Working | Persisted in DataStore |
| History Display | ✅ Working | Mock data with filters |
| WebView | ✅ Working | Chrome version checked |
| Navigation | ✅ Working | Bottom nav + back handling |
| Permissions | ✅ Working | Real-time status tracking |
| WebSocket (WS) | ⚠️ Issues | 401 Error - cookies not sent |
| WebSocket (AS) | ⚠️ Issues | 401 Error - cookies not sent |
| Cookie Handling | ⚠️ Issues | CookieJar not capturing Set-Cookie |
| Real-time Price | ❌ Blocked | Waiting for WebSocket fix |

---

## 📝 Summary

**Total Screens:** 8  
**Total ViewModels:** 3  
**Network Models:** 3  
**UI Components:** 30+  
**Animations:** Slide + Fade transitions  
**State Management:** Flow-based  
**Build Version:** 6.6.6  

**Main Issue:** WebSocket 401 Unauthorized - Cookies tidak berhasil di-persist/di-load dengan benar

