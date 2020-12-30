package ru.herobrine1st.ocbridge.network

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonStreamParser
import com.google.gson.stream.JsonWriter
import ru.herobrine1st.ocbridge.data.AuthenticationData
import java.io.BufferedWriter
import java.io.FileDescriptor
import java.io.OutputStream
import java.io.OutputStreamWriter
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.SocketAddress
import java.nio.channels.SelectionKey
import java.nio.channels.Selector
import java.nio.channels.ServerSocketChannel
import java.nio.channels.SocketChannel


object SocketThread: Thread() {
    override fun run() {
        val selector = Selector.open()
        val listenerChannel = ServerSocketChannel.open()
        listenerChannel.socket().reuseAddress = true
        listenerChannel.socket().bind(InetSocketAddress(1024))
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
                    val service: Service
                    if(Service.services.find { it.socket == ch.socket() }?.also { service = it } != null) {
                        TODO()
                    } else {
                        val auth = Gson().fromJson(ch.socket().getInputStream().bufferedReader(), AuthenticationData::class.java)
                        if(auth.username == null || auth.password == null) {

                        }
                    }
                }
            }
        }
    }
}