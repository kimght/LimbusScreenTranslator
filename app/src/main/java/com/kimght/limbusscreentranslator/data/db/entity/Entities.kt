package com.kimght.limbusscreentranslator.data.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "installed_pack")
data class InstalledPackEntity(
    @PrimaryKey val key: String,
    val id: String,
    val version: String,
    val sourceName: String,
    val name: String,
    val flag: String,
    val description: String,
    val installedAt: Long,
)

@Entity(tableName = "source")
data class SourceEntity(
    @PrimaryKey val name: String,
    val url: String,
)

@Entity(
    tableName = "scenario_line",
    primaryKeys = ["localizationId", "scenarioCode", "lineIndex"],
)
data class ScenarioLineEntity(
    val localizationId: String,
    val scenarioCode: String,
    val lineIndex: Int,
    val content: String,
    val speakerName: String?,
    val title: String?,
    val place: String?,
)

@Entity(
    tableName = "chapter",
    primaryKeys = ["sourceName", "position"],
)
data class ChapterEntity(
    val sourceName: String,
    val position: Int,
    val name: String,
    val subtitle: String,
)

@Entity(
    tableName = "chapter_episode",
    primaryKeys = ["sourceName", "chapterPosition", "position"],
    indices = [Index(value = ["sourceName", "chapterPosition"])],
)
data class ChapterEpisodeEntity(
    val sourceName: String,
    val chapterPosition: Int,
    val position: Int,
    val episodeCode: String,
)

@Entity(
    tableName = "episode_progress",
    primaryKeys = ["localizationId", "episodeCode"],
)
data class EpisodeProgressEntity(
    val localizationId: String,
    val episodeCode: String,
    val viewedAt: Long,
)

@Entity(tableName = "reading_state")
data class ReadingStateEntity(
    @PrimaryKey val localizationId: String,
    val currentEpisode: String?,
    val recentEpisode: String?,
    val lineIndex: Int,
)
