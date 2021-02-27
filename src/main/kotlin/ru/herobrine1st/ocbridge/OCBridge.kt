@file:Suppress("MemberVisibilityCanBePrivate")

package ru.herobrine1st.ocbridge

import ru.herobrine1st.ocbridge.network.Service
import ru.herobrine1st.ocbridge.network.SocketThread
import java.lang.RuntimeException

object OCBridge {
    val services = HashSet<Service>()

    /**
     * Starts the bridge with given port
     * @param port: port which bridge will listen
     */

    fun start(port: Int) = SocketThread.start(port)

    /**
     * Stops OCBridge thread softly. Thread will stop in 5 seconds at maximum.
     * All connections will be closed, but not every pending operation will be finished.
     */
    fun stop() {
        SocketThread.shouldStop = true
    }


    /**
     * Adds service to the bridge
     * @param service: service to add
     */
    fun add(service: Service) {
        if(services.any { it.name == service.name })
            throw RuntimeException("Service with name \"${service.name}\" already exists")
        services.add(service)
    }

    /**
     * Removes service from the bridge
     * @param service: service to remove
     */
    fun remove(service: Service) {
        service.pendingRemove = true
        service.disconnect()
        services.remove(service)
    }

    /**
     * Removes service from the bridge
     * @param name: name of service you want to remove
     */
    fun remove(name: String) {
        remove(services.find { it.name == name } ?: throw NoSuchElementException())
    }
}