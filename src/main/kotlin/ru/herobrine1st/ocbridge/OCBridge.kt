package ru.herobrine1st.ocbridge

import ru.herobrine1st.ocbridge.network.Service
import ru.herobrine1st.ocbridge.network.SocketThread


class OCBridge(private val port: Int) {
    val services = ArrayList<Service>()
    fun start() {
        SocketThread(this, port).start()
    }

    infix fun add(service: Service) {

    }

}