package ru.herobrine1st.ocbridge

import com.google.gson.Gson
import com.google.gson.JsonPrimitive
import org.junit.Test
import ru.herobrine1st.ocbridge.data.*
import ru.herobrine1st.ocbridge.network.Service


class Test {
    @Test
    fun test() {
//        val a = RootStructure(RootStructure.Type.EXECUTE, 500, listOf(
//            FunctionEntry(listOf("computer", "beep"), listOf(JsonPrimitive(2000), JsonPrimitive(0.5))),
//            CodeEntry("""local computer = require("computer")
//                        |computer.beep(2000, 0.5)""".trimMargin())
//        ))
//        println(Gson().toJson(a, RootStructure::class.java))
//        println(Gson().toJson(PingRequest(2334145), PingRequest::class.java))
//        RootStructure.Type.valueOf("1234")
        val a = Gson().fromJson("{\"type\":\"PONG1\", \"hash\":\"234234\"}", Response::class.java)
        println(a.hash)
        println(a.type)
        println(a.result)
    }
}