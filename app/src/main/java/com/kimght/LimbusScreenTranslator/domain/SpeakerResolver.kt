package com.kimght.LimbusScreenTranslator.domain

data class ModelInfo(val name: String, val nickName: String)

data class ResolvedSpeaker(val name: String?, val title: String?)

object SpeakerResolver {

    fun resolve(
        teller: String?,
        title: String?,
        model: String?,
        modelTable: Map<String, ModelInfo>,
    ): ResolvedSpeaker {
        val mapped = model?.takeIf { it.isNotBlank() }?.let { modelTable[it] }
        val name = teller?.takeIf { it.isNotBlank() } ?: mapped?.name?.takeIf { it.isNotBlank() }
        val resolvedTitle =
            title?.takeIf { it.isNotBlank() } ?: mapped?.nickName?.takeIf { it.isNotBlank() }
        return ResolvedSpeaker(name = name, title = resolvedTitle)
    }
}
