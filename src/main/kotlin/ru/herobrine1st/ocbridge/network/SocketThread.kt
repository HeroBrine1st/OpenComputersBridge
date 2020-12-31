package ru.herobrine1st.ocbridge.network

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonSyntaxException
import ru.herobrine1st.ocbridge.OCBridge
import ru.herobrine1st.ocbridge.data.AuthenticationData
import ru.herobrine1st.ocbridge.data.Response
import ru.herobrine1st.ocbridge.data.RootStructure
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.channels.SelectionKey
import java.nio.channels.Selector
import java.nio.channels.ServerSocketChannel
import java.nio.channels.SocketChannel


class SocketThread(private val bridge: OCBridge, private val port: Int): Thread() {
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
                    val json = JsonObject()
                    json.addProperty("type", "AUTHORIZATION_REQUIRED")
                    ch.write(ByteBuffer.wrap("$json\n".toByteArray()))
                    return@forEach
                }
                val ch = (key.channel() as SocketChannel)
                if(key.isConnectable) {
                    ch.finishConnect()
                }
                if(key.isReadable) {
                    val stringBuilder = StringBuilder()
                    val buf = ByteBuffer.allocate(256)
                    var read = 0
                    while (ch.read(buf).also { read = it } > 0) {
                        buf.flip()
                        val bytes = ByteArray(buf.limit())
                        buf.get(bytes)
                        stringBuilder.append(String(bytes))
                        buf.clear()
                    }
                    val str = stringBuilder.toString()
                    val service = bridge.services.find { it.channel == ch }
                    if(service != null) {
                        try {
                            if(read < 0) {
                                service.disconnect()
                            }
                            if(str.isEmpty()) return@forEach
                            val response = Gson().fromJson(str, Response::class.java)
                            if(response.hash == null || response.type == null) return@forEach
                            if(response.type == Response.Type.PONG) {
                                val hash = response.hash
                                service.pending.removeIf { it.hash == hash }
                                ch.write(ByteBuffer.wrap("Ok".toByteArray()))
                            }else if(response.type == Response.Type.RESULT) {
                                service.callbacks.remove(response.hash)?.invoke(response.result ?: return@forEach)
                            }
                        } catch (exc: JsonSyntaxException) {
                            service.disconnect()
                        }
                    } else {
                        try {
                            if(read < 0) ch.close()
                            if(str.isEmpty()) return@forEach
                            val auth = Gson().fromJson(str, AuthenticationData::class.java)
                            if(auth.type == null || auth.type != "AUTHENTICATION" || auth.name == null || auth.password == null) {
                                ch.close()
                            }
                            val found = bridge.services.find { !it.isReady && it.name == auth.name && it.password == auth.password }
                            if(found != null) {
                                found.channel = ch
                                found.onConnect()
                            }
                        } catch (exc: JsonSyntaxException) {
                            ch.close()
                        }
                    }
                }
            }
            selector.keys().filter { !it.isValid }.forEach { key ->
                bridge.services.find { it.channel == (key.channel() as SocketChannel) }?.channel = null
                key.cancel()
            }
            bridge.services.forEach { it.pingTick() }
        }
    }
}