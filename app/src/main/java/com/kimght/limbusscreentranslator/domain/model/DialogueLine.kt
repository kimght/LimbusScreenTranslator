package com.kimght.limbusscreentranslator.domain.model

data class DialogueLine(
    val index: Int,
    val speakerName: String?,
    val title: String?,
    val place: String?,
    val text: String,
)
