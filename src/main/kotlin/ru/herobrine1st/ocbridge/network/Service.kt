package ru.herobrine1st.ocbridge.network

import java.net.Socket

abstract class Service(val username: String, val password: String) {
    var socket: Socket? = null
    val isReady
        get() = socket != null

    abstract fun onConnected()
}