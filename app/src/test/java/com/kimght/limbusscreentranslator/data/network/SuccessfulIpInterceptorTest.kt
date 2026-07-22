package com.kimght.limbusscreentranslator.data.network

import okhttp3.Dns
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.SocketPolicy
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import okio.Buffer
import okio.GzipSink
import okio.buffer
import java.io.IOException
import java.net.InetAddress
import java.net.Proxy
import java.util.concurrent.TimeUnit

class SuccessfulIpInterceptorTest {

    private lateinit var server: MockWebServer

    @Before
    fun setUp() {
        server = MockWebServer().also { it.start(WORKING_ADDRESS, 0) }
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `connection falls back to the next address and caches the working route`() {
        server.enqueue(MockResponse().setResponseCode(200).setBody("pack"))
        val store = InMemoryPreferredIpStore()

        client(store, listOf(BLOCKED_ADDRESS, WORKING_ADDRESS))
            .newCall(request())
            .execute()
            .use { response -> assertEquals("pack", response.body.string()) }

        assertEquals(WORKING_ADDRESS.hostAddress, store.getPreferredIp(HOSTNAME))
    }

    @Test
    fun `final HTTP error does not cache its route`() {
        server.enqueue(MockResponse().setResponseCode(503).setBody("unavailable"))
        val store = InMemoryPreferredIpStore()

        client(store, listOf(WORKING_ADDRESS))
            .newCall(request())
            .execute()
            .close()

        assertNull(store.getPreferredIp(HOSTNAME))
    }

    @Test
    fun `response body failure does not cache its route`() {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("x".repeat(8_192))
                .setSocketPolicy(SocketPolicy.DISCONNECT_DURING_RESPONSE_BODY)
        )
        val store = InMemoryPreferredIpStore()

        assertThrows(IOException::class.java) {
            client(store, listOf(WORKING_ADDRESS))
                .newCall(request())
                .execute()
                .use { response -> response.body.string() }
        }
        assertNull(store.getPreferredIp(HOSTNAME))
    }

    @Test
    fun `fully consuming a transparently gzip encoded response caches its route`() {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Encoding", "gzip")
                .setBody(gzip("pack"))
        )
        val store = InMemoryPreferredIpStore()

        client(store, listOf(WORKING_ADDRESS))
            .newCall(request())
            .execute()
            .use { response -> assertEquals("pack", response.body.string()) }

        assertEquals(WORKING_ADDRESS.hostAddress, store.getPreferredIp(HOSTNAME))
    }

    @Test
    fun `closing a gzip response before decoded EOF does not cache its route`() {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Encoding", "gzip")
                .setBody(gzip("decoded pack content".repeat(1_024)))
        )
        val store = InMemoryPreferredIpStore()

        client(store, listOf(WORKING_ADDRESS))
            .newCall(request())
            .execute()
            .use { response -> response.body.source().readByte() }

        assertNull(store.getPreferredIp(HOSTNAME))
    }

    @Test
    fun `truncated gzip response that fails decoded reading does not cache its route`() {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Encoding", "gzip")
                .setBody(gzip("pack", trailerBytesToRemove = 8))
        )
        val store = InMemoryPreferredIpStore()

        assertThrows(IOException::class.java) {
            client(store, listOf(WORKING_ADDRESS))
                .newCall(request())
                .execute()
                .use { response -> response.body.string() }
        }
        assertNull(store.getPreferredIp(HOSTNAME))
    }

    @Test
    fun `closing a successful response before body EOF does not cache its route`() {
        server.enqueue(MockResponse().setResponseCode(200).setBody("x".repeat(8_192)))
        val store = InMemoryPreferredIpStore()

        client(store, listOf(WORKING_ADDRESS))
            .newCall(request())
            .execute()
            .use { response -> response.body.source().readByte() }

        assertNull(store.getPreferredIp(HOSTNAME))
    }

    @Test
    fun `canceling after successful headers before body EOF does not cache its route`() {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("x".repeat(8_192))
                .throttleBody(1, 1, TimeUnit.SECONDS)
        )
        val store = InMemoryPreferredIpStore()
        val call = client(store, listOf(WORKING_ADDRESS)).newCall(request())
        val response = call.execute()

        call.cancel()
        response.close()

        assertNull(store.getPreferredIp(HOSTNAME))
    }

    @Test
    fun `successful zero length response caches its route`() {
        server.enqueue(MockResponse().setResponseCode(204))
        val store = InMemoryPreferredIpStore()

        client(store, listOf(WORKING_ADDRESS))
            .newCall(request())
            .execute()
            .close()

        assertEquals(WORKING_ADDRESS.hostAddress, store.getPreferredIp(HOSTNAME))
    }

    @Test
    fun `successful response through a proxy does not cache its route`() {
        server.enqueue(MockResponse().setResponseCode(200).setBody("pack"))
        val store = InMemoryPreferredIpStore()

        client(
            store = store,
            addresses = listOf(WORKING_ADDRESS),
            proxy = server.toProxyAddress(),
        )
            .newCall(request())
            .execute()
            .use { response -> assertEquals("pack", response.body.string()) }

        assertNull(store.getPreferredIp(HOSTNAME))
    }

    @Test
    fun `successful request is not failed by a cache write error`() {
        server.enqueue(MockResponse().setResponseCode(200).setBody("manifest"))
        val store = object : PreferredIpStore {
            override fun getPreferredIp(hostname: String): String? = null
            override fun setPreferredIp(hostname: String, ipAddress: String) {
                error("cache unavailable")
            }
        }

        client(store, listOf(WORKING_ADDRESS))
            .newCall(request())
            .execute()
            .use { response -> assertEquals("manifest", response.body.string()) }
    }

    private fun client(
        store: PreferredIpStore,
        addresses: List<InetAddress>,
        proxy: Proxy = Proxy.NO_PROXY,
    ): OkHttpClient {
        val delegate = object : Dns {
            override fun lookup(hostname: String): List<InetAddress> = addresses
        }
        val routes = SuccessfulIpRoutes()
        return OkHttpClient.Builder()
            .dns(PreferredIpDns(delegate, store))
            .proxy(proxy)
            .eventListenerFactory(SuccessfulIpEventListenerFactory(routes))
            .addInterceptor(SuccessfulIpInterceptor(store, routes))
            .connectTimeout(500, TimeUnit.MILLISECONDS)
            .readTimeout(2, TimeUnit.SECONDS)
            .build()
    }

    private fun request(): Request = Request.Builder()
        .url(server.url("/").newBuilder().host(HOSTNAME).build())
        .build()

    private fun gzip(
        body: String,
        trailerBytesToRemove: Int = 0,
    ): Buffer {
        val encoded = Buffer()
        GzipSink(encoded).buffer().use { it.writeUtf8(body) }
        if (trailerBytesToRemove == 0) return encoded

        return Buffer().write(
            encoded.readByteString(encoded.size - trailerBytesToRemove)
        )
    }

    private class InMemoryPreferredIpStore : PreferredIpStore {
        private val values = mutableMapOf<String, String>()

        override fun getPreferredIp(hostname: String): String? = values[hostname]

        override fun setPreferredIp(hostname: String, ipAddress: String) {
            values[hostname] = ipAddress
        }
    }

    private companion object {
        const val HOSTNAME = "cdn.example"
        val WORKING_ADDRESS: InetAddress = InetAddress.getByAddress(byteArrayOf(127, 0, 0, 1))
        val BLOCKED_ADDRESS: InetAddress = InetAddress.getByAddress(byteArrayOf(127, 0, 0, 2))
    }
}
