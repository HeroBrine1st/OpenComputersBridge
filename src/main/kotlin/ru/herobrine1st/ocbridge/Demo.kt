package ru.herobrine1st.ocbridge

import com.google.gson.JsonArray
import ru.herobrine1st.ocbridge.network.Service

fun main() {
    OCBridge.add(TestService1())
    OCBridge.add(TestService2())
    OCBridge.start(1024)
}


class TestService1: Service("1", "abcd") {
    override fun onConnect() {
        println("Connected 1")
        this.request()
            .addMethod("computer.beep", 2000, 0.2)
            .addCode("require(\"computer\").beep(2000, 1)")
            .build()
            .execute {
                println(it.result)
            }
    }

    override fun onDisconnect() {
        println("Disconnected 1")
    }

    override fun onMessage(message: String) {
        println("Msg from 1: $message")
    }

    override fun onEvent(event: JsonArray) {
        println("Event from 1: $event")
    }
}

class TestService2: Service("2", "abcd") {
    override fun onConnect() {
        println("Connected 2")
    }

    override fun onDisconnect() {
        println("Disconnected 2")
    }

    override fun onMessage(message: String) {
        println("Msg from 2: $message")
    }

    override fun onEvent(event: JsonArray) {
        println("Event from 2: $event")
    }
}