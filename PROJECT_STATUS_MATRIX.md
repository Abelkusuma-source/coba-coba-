# 📊 PROJECT COBA - Complete Status Matrix

## 🎬 Screen-by-Screen Breakdown

### **1. SPLASH SCREEN**
```
Purpose:      Load DataStore values & show branding
Status:       ✅ WORKING
Location:     MainActivity.kt (line 87-99)
Components:
  - AT ST branding text
  - Circular loading indicator
  - Center alignment
Behavior:
  - Shows while DataStore preferences loading
  - Auto dismiss once data loaded
  - No user interaction
```

---

### **2. LOGIN SCREEN**
```
Purpose:      Authenticate user with email + password (+ 2FA)
Status:       ✅ WORKING
Location:     LoginScreen.kt, LoginViewModel.kt
Components:
  - "Welcome" title
  - Email TextField (disabled during 2FA)
  - Password TextField (toggle show/hide)
  - OTP Input (6 fields, auto-focus)
  - Login Button
  - Verify Button
  - Error message display
Features:
  ✅ Email validation (required)
  ✅ Password validation (required, 8+ chars recommended)
  ✅ Show/hide password toggle
  ✅ Loading state (spinner on button)
  ✅ 2FA detection (code "2fa_required")
  ✅ OTP auto-focus to next field
  ✅ Backspace to previous field
  ✅ Error message display
  ✅ Success state triggers navigation
Endpoints:
  - POST /passport/v2/sign_in?locale=id
DataStore Updates:
  - auth_token ← authtoken from response
  - is_2fa_enabled ← flag from response
  - cookies ← Set-Cookie header (❌ NOT WORKING)
```

---

### **3. USER AGREEMENT SCREEN**
```
Purpose:      Get user consent before app access
Status:       ✅ WORKING
Location:     UserAgreementScreen.kt
Components:
  - "User Agreement" title
  - Scrollable liability text
  - "I Understand and Agree" button
  - "I Disagree - Exit App" button
Features:
  ✅ Scrollable content
  ✅ Can't proceed without agree
  ✅ Exit app button works
  ✅ Agree flag saved
DataStore Updates:
  - user_agreed ← true when clicked
Behavior:
  - Shown if user_agreed = false in DataStore
  - After agree, navigate to Permission screen
  - If disagree, exit app
```

---

### **4. PERMISSION SCREEN**
```
Purpose:      Request system permissions & optimizations
Status:       ✅ WORKING
Location:     PermissionScreen.kt
Components:
  - "Permissions" title
  - Notification permission item
  - Battery optimization item
  - App restrictions item
  - "Continue Anyway" button
Features:
  ✅ Real-time permission status check
  ✅ Visual checkmark when granted
  ✅ Green background for granted
  ✅ Auto-refresh every 1 second
  ✅ Can skip/continue anyway
  ✅ Click to open system settings
Permissions Requested:
  1. POST_NOTIFICATIONS (Android 13+)
  2. REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
  3. APP_RESTRICTIONS (custom)
DataStore Updates:
  - permissions_shown ← true when clicked
Behavior:
  - Shown if permissions_shown = false
  - After continue, navigate to Main app
  - Can skip if user doesn't grant
```

---

### **5. TRADE SCREEN** ⭐ Main Screen
```
Purpose:      Real-time trading terminal with WebSocket
Status:       ⚠️ PARTIAL (WebSocket failing)
Location:     TradeScreen.kt, TradeViewModel.kt
Components:
  - "Trading Terminal" title
  - Connection Status Card
    - Main WebSocket (WS) status
    - Asset Socket (AS) status
  - Price Display (Z-CRY/IDX)
  - START/STOP CONNECTION button
Features:
  ✅ Show connection status
  ✅ Color-coded badges (Green/Yellow/Red)
  ✅ Real-time status updates
  ✅ START/STOP button toggle
  ✅ Display current price (when connected)
  ❌ WebSocket connection fails (401 error)
  ❌ Real-time price not updating
WebSockets Used:
  1. Main WS: wss://ws.stockity.id/?v=2&vsn=2.0.0
     - Subscribe: connection, bo, marathon, user, account, tournament, etc
     - Heartbeat: 60s interval
     - Ping: 30s interval
  2. Asset WS: wss://as.stockity.id/
     - Subscribe: Z-CRY/IDX price ticks
     - Parse: rate, build candles (5s period)
Issues:
  ❌ 401 Unauthorized on both WebSockets
  ❌ Cookies not being sent (CookieJar issue)
  ❌ Auth token not being sent
  ❌ Cannot establish WebSocket connection
```

