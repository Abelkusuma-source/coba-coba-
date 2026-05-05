# AGENTS.md

## Architecture Overview
This is a Jetpack Compose Android app with a single-activity architecture using Navigation Compose. The app follows MVVM pattern with ViewModels for each screen.

**Key Components:**
- `MainActivity`: Entry point with conditional rendering based on auth state, user agreement, and permissions
- `DataStoreManager`: Handles all persistent data (theme, auth tokens, user preferences)
- Navigation: Bottom navigation bar with Trade/History/Web/Profile, plus Debug screen accessible via top bar icon
- Networking: Retrofit for API calls, WebSocket managers for real-time data

**Data Flow:**
- App starts with splash screen while loading DataStore values
- Conditional navigation: Login → User Agreement → Permissions → Main screens
- Auth token drives navigation state changes
- Theme mode persists and applies globally

## Critical Workflows

### Building & Running
- Use `./gradlew assembleDebug` for debug APK
- Use `./gradlew installDebug` to install on connected device/emulator
- BuildConfig is enabled for accessing build info (see MainScreen for version display)

### Authentication Flow
- Login via `/passport/v2/sign_in` endpoint with device ID
- If the server returns `2fa_required`, the app calls `/passport/v1/2fa/validate/otp?locale=id`, then signs in again with the temporary `2fa_token` in the login body
- The interim OTP token from validate-otp exists only in memory for that request chain; it is **not** persisted in DataStore (only the final auth token and cookies are stored after successful login)
- While waiting for OTP, email/password are held in `SavedStateHandle` so process death does not force a full re-login unless the user clears app data
- Clears auth data on logout via `DataStoreManager.clearAuthData()`

### Theme Management
- Three modes: LIGHT(1), DARK(2), SYSTEM_DEFAULT(0)
- Persisted in DataStore, applied via `CobaTheme(darkTheme = ...)`
- Updated in Profile screen via ViewModel

### Navigation Patterns
- Bottom nav items: Trade, History, Web, Profile
- Debug screen accessible from any screen via top bar bug icon (tabs: DataStore, Room DB, Bot DB); optional routes `debug_db` and `debug_bot_db` for full-screen inspectors
- Back navigation uses `navController.navigateUp()`
- Screen transitions use slide + fade animations

## Project Conventions

### Dependency Management
- Uses version catalog (`gradle/libs.versions.toml`)
- References like `libs.androidx.compose.ui` in build.gradle.kts
- Plugins: `libs.plugins.android.application`, `libs.plugins.kotlin.compose`

### Data Persistence
- App preferences and session: DataStore Preferences
- **Two separate Room databases** with clear entity boundaries:
  - `CobaDatabase` (`coba.db`): server-synced data — `trade_deals` (REST API history cache; includes optional `serverUuid` for deal correlation) and `asset_choices`
  - `BotDatabase` (`bot.db`): local bot data — `bot_deals` (deals placed by TradingEngine with indicator snapshots and WS reply status)
- **History screen** merges server `trade_deals` with `bot_deals` (same list, sorted by time). Rows from the bot DB show a **Bot** badge (Android icon). Won/loss for bot rows is resolved when a server deal with the same `serverUuid` exists after history sync (`BotDealPresentation`).
- Singleton providers: `DatabaseProvider` for `CobaDatabase`, `BotDatabaseProvider` for `BotDatabase`
- Keys defined as constants in `DataStoreManager.Companion`
- Flows for reactive UI updates, suspend functions for writes
- Device ID auto-generated if missing

### Networking
- ApiService interface for Retrofit endpoints
- **Cookie Interceptor** in ApiClient explicitly captures Set-Cookie headers from login response
- AuthInterceptor adds Device-Id, Device-Type, Authorization-Token to all requests
- WebSocket managers for asset data and general connections
- OkHttp logging interceptor for debug builds

### UI Patterns
- Screens in `ui/screens/` with corresponding ViewModels
- ViewModels created with factory pattern: `viewModel(factory = XxxViewModel.Factory(dataStoreManager))` or `AbstractSavedStateViewModelFactory` where `SavedStateHandle` is needed (e.g. `LoginViewModel`)
- State collected with `collectAsStateWithLifecycle()`
- Icons from Material Icons, content descriptions provided

### File Structure
- `data/`: DataStoreManager, network clients, models
- `ui/`: Navigation, screens/, theme/
- Screens include both composable and ViewModel in same file
- Theme files: Color.kt, Theme.kt, Type.kt (standard Compose setup)

## Integration Points
- External API: Login endpoint at `/passport/v2/sign_in?locale=id`
- WebSockets: AssetSocketManager, WebSocketManager for real-time data
- Permissions: INTERNET, NETWORK_STATE, POST_NOTIFICATIONS, battery optimization, foreground services
- Data extraction/backup rules defined in XML resources

## Debugging
- Debug screen accessible via top bar icon on any screen
- Three tabs: **DataStore**, **Room DB** (server history), **Bot DB** (bot deal history)
- DataStore values exposed via `DataStoreManager.allData` flow
- Room tables `trade_deals` and `asset_choices` visible in Debug → Room DB tab or `Screen.DebugDb` route
- Bot table `bot_deals` visible in Debug → Bot DB tab or `Screen.DebugBotDb` route (strategy-specific indicator lines, lifecycle status open/won/awaiting, WS reply; settlement line when matched to `trade_deals` by UUID)
- Build version displayed in top bar (fetched from packageManager)
- OkHttp logging for network requests
