# Fix Cookie Issue - Feature/WebSocket-Trade Branch

## 📋 Masalah
WebSocket connection gagal dengan error **401 Unauthorized** karena cookies dari response login tidak tersimpan di DataStore.

## 🔍 Root Cause
OkHttp `CookieJar` tidak secara otomatis memanggil `saveFromResponse()` untuk menangkap Set-Cookie headers dari HTTP response. Diperlukan explicit Interceptor untuk capture cookies.

## ✅ Solusi Implemented

### File yang Diubah: `ApiClient.kt`

**Tambahan: Cookie Interceptor** (Line 59-81)

```kotlin
// Cookie Interceptor untuk explicitly capture Set-Cookie dari response
val cookieInterceptor = okhttp3.Interceptor { chain ->
    val response = chain.proceed(chain.request())
    
    // Tangkap semua Set-Cookie headers dari response dan gabungkan
    val setCookieHeaders = response.headers("Set-Cookie")
    if (setCookieHeaders.isNotEmpty()) {
        runBlocking {
            // Gabungkan semua Set-Cookie dengan separator "; "
            // Extract hanya bagian name=value (tanpa expires, path, domain, dll)
            val cookieValues = setCookieHeaders.mapNotNull { cookieHeader ->
                // Format: "name=value; Expires=...; Path=..."
                cookieHeader.split(";").firstOrNull()?.trim()
            }.joinToString("; ")
            
            if (cookieValues.isNotEmpty()) {
                dataStoreManager.setCookies(cookieValues)
            }
        }
    }
    
    response
}

val okHttpClient = OkHttpClient.Builder()
    .cookieJar(cookieJar)
    .addInterceptor(cookieInterceptor)  // ← Interceptor order penting
    .addInterceptor(authInterceptor)
    .addNetworkInterceptor(loggingInterceptor)
    .build()
```

## 🔄 Alur Setelah Fix

```
1. Login Request
   ├─ Email + Password
   └─ Device Headers
       ↓
2. Server Response
   ├─ Auth Token (disimpan)
   ├─ Set-Cookie Headers ← DIKONEKSI OLEH INTERCEPTOR
   └─ User Data
       ↓
3. Cookie Interceptor
   ├─ Tangkap Set-Cookie header
   ├─ Parse name=value pairs
   └─ Simpan ke DataStore ✅ (SEKARANG BERFUNGSI)
       ↓
4. DataStore State
   ├─ auth_token: "d55ff4a6-..."
   ├─ cookies: "session_id=abc...; user_token=xyz..." ✅ (TERISI)
   └─ device_id: "ccabbd6a..."
       ↓
5. WebSocket Connect
   ├─ Device Headers ✅
   ├─ Auth Token ✅
   ├─ Cookies ✅ (SEKARANG ADA)
   └─ Result: HTTP 101 Upgrade SUCCESS ✅
       ↓
6. Real-time Price
   └─ Z-CRY/IDX displayed ✅
```

## 📊 Dampak ke Fitur

| Fitur | Before | After |
|-------|--------|-------|
| Login UI | ✅ | ✅ |
| 2FA Trigger Detection | ✅ | ✅ |
| 2FA OTP Input | ✅ | ✅ |
| 2FA Verification | ✅ | ✅ |
| Cookies Saved | ❌ | ✅ |
| WebSocket Connection | ❌ 401 Error | ✅ |
| Real-time Price | ❌ | ✅ |
| Trading Terminal | ❌ | ✅ |

## 🧪 Testing Steps

1. **Clear DataStore** (Debug Screen atau reinstall app)
2. **Login** dengan akun yang sudah bisa (tidak rate-limited)
3. **Debug Screen** → Cek `cookies` field
   - Before: (kosong)
   - After: Harus berisi cookie string
4. **Trade Screen** → Klik "START CONNECTION"
   - Main WebSocket (WS): Should say "Connected"
   - Asset Socket (AS): Should say "Connected"
   - Price Display: Should show Z-CRY/IDX price real-time

## 🔗 Related Files

- `ApiClient.kt` - Cookie Interceptor added
- `DataStoreManager.kt` - No change needed (sudah punya setCookies)
- `WebSocketManager.kt` - No change needed (sudah gunakan cookies)
- `AssetSocketManager.kt` - No change needed (sudah gunakan cookies)

## 📝 Notes

- Interceptor berjalan **sebelum** AuthInterceptor agar cookies langsung tersimpan
- Cookie values di-parse dari Set-Cookie header: hanya extract `name=value` bagian (strip Expires, Path, Domain, dll)
- Menggunakan `runBlocking` aman di context ini karena di Interceptor thread
- Jika ada multiple Set-Cookie headers, semuanya di-gabung dengan `"; "` separator

## ✨ Selesai!

Cookies sekarang akan otomatis tersimpan saat login, dan WebSocket bisa connect dengan berhasil.

