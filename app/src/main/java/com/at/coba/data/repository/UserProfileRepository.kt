package com.at.coba.data.repository

import android.content.Context
import android.net.Uri
import com.at.coba.data.DataStoreManager
import com.at.coba.data.network.ApiClient
import com.at.coba.data.network.LoginData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import retrofit2.HttpException
import java.io.File

sealed class ProfileFetchResult {
    /**
     * @param avatarLocalCacheFailed true jika unduh ke file lokal gagal; UI tetap bisa memuat dari URL HTTPS.
     */
    data class Success(val avatarLocalCacheFailed: Boolean = false) : ProfileFetchResult()
    data class Failure(val httpCode: Int?, val message: String?) : ProfileFetchResult()
}

object UserProfileRepository {

    suspend fun syncAfterLogin(appContext: Context, loginData: LoginData) {
        val dm = DataStoreManager(appContext.applicationContext)
        val previousId = dm.getStoredUserId()
        val newId = loginData.user_id
        if (previousId != null && previousId != newId) {
            dm.clearProfileImageStorage()
        }
        dm.setUserId(newId)

        fetchAndSyncFullProfile(appContext)

        val remoteUrl = listOf(
            loginData.avatar_url,
            loginData.profile_photo_url,
            loginData.photo_url
        ).firstOrNull { !it.isNullOrBlank() }?.trim()

        if (!remoteUrl.isNullOrEmpty()) {
            val resolved = resolveAvatarUrl(remoteUrl)
            dm.setProfileRemoteAvatarUrl(resolved)
            dm.bumpProfileAvatarEpoch()
            if (!cacheAvatarFromRemoteUrl(appContext, resolved)) {
                dm.removeProfileLocalImageUriOnly()
            }
        }
    }

    /**
     * GET profile (Retrofit/OkHttp). Persists to Datastore only on 200 + parsed body.
     */
    suspend fun fetchAndSyncFullProfile(context: Context): ProfileFetchResult {
        return try {
            val apiService = ApiClient.getApiService(context)
            val profile = apiService.getProfile()
            val dm = DataStoreManager(context.applicationContext)

            dm.setUserProfileInfo(
                email = profile.data.email,
                phone = profile.data.phone,
                nickname = profile.data.nickname ?: profile.data.firstName,
                emailVerified = profile.data.emailVerified,
                phoneVerified = profile.data.phoneVerified,
                docsVerified = profile.data.docsVerified
            )

            dm.syncUserPersonalFieldsFromServer(
                firstName = profile.data.firstName,
                lastName = profile.data.lastName,
                genderRaw = profile.data.gender,
                birthdayIso = profile.data.birthday
            )

            val avatarRaw = profile.data.avatar?.trim().orEmpty()
            if (avatarRaw.isEmpty()) {
                dm.clearProfileImageStorage()
                ProfileFetchResult.Success(avatarLocalCacheFailed = false)
            } else {
                val resolved = resolveAvatarUrl(avatarRaw)
                dm.setProfileRemoteAvatarUrl(resolved)
                dm.bumpProfileAvatarEpoch()

                val cachedOk = cacheAvatarFromRemoteUrl(context, resolved)
                if (!cachedOk) {
                    dm.removeProfileLocalImageUriOnly()
                    ProfileFetchResult.Success(avatarLocalCacheFailed = true)
                } else {
                    ProfileFetchResult.Success(avatarLocalCacheFailed = false)
                }
            }
        } catch (e: HttpException) {
            ProfileFetchResult.Failure(e.code(), e.message())
        } catch (e: Exception) {
            ProfileFetchResult.Failure(null, e.message ?: e.toString())
        }
    }

    fun resolveAvatarUrl(raw: String): String {
        val t = raw.trim()
        if (t.startsWith("http://", ignoreCase = true) || t.startsWith("https://", ignoreCase = true)) {
            return t
        }
        if (t.startsWith("//")) return "https:$t"
        val base = "https://api.stockity.id"
        return if (t.startsWith("/")) "$base$t" else "$base/$t"
    }

    /**
     * Menyimpan avatar ke file internal. URL remote sudah harus ditulis pemanggil.
     * @return false jika status bukan 2xx atau pengecualian I/O.
     */
    suspend fun cacheAvatarFromRemoteUrl(appContext: Context, url: String): Boolean =
        withContext(Dispatchers.IO) {
            val ctx = appContext.applicationContext
            val dm = DataStoreManager(ctx)
            try {
                val client = ApiClient.getOkHttpClient(ctx)
                val request = Request.Builder().url(url).get().build()
                var success = false
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) return@use
                    val responseBody = response.body ?: return@use
                    val dest = File(ctx.filesDir, DataStoreManager.PROFILE_IMAGE_INTERNAL_FILE)
                    responseBody.byteStream().use { input ->
                        dest.outputStream().use { output -> input.copyTo(output) }
                    }
                    val fileUri = Uri.fromFile(dest).toString()
                    dm.setProfileImageUri(fileUri)
                    success = true
                }
                success
            } catch (_: Exception) {
                false
            }
        }

    suspend fun uploadCachedProfileAvatar(appContext: Context): Result<String> = withContext(Dispatchers.IO) {
        val ctx = appContext.applicationContext
        val dm = DataStoreManager(ctx)
        val file = File(ctx.filesDir, DataStoreManager.PROFILE_IMAGE_INTERNAL_FILE)
        if (!file.exists() || file.length() == 0L) {
            return@withContext Result.failure(IllegalStateException("No profile file to upload"))
        }
        val token = dm.authToken.first()
        if (token.isNullOrBlank()) {
            return@withContext Result.failure(IllegalStateException("Not authenticated"))
        }
        try {
            val body = file.asRequestBody("image/jpeg".toMediaType())
            val part = MultipartBody.Part.createFormData("avatar", file.name, body)
            val response = ApiClient.getApiService(ctx).uploadProfileAvatar(part)
            val url = response.data?.resolvedUrl()?.trim().orEmpty()
            if (url.isEmpty()) {
                return@withContext Result.failure(IllegalStateException("Empty avatar URL in response"))
            }
            dm.setProfileImageUri(url)
            dm.setProfileRemoteAvatarUrl(url)
            dm.bumpProfileAvatarEpoch()
            Result.success(url)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
