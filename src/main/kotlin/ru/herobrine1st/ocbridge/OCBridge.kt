package ru.herobrine1st.ocbridge

import ru.herobrine1st.ocbridge.network.Service
import ru.herobrine1st.ocbridge.network.SocketThread



class OCBridge(private val port: Int) {
    val services = HashSet<Service>()
    fun start() {
        val thread = SocketThread(this, port)
        thread.start()
        thread.join()
    }

    infix fun add(service: Service) = services.add(service)

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            val bridge = OCBridge(1024)
            bridge add TestService1()
            bridge add TestService2()
            bridge.start()
        }
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