package ru.herobrine1st.ocbridge.network

import kotlin.jvm.JvmStatic

object Abcd {
    @JvmStatic
    fun main(args: Array<String>) {
        var a: String?
        if (args[0].also { a = it } != null) {
            println(3)
        }
    }
}