package ru.herobrine1st.ocbridge

import ru.herobrine1st.ocbridge.network.SocketThread


class OCBridge(private val port: Int) {
    fun start() {
        SocketThread.port = port
        SocketThread.start()
    }
}