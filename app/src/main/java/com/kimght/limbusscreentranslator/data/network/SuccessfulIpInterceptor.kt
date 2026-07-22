package com.kimght.limbusscreentranslator.data.network

import okhttp3.Call
import okhttp3.Connection
import okhttp3.EventListener
import okhttp3.Interceptor
import okhttp3.MediaType
import okhttp3.Response
import okhttp3.ResponseBody
import okio.Buffer
import okio.BufferedSource
import okio.ForwardingSource
import okio.buffer
import java.io.IOException
import java.net.Proxy
import java.util.WeakHashMap

class SuccessfulIpInterceptor(
    private val store: PreferredIpStore,
    private val routes: SuccessfulIpRoutes,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val call = chain.call()
        val response = try {
            chain.proceed(chain.request())
        } catch (failure: Throwable) {
            routes.remove(call)
            throw failure
        }
        if (!response.isSuccessful) {
            routes.remove(call)
            return response
        }

        val hostname = response.request.url.host
        val cacheRoute = {
            val ipAddress = routes.take(call)
            if (ipAddress != null && !call.isCanceled()) {
                runCatching { store.setPreferredIp(hostname, ipAddress) }
            }
            Unit
        }
        val body = response.body
        if (body.contentLength() == 0L) {
            cacheRoute()
            return response
        }

        return response.newBuilder()
            .body(
                EofTrackingResponseBody(
                    delegate = body,
                    onEof = cacheRoute,
                    onIncomplete = { routes.remove(call) },
                )
            )
            .build()
    }

    private class EofTrackingResponseBody(
        private val delegate: ResponseBody,
        onEof: () -> Unit,
        onIncomplete: () -> Unit,
    ) : ResponseBody() {

        private val trackingSource = object : ForwardingSource(delegate.source()) {
            private var terminal = false

            override fun read(sink: Buffer, byteCount: Long): Long {
                try {
                    val bytesRead = super.read(sink, byteCount)
                    if (bytesRead == -1L && !terminal) {
                        terminal = true
                        onEof()
                    }
                    return bytesRead
                } catch (failure: Throwable) {
                    if (!terminal) {
                        terminal = true
                        onIncomplete()
                    }
                    throw failure
                }
            }

            override fun close() {
                try {
                    super.close()
                } finally {
                    if (!terminal) {
                        terminal = true
                        onIncomplete()
                    }
                }
            }
        }.buffer()

        override fun contentType(): MediaType? = delegate.contentType()

        override fun contentLength(): Long = delegate.contentLength()

        override fun source(): BufferedSource = trackingSource
    }
}

class SuccessfulIpRoutes {
    private val addresses = WeakHashMap<Call, String>()

    @Synchronized
    fun record(call: Call, ipAddress: String) {
        addresses[call] = ipAddress
    }

    @Synchronized
    fun remove(call: Call) {
        addresses.remove(call)
    }

    @Synchronized
    fun take(call: Call): String? = addresses.remove(call)
}

class SuccessfulIpEventListenerFactory(
    private val routes: SuccessfulIpRoutes,
) : EventListener.Factory {
    override fun create(call: Call): EventListener = SuccessfulIpEventListener(routes)
}

class SuccessfulIpEventListener(
    private val routes: SuccessfulIpRoutes,
) : EventListener() {

    override fun connectionAcquired(call: Call, connection: Connection) {
        val route = connection.route()
        val ipAddress = if (route.proxy.type() == Proxy.Type.DIRECT) {
            route.socketAddress.address?.hostAddress
        } else {
            null
        }
        if (ipAddress == null) {
            routes.remove(call)
        } else {
            routes.record(call, ipAddress)
        }
    }

    override fun callFailed(call: Call, ioe: IOException) {
        routes.remove(call)
    }
}
