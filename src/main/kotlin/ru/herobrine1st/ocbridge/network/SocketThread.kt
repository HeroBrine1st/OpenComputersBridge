package ru.herobrine1st.ocbridge.network

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonSyntaxException
import ru.herobrine1st.ocbridge.OCBridge
import ru.herobrine1st.ocbridge.data.AuthenticationData
import java.net.InetSocketAddress
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
        listenerChannel.register(selector, SelectionKey.OP_ACCEPT)
        while(true) {
            selector.select()
            selector.selectedKeys().forEach { key ->
                if(key.isAcceptable) {
                    val ch = (key.channel() as ServerSocketChannel).accept()
                    ch.register(selector, SelectionKey.OP_READ)
                    val writer = ch.socket().getOutputStream().bufferedWriter()
                    writer.write(JsonObject().addProperty("type", "AUTHORIZATION_REQUIRED").toString())
                    writer.newLine()
                    writer.close()
                }
                val ch = (key.channel() as SocketChannel)
                if(key.isConnectable) {
                    ch.finishConnect()
                }
                if(key.isReadable) {
                    val service = bridge.services.find { it.socket == ch.socket() }
                    if(service != null) {
                        try {
                            ch.socket().getInputStream().bufferedReader().use { reader ->
                                val json = Gson().fromJson(reader, JsonObject::class.java)
                                TODO("Later")
                            }
                        } catch (exc: JsonSyntaxException) {
                            ch.close()
                        }
                    } else {
                        try {
                            ch.socket().getInputStream().bufferedReader().use { reader ->
                                val auth = Gson().fromJson(reader, AuthenticationData::class.java)
                                if(auth.username == null || auth.password == null) {
                                    ch.close()
                                }
                                val found = bridge.services.find { it.isReady && it.username == auth.username && it.password == auth.password }
                                if(found != null) {
                                    found.socket = ch.socket()
                                    found.onConnected()
                                }
                            }
                        } catch (exc: JsonSyntaxException) {
                            ch.close()
                        }
                    }
                }
            }
        }
    }
}