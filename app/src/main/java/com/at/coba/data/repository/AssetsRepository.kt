package com.at.coba.data.repository

import android.content.Context
import com.at.coba.data.network.ApiClient

/** Daftar pasangan untuk filter/UI; nilai utama = **ric** (selaras dengan asset_ric deals). */
data class AssetChoice(
    /** Identitas untuk filter/pemilihan socket (biasanya sama dengan API `ric`). */
    val ric: String,
    /** Teks dropdown: name (ric). */
    val label: String
)

object AssetsRepository {

    /**
     * Sama seperti filter Pair di History: gabung ric dari API dan baris deal, unik,
     * lalu urut leksografis (**ric**).
     */
    fun mergeSortedRicLists(apiRics: List<String>, additionalRics: List<String>): List<String> {
        return (apiRics.asSequence() + additionalRics.asSequence())
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinct()
            .sorted()
            .toList()
    }

    suspend fun fetchChoices(context: Context, locale: String = "id"): Result<List<AssetChoice>> {
        return try {
            val envelope = ApiClient.getApiService(context).getAssets(locale)
            val raw = envelope.data?.assets.orEmpty().mapNotNull { a ->
                val ric = a.ric?.trim()?.takeIf { it.isNotEmpty() } ?: return@mapNotNull null
                val namePart = a.name?.trim()?.takeIf { it.isNotEmpty() }
                val label = if (namePart != null) "$namePart ($ric)" else ric
                AssetChoice(ric = ric, label = label)
            }.distinctBy { it.ric }
                .sortedBy { it.label.lowercase() }
            Result.success(raw)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