---

### **6. HISTORY SCREEN**
```
Purpose:      View trading history with filters
Status:       ✅ WORKING
Location:     HistoryScreen.kt
Components:
  - Filter dropdowns (Status, Pair, Account Mode)
  - History card list
  - Detail bottom sheet (modal)
Features:
  ✅ Filter by status (All/Won/Lost/Tie)
  ✅ Filter by pair (All/ASIA/X/USD/USDT)
  ✅ Filter by account mode (All/Real/Demo)
  ✅ Multi-filter support
  ✅ Card display with status badge
  ✅ Click card → Detail modal
  ✅ Date/time formatting
  ✅ Currency formatting (IDR/USD)
Data:
  - Mock data with 4 items
  - Real data would come from API
Detail Modal Shows:
  - Order pair
  - Order type (BUY/SELL)
  - Status (Won/Lost/Tie)
  - Account mode
  - Date & time
  - Total profit/loss
  - Currency
Behavior:
  - Always shows 4 mock items
  - Filters work on mock data
  - Bottom sheet dismissible
```

---

### **7. WEB SCREEN**
```
Purpose:      WebView for web content
Status:       ✅ WORKING
Location:     WebScreen.kt
Components:
  - WebView (AndroidView)
  - Fallback UI (if Chrome too old)
Features:
  ✅ Load https://stockity.id/not-found
  ✅ Chrome version detection
  ✅ Minimum Chrome version: 74.0.3279.185
  ✅ JavaScript enabled
  ✅ DOM storage enabled
  ✅ Wide viewport
  ✅ No reload on recomposition
Settings:
  - javaScriptEnabled = true
  - domStorageEnabled = true
  - loadWithOverviewMode = true
  - useWideViewPort = true
Fallback:
  - Shows error if Chrome < minimum version
  - Prompts user to update
```

---

### **8. PROFILE SCREEN**
```
Purpose:      User profile & app settings
Status:       ✅ WORKING
Location:     ProfileScreen.kt, ProfileViewModel.kt
Components:
  - Person icon (100dp)
  - "User Profile" title
  - Personalization card
  - Theme selector dropdown
Features:
  ✅ Display profile icon
  ✅ Theme selector (3 options)
  ✅ Show current theme
  ✅ Save theme selection
  ✅ Apply theme globally
Theme Options:
  1. System Default (BrightnessAuto icon)
     - Auto follow device theme
  2. Light Mode (LightMode icon)
     - Always light colors
  3. Dark Mode (DarkMode icon)
     - Always dark colors
DataStore Updates:
  - theme_mode ← SYSTEM_DEFAULT(0)/LIGHT(1)/DARK(2)
Behavior:
  - Theme saved immediately
  - Applied to all screens on next recomposition
  - Persists across app restarts
```

---

### **9. DEBUG SCREEN**
```
Purpose:      Development debugging & data inspection
Status:       ✅ WORKING
Location:     DebugScreen.kt
Access:       Via bug icon in top bar (any screen)
Components:
  - "DataStore Debug Info" title
  - Key-value card list (lazy column)
Features:
  ✅ Display all DataStore keys
  ✅ Display all DataStore values
  ✅ Real-time updates
  ✅ Scrollable list
  ✅ Card layout per entry
Data Shown:
  - device_id
  - auth_token
  - cookies (shows if empty!)
  - is_2fa_enabled
  - user_agreed
  - permissions_shown
  - two_fa_token (if set)
  - theme_mode
Very Useful For:
  - Debugging cookie issues
  - Verifying token storage
  - Checking flag states
  - DataStore persistence
```

