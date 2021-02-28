package ru.herobrine1st.ocbridge.network

import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import ru.herobrine1st.ocbridge.OCBridge
import ru.herobrine1st.ocbridge.data.*
import ru.herobrine1st.ocbridge.integration.Response
import java.io.IOException
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.channels.*
import kotlin.properties.Delegates
import java.util.concurrent.FutureTask

val gson = Gson()

fun <T> SocketChannel.writeJson(obj: T) {
    try {
        this.write(ByteBuffer.wrap("${gson.toJson(obj)}\n".toByteArray()))
    }catch(exc: IOException) {
        SocketThread.logger.warn(exc.toString())
        this.close() // SocketThread will check and detach this invalid channel from attached service
    }
}

object SocketThread : Thread("OCBridge Socket") {
    val logger: Logger = LoggerFactory.getLogger(this::class.java)
    var shouldStop = false
    private var port by Delegates.notNull<Int>()
    fun start(port: Int) {
        this.port = port
        super.start()
    }

    override fun start() {
        throw IllegalStateException()
    }

    override fun run() {
        val selector = Selector.open()
        val listenerChannel = ServerSocketChannel.open()
        listenerChannel.socket().reuseAddress = true
        listenerChannel.socket().bind(InetSocketAddress(port))
        listenerChannel.configureBlocking(false)
        listenerChannel.register(selector, SelectionKey.OP_ACCEPT)
        while(true) {
            selector.select(5000)
            selector.selectedKeys().iterator().forEach { key ->
                if(key.isAcceptable) {
                    val ch = (key.channel() as ServerSocketChannel).accept() ?: return@forEach
                    ch.configureBlocking(false)
                    ch.register(selector, SelectionKey.OP_READ)
                    ch.writeJson(AuthorizationRequired())
                    return@forEach
                }
                val ch = (key.channel() as SocketChannel)
                if(key.isConnectable) {
                    ch.finishConnect()
                }
                if(key.isReadable) {
                    val timestamp = System.nanoTime()
                    val stringBuilder = StringBuilder()
                    val buf = ByteBuffer.allocate(256)
                    var read: Int
                    try {
                        while(ch.read(buf).also { read = it } > 0) {
                            buf.flip()
                            val bytes = ByteArray(buf.limit())
                            buf.get(bytes)
                            stringBuilder.append(String(bytes))
                            buf.clear()
                        }
                    } catch(e: IOException) {
                        return@forEach
                    }
                    val str = stringBuilder.toString()
                    if(str.isEmpty()) return@forEach
                    val service = OCBridge.services.find { it.channel == ch }
                    if(service != null) {
                        if(read < 0) {
                            service.unbind()
                            ch.close()
                        }
                        val response: ResponseStructure
                        try {
                            response = gson.fromJson(str, ResponseStructure::class.java)
                        } catch(exc: JsonSyntaxException) {
                            service.unbind()
                            ch.close()
                            return@forEach
                        }
                        when(response.type) {
                            ResponseStructure.Type.EVENT -> (response.events ?: return@forEach)
                                .filter { it.isJsonArray }
                                .map { it.asJsonArray }
                                .forEach { service.onEvent(it) }
                            ResponseStructure.Type.MESSAGE -> service.onMessage(response.message ?: return@forEach)
                            ResponseStructure.Type.PONG -> service.pending.removeIf { it.hash == response.hash }
                            ResponseStructure.Type.RESULT -> {
                                val callback = service.callbacks.remove(response.hash)
                                if(callback != null) {
                                    val response1 = Response(
                                        success = response.success ?: return@forEach,
                                        result = response.result ?: return@forEach,
                                        request = service.pending.find { it.hash == response.hash } ?: return@forEach,
                                        timestamp = timestamp)
                                    service.pending.remove(response1.request)
                                    FutureTask { // Может быть говнокод, впервые юзаю эту хрень, но мне нужно убрать это подальше от этого потока
                                        try {
                                            callback(response1)
                                        } catch(exc: Exception) {
                                            logger.error("Service ${service.name} have an uncaught exception in callback", exc)
                                        }
                                    }.run()
                                }
                            }
                        }

                    } else {

                        if(read < 0) ch.close()
                        val auth: AuthenticationData
                        try {
                            auth = gson.fromJson(str, AuthenticationData::class.java)
                        } catch(exc: JsonSyntaxException) {
                            ch.close()
                            return@forEach
                        }
                        if(auth.type != "AUTHENTICATION" || auth.name == null || auth.password == null) {
                            ch.close()
                        }
                        val foundService = OCBridge.services.find { !it.pendingRemove && it.name == auth.name }
                        when {
                            foundService == null -> ch.writeJson(NotFound())
                            foundService.isNotReady -> ch.writeJson(ServiceBusy())
                            foundService.password != auth.password -> ch.writeJson(WrongPassword())
                            else -> {
                                foundService.bind(ch)
                            }
                        }
                    }
                }
            }
            selector.keys().filter { !it.isValid }.forEach { key ->
                OCBridge.services.find { it.channel == (key.channel() as SocketChannel) }?.unbind()
                key.cancel()
            }
            OCBridge.services.forEach { it.heartbeat() }
            if(shouldStop) break
        }
        selector.keys().forEach {
            it.channel().close()
            it.cancel()
        }
    }
}