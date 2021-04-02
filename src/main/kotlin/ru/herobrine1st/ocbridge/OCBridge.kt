package ru.herobrine1st.ocbridge

import ru.herobrine1st.ocbridge.network.Service
import ru.herobrine1st.ocbridge.network.SocketThread
import java.lang.RuntimeException
import java.util.*

@Suppress("MemberVisibilityCanBePrivate", "unused")
object OCBridge {
    val services = HashSet<Service>()
    private lateinit var socketThread: SocketThread

    /**
     * Starts the bridge with given port
     * @param port: port which bridge will listen to
     */
    fun start(port: Int) {
        socketThread = SocketThread(port)
        socketThread.start()
    }

    /**
     * Stops OCBridge thread softly. Thread will stop in 5 seconds at maximum.
     * All connections will be closed, but pending requests might not be satisfied.
     */
    fun stop() {
        socketThread.shouldStop = true
    }


    /**
     * Adds service to the bridge. You can execute it dynamically, even when there are connected clients.
     * @param service: service to add
     */
    fun add(service: Service) {
        if (services.any { it.name == service.name })
            throw RuntimeException("Service with name \"${service.name}\" already exists")
        services.add(service)
    }

    /**
     * Removes provided service from the bridge. You can execute it dynamically, but if there's connected to the service client it's not recommended.
     * Connection will be closed, but pending requests of the service might not be satisfied.
     * @param service: service to remove
     */
    fun remove(service: Service) {
        service.pendingRemove = true
        service.unbind()
        services.remove(service)
    }

    /**
     * Removes service from the bridge
     * @param name: name of service you want to remove
     * @see remove(service: Service)
     */
    fun remove(name: String) {
        remove(services.find { it.name == name } ?: throw NoSuchElementException())
    }
}