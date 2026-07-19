package com.kimght.limbusscreentranslator.domain.model

enum class PackFormat {
    COMPATIBLE,
    NEW,
    AUTO,
    UNKNOWN,
    ;

    companion object {
        fun fromManifest(value: String): PackFormat = when (value.trim().lowercase()) {
            "compatible" -> COMPATIBLE
            "new" -> NEW
            "auto" -> AUTO
            else -> UNKNOWN
        }
    }
}
