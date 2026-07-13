package com.kimght.LimbusScreenTranslator.domain.model

object PackKey {
    const val SEPARATOR = '\u001F'

    fun of(sourceName: String, id: String): String = "$sourceName$SEPARATOR$id"
}
