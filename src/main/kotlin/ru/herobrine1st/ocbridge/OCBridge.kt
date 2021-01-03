package ru.herobrine1st.ocbridge

import ru.herobrine1st.ocbridge.network.Service
import ru.herobrine1st.ocbridge.network.SocketThread
import java.lang.RuntimeException

object OCBridge {
    val services = HashSet<Service>()
    fun start(port: Int) {
        SocketThread.start(port)
        SocketThread.join() // TODO
    }

    /**
     * Stops OCBridge thread softly. Thread will stop in 5 seconds at maximum.
     * All connections will be closed, but not every operation will be finished.
     */
    fun stop() {
        SocketThread.shouldStop = true
    }

    fun add(service: Service) {
        if(services.any { it.name == service.name })
            throw RuntimeException("Service with name \"${service.name}\" already exists")
        services.add(service)
    }


    @JvmStatic
    fun main(args: Array<String>) {
        this.add(TestService1())
        this.add(TestService2())
        this.start(1024)
    }


    class TestService1: Service("1", "abcd") {
        override fun onConnect() {
            println("Connected 1")
        }

        override fun onDisconnect() {
            println("Disconnected 1")
        }
    }

    class TestService2: Service("2", "abcd") {
        override fun onConnect() {
            println("Connected 1")
        }

        override fun onDisconnect() {
            println("Disconnected 1")
        }
    }

}