package com.kimght.LimbusScreenTranslator.data.install

import com.kimght.LimbusScreenTranslator.data.network.Downloader
import com.kimght.LimbusScreenTranslator.domain.model.InstalledPack
import com.kimght.LimbusScreenTranslator.domain.model.Localization
import com.kimght.LimbusScreenTranslator.domain.model.PackFormat
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.flowOn
import java.io.File
import java.net.URLEncoder

class PackInstaller(
    private val downloader: Downloader,
    private val writer: PackContentWriter,
    private val cacheRoot: File,
    private val clock: () -> Long = System::currentTimeMillis,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {

    fun install(localization: Localization, sourceName: String): Flow<InstallState> = channelFlow {
        val workDir = File(
            File(cacheRoot, "install"),
            URLEncoder.encode(sourceName, "UTF-8") + "-" + URLEncoder.encode(
                localization.id,
                "UTF-8"
            ),
        )
        try {
            if (localization.format == PackFormat.UNKNOWN) {
                send(InstallState.Failed(InstallError.UNKNOWN_FORMAT))
                return@channelFlow
            }

            workDir.deleteRecursively()
            workDir.mkdirs()

            send(InstallState.Downloading(0))
            val zip = File(workDir, "pack.zip")
            val written = try {
                downloader.download(localization.downloadUrl, zip) { read ->
                    trySend(InstallState.Downloading(percentOf(read, localization.sizeBytes)))
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                send(InstallState.Failed(InstallError.DOWNLOAD_FAILED))
                return@channelFlow
            }

            send(InstallState.Verifying)
            if (localization.sizeBytes > 0 && written != localization.sizeBytes) {
                send(InstallState.Failed(InstallError.SIZE_MISMATCH))
                return@channelFlow
            }

            send(InstallState.Extracting)
            val extractDir = File(workDir, "extracted")
            try {
                ZipUtils.extract(zip, extractDir)
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                send(InstallState.Failed(InstallError.EXTRACTION_FAILED))
                return@channelFlow
            }
            val languageDir = ZipUtils.locateLanguageDir(extractDir, localization.format)
            if (languageDir == null) {
                send(InstallState.Failed(InstallError.LANGUAGE_DIR_NOT_FOUND))
                return@channelFlow
            }

            send(InstallState.Persisting)
            val scenarios = try {
                PackContentParser.parse(languageDir)
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                send(InstallState.Failed(InstallError.PARSE_FAILED))
                return@channelFlow
            }
            val pack = InstalledPack(
                id = localization.id,
                version = localization.version,
                sourceName = sourceName,
                installedAt = clock(),
                name = localization.name,
                flag = localization.flag,
                description = localization.description,
            )
            try {
                writer.replacePack(pack, scenarios)
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                send(InstallState.Failed(InstallError.PERSIST_FAILED))
                return@channelFlow
            }

            send(InstallState.Done)
        } finally {
            workDir.deleteRecursively()
        }
    }.flowOn(ioDispatcher)

    private fun percentOf(read: Long, total: Long): Int =
        if (total > 0) ((read * 100) / total).toInt().coerceIn(0, 100) else 0
}
