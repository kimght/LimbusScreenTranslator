package com.kimght.LimbusScreenTranslator.domain.model

data class Source(
    val name: String,
    val url: String,
) {
    val host: String
        get() = runCatching { java.net.URI(url).host }.getOrNull()?.removePrefix("www.") ?: url
}