---

## 🔄 ViewModel Breakdown

### **LoginViewModel**
```
Purpose:     Handle login & 2FA flow
Location:    LoginViewModel.kt
Manages:
  - Login request/response
  - 2FA detection
  - OTP verification
  - Error handling
  - Loading states
State:
  - uiState: Flow<LoginUiState>
    - Idle
    - Loading
    - Is2FARequired
    - Success
    - Error
Methods:
  - login(context, email, password)
    1. Send POST /passport/v2/sign_in
    2. Parse response
    3. Check for "2fa_required" code
    4. Save token to DataStore
    5. Emit Success or Is2FARequired state
  
  - verifyOtp(context, otpCode)
    1. Send POST /passport/v2/sign_in (with 2FA token)
    2. Save returned auth token
    3. Emit Success state
Factory Pattern:
  - LoginViewModel.Factory(dataStoreManager)
```

---

### **TradeViewModel**
```
Purpose:     Manage WebSocket connections & trading data
Location:    TradeViewModel.kt
Manages:
  - WebSocketManager lifecycle
  - AssetSocketManager lifecycle
  - Trading data flows
State:
  - wsStatus: StateFlow<WebSocketStatus>
  - asStatus: StateFlow<WebSocketStatus>
  - wsReceivedMessage: StateFlow<String?>
  - asReceivedMessage: StateFlow<String?>
  - tickData: StateFlow<AssetTick?>
  - candles: StateFlow<List<CandleData>>
Methods:
  - startConnection(context)
    1. Call webSocketManager.connect()
    2. Call assetSocketManager.connect()
  
  - stopConnection()
    1. Call webSocketManager.disconnect()
    2. Call assetSocketManager.disconnect()
  
  - onCleared()
    - Auto-disconnect when ViewModel destroyed
Factory Pattern:
  - TradeViewModel.Factory(dataStoreManager)
```

---

### **ProfileViewModel**
```
Purpose:     Manage theme selection & persistence
Location:    ProfileViewModel.kt
Manages:
  - Theme mode state
  - Theme persistence
State:
  - themeMode: StateFlow<ThemeMode>
    - Converted from DataStore Flow
    - Stateflow with lifecycle awareness
Methods:
  - onThemeSelected(mode: ThemeMode)
    1. Call dataStoreManager.setThemeMode(mode)
    2. Flow emits new value
    3. UI recomposes with new theme
Factory Pattern:
  - ProfileViewModel.Factory(dataStoreManager)
```

---

## 🏗️ Navigation Flow

```
Splash Screen (loading state)
        ↓
   [Check DataStore]
        ↓
   ┌────┴────────┬──────────────┬──────────────┐
   ↓             ↓              ↓              ↓
   No Token      Token OK       Token OK       Token OK
   │             │              │              │
   ↓             ↓              ↓              ↓
Login Screen    No Agreement   No Perm      Main App
   │             │              │              │
   │(2FA)        ↓              ↓              ├─→ Trade
   │        Agreement Screen   Permission    ├─→ History
   │             │             Screen        ├─→ Web
   │             ↓             │              ├─→ Profile
   │        Permission Screen  ↓              └─→ Debug
   │             │         Main App
   │             ↓             │
   └──→──────────┴─────────→───┴────→ [Main App Ready]

Within Main App:
  Bottom Nav:
    Trade ←→ History ←→ Web ←→ Profile
  
  Bug Icon (any screen):
    → Debug Screen
  
  Back Arrow (any screen):
    → Previous screen (or exit if top-level)
```

---

## 📡 Data Flow & State Management

