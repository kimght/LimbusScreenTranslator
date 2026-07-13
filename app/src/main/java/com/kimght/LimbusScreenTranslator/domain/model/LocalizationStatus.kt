package com.kimght.LimbusScreenTranslator.domain.model

enum class LocalizationStatus {
    NOT_INSTALLED,
    INSTALLING,
    INSTALLED,
    ACTIVE,
    UPDATE_AVAILABLE,
    ;

    companion object {
        fun of(
            installedVersion: String?,
            manifestVersion: String,
            isActive: Boolean,
            isInstalling: Boolean,
        ): LocalizationStatus = when {
            isInstalling -> INSTALLING
            installedVersion == null -> NOT_INSTALLED
            installedVersion != manifestVersion -> UPDATE_AVAILABLE
            isActive -> ACTIVE
            else -> INSTALLED
        }
    }
}

fun hasUpdate(installedVersion: String?, manifestVersion: String): Boolean =
    installedVersion != null && installedVersion != manifestVersion
