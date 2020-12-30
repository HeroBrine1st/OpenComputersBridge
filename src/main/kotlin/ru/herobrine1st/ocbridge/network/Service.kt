package ru.herobrine1st.ocbridge.network

import java.net.Socket

abstract class Service(val username: String, val password: String) {
    companion object {
        val services = HashSet<Service>()
    }
    var socket: Socket? = null
    val isReady
        get() = socket != null
    init {
        if(services.any {it.username == this.username}) {
            throw IllegalArgumentException("Same username between two services")
        }
    }
    // TODO реализовать нормально
    // fun add() = services.add(this)

    abstract fun onConnected()
}