```
User Action → ViewModel → Flow Emission → UI Recomposition

Example: Login
─────────────────────────────────────────────────────────
1. User clicks "Login" button
2. LoginViewModel.login(email, password) called
3. LoginViewModel sends POST request
4. Response received:
   - Update DataStore.auth_token
   - Update DataStore.cookies
   - Emit uiState.Success
5. MainActivity observes state change
6. Navigate to next screen

Example: Theme Selection
─────────────────────────────────────────────────────────
1. User selects theme from dropdown
2. ProfileViewModel.onThemeSelected(mode) called
3. DataStoreManager.setThemeMode(mode) called
4. DataStore.theme_mode updated
5. Theme Flow emits new value
6. MainActivity observes theme flow
7. CobaTheme(darkTheme) recomposes with new value
8. All screens reflect new theme

Example: WebSocket Price Update
─────────────────────────────────────────────────────────
1. TradeViewModel starts WebSocket connections
2. AssetSocketManager receives price tick
3. Parse JSON and extract rate
4. Update tickData StateFlow
5. TradeScreen observes tickData
6. UI recomposes with new price value
7. Display updates in real-time
```

---

## 🔐 Security & Auth

```
Token Management:
  - Stored in: DataStoreManager.auth_token (encrypted by system)
  - Sent via: AuthInterceptor to all API requests
  - Cleared on: DataStoreManager.clearAuthData()
  
Cookie Management:
  - Received from: Server Set-Cookie header
  - Stored in: DataStoreManager.cookies
  - Sent via: WebSocket headers + AuthInterceptor
  - Issue: CookieJar not capturing Set-Cookie properly ❌

Device Identification:
  - UUID: DataStoreManager.device_id (auto-generated)
  - Sent via: Device-Id header in all requests
  - Sent via: Device-Type = "web"
  - Sent via: User-Agent string

2FA Security:
  - OTP Token: Temporary, single-use
  - Not stored permanently
  - Only used to re-login and get auth token
```

---

## 📦 Architecture Layers

```
UI Layer (Screens)
├─ LoginScreen
├─ TradeScreen
├─ HistoryScreen
├─ WebScreen
├─ ProfileScreen
├─ UserAgreementScreen
├─ PermissionScreen
└─ DebugScreen

ViewModel Layer
├─ LoginViewModel
├─ TradeViewModel
└─ ProfileViewModel

Data Layer
├─ DataStoreManager (Preferences)
├─ WebSocketManager (WS data)
├─ AssetSocketManager (Price data)
├─ ApiClient (Retrofit)
└─ ApiService (Endpoints)

Network Layer
├─ Retrofit
├─ OkHttp
├─ Interceptors
│  ├─ AuthInterceptor
│  ├─ CookieJar
│  └─ Logging
└─ WebSocket (OkHttp)
```

---

## 🧪 Testing Scenarios

### **Scenario 1: Complete Login with 2FA**
```
1. Open app → Splash screen
2. See login form
3. Enter email + password
4. Click Login
5. Get 2FA required error
6. See OTP input fields
7. Enter 6 digit code
8. Click Verify
9. Get redirected to User Agreement (if first time)
10. Agree → Permission screen
11. Grant/skip permissions
12. Reach Main app (Trade tab)
```

### **Scenario 2: WebSocket Connection** ❌ FAILING
```
1. Login successfully
2. Token saved to DataStore ✅
3. Cookies NOT saved to DataStore ❌
4. Open Trade screen
5. Click "START CONNECTION"
6. WebSocketManager tries to connect
7. Sends headers WITH token ✅
8. Sends headers WITHOUT cookies ❌
9. Server rejects with 401 ❌
10. Status shows "Error: Expected HTTP 101..."
```

### **Scenario 3: Theme Change**
```
1. Open Profile screen
2. Open theme dropdown
3. Select "Dark Mode"
4. App immediately switches to dark theme
5. Go to any other screen
6. Theme persists
7. Close and reopen app
8. Theme still dark
```

---

**Total Lines of Code:** ~2000+  
**Total Screens:** 8  
**Total ViewModels:** 3  
**Total Network Models:** 3  
**Dependencies:** 15+  
**Main Issue:** WebSocket 401 - Cookie persistence broken

