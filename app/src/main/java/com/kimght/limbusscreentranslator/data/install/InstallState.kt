package com.kimght.limbusscreentranslator.data.install

enum class InstallError {
    UNKNOWN_FORMAT,
    DOWNLOAD_FAILED,
    SIZE_MISMATCH,
    EXTRACTION_FAILED,
    LANGUAGE_DIR_NOT_FOUND,
    PARSE_FAILED,
    PERSIST_FAILED,
}

sealed interface InstallState {
    data object Idle : InstallState
    data class Downloading(val percent: Int) : InstallState
    data object Verifying : InstallState
    data object Extracting : InstallState
    data object Persisting : InstallState
    data object Done : InstallState
    data class Failed(val reason: InstallError) : InstallState
}
