package com.at.coba.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "asset_choices")
data class AssetChoiceEntity(
    @PrimaryKey val ric: String,
    val label: String,
)
