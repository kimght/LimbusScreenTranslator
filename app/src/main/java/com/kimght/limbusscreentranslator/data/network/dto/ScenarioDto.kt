package com.kimght.limbusscreentranslator.data.network.dto

import kotlinx.serialization.Serializable

@Serializable
data class ScenarioDto(
    val dataList: List<LineDto> = emptyList(),
)

@Serializable
data class LineDto(
    val id: String? = null,
    val content: String? = null,
    val model: String? = null,
    val teller: String? = null,
    val title: String? = null,
    val place: String? = null,
)

@Serializable
data class ModelCodesDto(
    val dataList: List<ModelCodeDto> = emptyList(),
)

@Serializable
data class ModelCodeDto(
    val id: String = "",
    val name: String = "",
    val nickName: String = "",
)
