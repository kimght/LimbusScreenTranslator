package com.kimght.limbusscreentranslator.data.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.SocketPolicy
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File
import java.util.concurrent.TimeUnit

class OkHttpDownloaderTest {

    private lateinit var server: MockWebServer

    @Before
    fun setUp() {
        server = MockWebServer().also { it.start() }
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `cancelling the coroutine promptly aborts a stalled download`() = runBlocking {
        // Server accepts the connection but never responds, so execute() blocks on the socket.
        server.enqueue(MockResponse().setSocketPolicy(SocketPolicy.NO_RESPONSE))
        val client = OkHttpClient.Builder()
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
        val downloader = OkHttpDownloader(client)
        val dest = File.createTempFile("download", ".zip").also { it.deleteOnExit() }

        val job = launch(Dispatchers.IO) {
            downloader.download(server.url("/pack.zip").toString(), dest)
        }

        // Let the blocking call begin, then cancel. Call.cancel() must unblock it far sooner
        // than the 30s read timeout — a broken wiring would leave cancelAndJoin stuck.
        Thread.sleep(300)
        withTimeout(5_000) { job.cancelAndJoin() }

        assertTrue(job.isCancelled)
    }
}
