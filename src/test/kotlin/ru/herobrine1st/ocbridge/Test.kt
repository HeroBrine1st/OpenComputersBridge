package ru.herobrine1st.ocbridge

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Test
import ru.herobrine1st.ocbridge.data.*


class Test {
    @Test
    fun test() {
        val a = RootStructure(RootStructure.Type.EXECUTE, 500, listOf(
            FunctionEntry(listOf("computer", "beep"), listOf(JsonPrimitive(2000), JsonPrimitive(0.5))),
            CodeEntry("""local computer = require("computer")
                        |computer.beep(2000, 0.5)""".trimMargin())
        ))
        print(Gson().toJson(a, RootStructure::class.java))
        print(Gson().fromJson("1234", JsonObject::class.java))
    }
}