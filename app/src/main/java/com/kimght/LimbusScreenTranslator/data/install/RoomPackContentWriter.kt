package com.kimght.LimbusScreenTranslator.data.install

import androidx.room.withTransaction
import com.kimght.LimbusScreenTranslator.data.db.LimbusDatabase
import com.kimght.LimbusScreenTranslator.data.db.entity.InstalledPackEntity
import com.kimght.LimbusScreenTranslator.data.db.entity.ScenarioLineEntity
import com.kimght.LimbusScreenTranslator.domain.model.InstalledPack
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RoomPackContentWriter @Inject constructor(
    private val db: LimbusDatabase,
) : PackContentWriter {

    private companion object {
        const val INSERT_CHUNK = 2000
    }

    override suspend fun replacePack(pack: InstalledPack, scenarios: List<ScenarioContent>) {
        db.withTransaction {
            val scenarioDao = db.scenarioDao()
            scenarioDao.deleteForLocalization(pack.key)

            val rows = scenarios.flatMap { scenario ->
                scenario.lines.map { line ->
                    ScenarioLineEntity(
                        localizationId = pack.key,
                        scenarioCode = scenario.code,
                        lineIndex = line.index,
                        content = line.text,
                        speakerName = line.speakerName,
                        title = line.title,
                        place = line.place,
                    )
                }
            }
            rows.chunked(INSERT_CHUNK).forEach { scenarioDao.insertLines(it) }

            db.installedPackDao().upsert(
                InstalledPackEntity(
                    key = pack.key,
                    id = pack.id,
                    version = pack.version,
                    sourceName = pack.sourceName,
                    name = pack.name,
                    flag = pack.flag,
                    description = pack.description,
                    installedAt = pack.installedAt,
                ),
            )
        }
    }

    override suspend fun deletePack(id: String) {
        db.withTransaction {
            db.scenarioDao().deleteForLocalization(id)
            db.overlayStateDao().deleteProgress(id)
            db.overlayStateDao().deleteReadingState(id)
            db.installedPackDao().delete(id)
        }
    }
}
