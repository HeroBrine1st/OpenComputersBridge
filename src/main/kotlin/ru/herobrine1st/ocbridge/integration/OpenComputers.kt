package ru.herobrine1st.ocbridge.integration

import com.google.gson.JsonArray
import com.google.gson.JsonPrimitive
import ru.herobrine1st.ocbridge.data.CallStackEntry
import ru.herobrine1st.ocbridge.data.CodeEntry
import ru.herobrine1st.ocbridge.data.FunctionEntry
import ru.herobrine1st.ocbridge.data.RequestStructure
import ru.herobrine1st.ocbridge.network.Service
import java.sql.Timestamp
import kotlin.jvm.Throws

data class PreviousEntryResult(val index: Int)


class Request(private val structure: RequestStructure, private val service: Service) {
    fun execute(callback: (Response) -> Unit) {
        service.executeRequest(structure, callback)
    }
}

class OpenComputersError(msg: String): Exception(msg)

class Response(val success: Boolean, val result: JsonArray, val request: RequestStructure, timestamp: Long) {
    val time: Long = timestamp - request.timestamp

    @Throws(OpenComputersError::class)
    fun throwIfError() {
        if(!success) {
            throw OpenComputersError(result.get(0)?.asString ?: "Unexpected error")
        }
    }
}

class RequestBuilder(private val hash: Long, private val service: Service) {
    private val stack = ArrayList<CallStackEntry>()
    fun addMethod(function: String, vararg arguments: Any): RequestBuilder {
        val func = function.split(".")
        val args = ArrayList<JsonPrimitive>()
        arguments.forEach {
            when (it) {
                is String -> args += JsonPrimitive(it)
                is Number -> args += JsonPrimitive(it)
                is Boolean -> args += JsonPrimitive(it)
                is PreviousEntryResult -> args += JsonPrimitive("$${it.index}")
            }
        }
        stack += FunctionEntry(func, args)
        return this
    }
    fun addCode(code: String): RequestBuilder {
        stack += CodeEntry(code)
        return this
    }

    fun build(): Request {
        return Request(RequestStructure(RequestStructure.Type.EXECUTE, hash, stack), service)
    }
}