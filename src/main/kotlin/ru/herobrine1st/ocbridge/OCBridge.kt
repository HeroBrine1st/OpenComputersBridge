@file:Suppress("MemberVisibilityCanBePrivate")

package ru.herobrine1st.ocbridge

import ru.herobrine1st.ocbridge.network.Service
import ru.herobrine1st.ocbridge.network.SocketThread
import java.lang.RuntimeException

object OCBridge {
    val services = HashSet<Service>()
    fun start(port: Int) = SocketThread.start(port)

    /**
     * Stops OCBridge thread softly. Thread will stop in 5 seconds at maximum.
     * All connections will be closed, but not every pending operation will be finished.
     */
    fun stop() {
        SocketThread.shouldStop = true
    }

    fun add(service: Service) {
        if(services.any { it.name == service.name })
            throw RuntimeException("Service with name \"${service.name}\" already exists")
        services.add(service)
    }

    fun remove(service: Service) {
        service.pendingRemove = true
        service.disconnect()
        services.remove(service)
    }

    fun remove(name: String) {
        remove(services.find { it.name == name } ?: throw NoSuchElementException())
    }
}