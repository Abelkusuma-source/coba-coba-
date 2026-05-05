package com.at.coba.data.network

import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * Menyelaraskan `expire_at` dengan aturan turbo:
 * anchor = menit penuh berikutnya pada detik 0, atau +2 menit jika detik saat ini ≥ 30;
 * `expire_at` epoch detik = anchor + interval.
 * Waktu calendar memakai [ZoneId.systemDefault].
 */
object BoExpireAtCalculator {

    fun expireAtEpochSeconds(intervalSeconds: Int, nowMillis: Long = System.currentTimeMillis()): Long {
        val interval = intervalSeconds.coerceAtLeast(1).toLong()
        val zone = ZoneId.systemDefault()
        val now = ZonedDateTime.ofInstant(Instant.ofEpochMilli(nowMillis), zone)
        val baseOfCurrentMinute = now.withSecond(0).withNano(0)
        val anchor = if (now.second < 30) {
            baseOfCurrentMinute.plusMinutes(1)
        } else {
            baseOfCurrentMinute.plusMinutes(2)
        }
        return anchor.plusSeconds(interval).toEpochSecond()
    }
}
