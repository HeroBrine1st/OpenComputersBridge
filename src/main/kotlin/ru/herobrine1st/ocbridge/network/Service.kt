package ru.herobrine1st.ocbridge.network

import java.net.Socket

class Service(private val username: String, private val password: String) {
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
    fun add() = services.add(this)
}