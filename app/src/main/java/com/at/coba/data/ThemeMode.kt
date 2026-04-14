package com.at.coba.data

enum class ThemeMode(val storageValue: Int) {
    SYSTEM_DEFAULT(0),
    LIGHT(1),
    DARK(2);

    companion object {
        fun fromStorageValue(value: Int): ThemeMode {
            return entries.find { it.storageValue == value } ?: SYSTEM_DEFAULT
        }
    }
}
