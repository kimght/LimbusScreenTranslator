package com.kimght.limbusscreentranslator.data.install

import com.kimght.limbusscreentranslator.data.network.dto.ModelCodesDto
import com.kimght.limbusscreentranslator.data.network.dto.ScenarioDto
import com.kimght.limbusscreentranslator.domain.ModelInfo
import com.kimght.limbusscreentranslator.domain.SpeakerResolver
import com.kimght.limbusscreentranslator.domain.model.DialogueLine

object ScenarioMapper {

    fun buildModelTable(codes: ModelCodesDto): Map<String, ModelInfo> =
        codes.dataList
            .filter { it.id.isNotBlank() }
            .associate { it.id to ModelInfo(name = it.name, nickName = it.nickName) }

    fun toDialogueLines(
        scenario: ScenarioDto,
        modelTable: Map<String, ModelInfo>,
    ): List<DialogueLine> {
        val out = ArrayList<DialogueLine>(scenario.dataList.size)
        var index = 0
        for (line in scenario.dataList) {
            val text = line.content
            if (text.isNullOrBlank()) continue // spacer — never displayed
            val speaker = SpeakerResolver.resolve(
                teller = line.teller,
                title = line.title,
                model = line.model,
                modelTable = modelTable,
            )
            out += DialogueLine(
                index = index,
                speakerName = speaker.name,
                title = speaker.title,
                place = line.place?.takeIf { it.isNotBlank() },
                text = text,
            )
            index++
        }
        return out
    }
}
