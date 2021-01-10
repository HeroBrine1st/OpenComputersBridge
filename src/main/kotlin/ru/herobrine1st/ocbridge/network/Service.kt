package ru.herobrine1st.ocbridge.network

import com.google.gson.Gson
import com.google.gson.JsonArray
import ru.herobrine1st.ocbridge.OCBridge
import ru.herobrine1st.ocbridge.data.PingRequest
import ru.herobrine1st.ocbridge.data.RequestStructure
import ru.herobrine1st.ocbridge.integration.RequestBuilder
import ru.herobrine1st.ocbridge.integration.Response
import java.nio.ByteBuffer
import java.nio.channels.SocketChannel
import kotlin.math.pow
import kotlin.random.Random

abstract class Service(val name: String, val password: String) {
    var channel: SocketChannel? = null
        set(value) {
            field = value
            pending.clear()
        }
    val pending = ArrayList<RequestStructure>()
    val callbacks = HashMap<String, (Response) -> Unit>()
    private var lastPingTimestamp = 0L
    val isReady
        get() = channel != null

    abstract fun onConnect()
    abstract fun onDisconnect()
    abstract fun onMessage(message: String)
    abstract fun onEvent(event: JsonArray)

    fun disconnect() {
        channel?.close()
        channel = null
        onDisconnect()
    }

    fun request(): RequestBuilder {
        var hash = Random.nextLong()
        while(pending.any { it.hash == hash.toString() }) hash = Random.nextLong()
        return RequestBuilder(hash, this)
    }

    fun executeRequest(structure: RequestStructure, callback: (Response) -> Unit) {
        if(!isReady) throw IllegalStateException()
        channel!!.write(ByteBuffer.wrap("${Gson().toJson(structure)}\n".toByteArray()))
        structure.timestamp = System.nanoTime()
        callbacks[structure.hash] = callback
        pending += structure
    }

    fun heartbeat() {
        if(!isReady) return
        if(pending.any { it.type == RequestStructure.Type.PING }
            && System.nanoTime() - lastPingTimestamp > 5 * 10.0.pow(9.0)) {
                SocketThread.logger.info("$name disconnected cause of no response")
                disconnect()
        } else if (System.nanoTime() - lastPingTimestamp > 5 * 10.0.pow(9.0)) {
            var hash = Random.nextLong()
            while(pending.any { it.hash == hash.toString() }) hash = Random.nextLong()
            val req = PingRequest(hash)
            pending.add(req)
            channel?.writeJson(req)
            lastPingTimestamp = req.timestamp
        }
    }
}