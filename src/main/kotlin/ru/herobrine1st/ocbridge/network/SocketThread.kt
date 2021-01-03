package ru.herobrine1st.ocbridge.network

import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import ru.herobrine1st.ocbridge.OCBridge
import ru.herobrine1st.ocbridge.data.*
import ru.herobrine1st.ocbridge.integration.Response
import java.io.IOException
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.channels.SelectionKey
import java.nio.channels.Selector
import java.nio.channels.ServerSocketChannel
import java.nio.channels.SocketChannel
import kotlin.properties.Delegates

object SocketThread: Thread() {
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
        val gson = Gson()
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
                    ch.write(ByteBuffer.wrap("${gson.toJson(AuthorizationRequired())}\n".toByteArray()))
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
                    } catch(e: IOException) { // I dunno what I should do in this case
                        // e.printStackTrace()
                        return@forEach
                    }
                    val str = stringBuilder.toString()
                    if(str.isEmpty()) return@forEach
                    val service = OCBridge.services.find { it.channel == ch }
                    if(service != null) {
                        try {
                            if(read < 0) {
                                service.disconnect()
                                ch.close()
                            }
                            if(str.isEmpty()) return@forEach
                            val response = gson.fromJson(str, ResponseStructure::class.java)
                            when (response.type) {
                                null -> return@forEach
                                ResponseStructure.Type.EVENT -> (response.events ?: return@forEach)
                                    .filter { it.isJsonArray }
                                    .map { it.asJsonArray }
                                    .forEach { service.onEvent(it) }
                                ResponseStructure.Type.MESSAGE -> service.onMessage(response.message ?: return@forEach)
                                ResponseStructure.Type.PONG -> { // Опускаем проверку на наличие поля hash - оно обязательно, но его отсуствие не вызовет ошибок
                                    service.pending.removeIf { it.hash == response.hash }
                                }
                                ResponseStructure.Type.RESULT -> {
                                    val response1 = Response(
                                        success = response.success ?: return@forEach,
                                        result = response.result ?: return@forEach,
                                        request = service.pending.find { it.hash == response.hash } ?: return@forEach,
                                        timestamp = timestamp)
                                    service.callbacks.remove(response.hash)?.invoke(response1)
                                    service.pending.removeIf { it.hash == response.hash }
                                }
                            }
                        } catch (exc: JsonSyntaxException) {
                             service.disconnect()
                             ch.close()
                        }
                    } else {
                        try {
                            if(read < 0) ch.close()
                            if(str.isEmpty()) return@forEach
                            val auth = gson.fromJson(str, AuthenticationData::class.java)
                            if(auth.type != "AUTHENTICATION" || auth.name == null || auth.password == null) {
                                ch.close()
                            }
                            if(!OCBridge.services.any { it.name == auth.name }) {
                                ch.write(ByteBuffer.wrap("${gson.toJson(NotFound())}\n".toByteArray()))
                                return@forEach
                            }
                            if(!OCBridge.services.any { it.name == auth.name && !it.isReady }) {
                                ch.write(ByteBuffer.wrap("${gson.toJson(ServiceBusy())}\n".toByteArray()))
                                return@forEach
                            }
                            val found = OCBridge.services.find { !it.isReady && it.name == auth.name && it.password == auth.password }
                            if(found != null) {
                                found.channel = ch
                                found.onConnect()
                            } else {
                                ch.write(ByteBuffer.wrap("${gson.toJson(WrongPassword())}\n".toByteArray()))
                            }
                        } catch (exc: JsonSyntaxException) {
                            ch.close()
                        }
                    }
                }
            }
            selector.keys().filter { !it.isValid }.forEach { key ->
                OCBridge.services.find { it.channel == (key.channel() as SocketChannel) }?.disconnect()
                key.cancel()
            }
            OCBridge.services.forEach { it.heartbeat() }
            if(shouldStop) {
                break
            }
        }
        selector.keys().forEach {
            it.channel().close()
            it.cancel()
        }
    }
}