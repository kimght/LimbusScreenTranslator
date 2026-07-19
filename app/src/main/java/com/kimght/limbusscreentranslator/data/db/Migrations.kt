package com.kimght.limbusscreentranslator.data.db

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        for (table in listOf("scenario_line", "episode_progress", "reading_state")) {
            db.execSQL(
                """
                UPDATE $table SET localizationId =
                  (SELECT sourceName || char(31) || id FROM installed_pack
                   WHERE installed_pack.id = $table.localizationId)
                WHERE localizationId IN (SELECT id FROM installed_pack)
                """.trimIndent(),
            )
        }
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `installed_pack_new` (
              `key` TEXT NOT NULL, `id` TEXT NOT NULL, `version` TEXT NOT NULL,
              `sourceName` TEXT NOT NULL, `name` TEXT NOT NULL, `flag` TEXT NOT NULL,
              `description` TEXT NOT NULL, `installedAt` INTEGER NOT NULL,
              PRIMARY KEY(`key`)
            )
            """.trimIndent(),
        )
        db.execSQL(
            """
            INSERT INTO installed_pack_new (`key`, id, version, sourceName, name, flag, description, installedAt)
            SELECT sourceName || char(31) || id, id, version, sourceName, '', '', '', installedAt
            FROM installed_pack
            """.trimIndent(),
        )
        db.execSQL("DROP TABLE installed_pack")
        db.execSQL("ALTER TABLE installed_pack_new RENAME TO `installed_pack`")
    }
}

// (localizationId, scenarioCode) is a strict prefix of the primary key, so the PK's
// index already serves every query this one could.
val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("DROP INDEX IF EXISTS `index_scenario_line_localizationId_scenarioCode`")
    }
}
