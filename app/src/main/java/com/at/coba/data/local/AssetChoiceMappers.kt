package com.at.coba.data.local

import com.at.coba.data.repository.AssetChoice

fun AssetChoice.toEntity(): AssetChoiceEntity = AssetChoiceEntity(
    ric = ric,
    label = label,
)
