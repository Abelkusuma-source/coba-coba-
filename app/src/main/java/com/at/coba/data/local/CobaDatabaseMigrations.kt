package com.at.coba.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `asset_choices` (
                `ric` TEXT NOT NULL PRIMARY KEY,
                `label` TEXT NOT NULL
            )
            """.trimIndent(),
        )
    }
}
