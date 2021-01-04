package ru.herobrine1st.ocbridge.integration

import com.google.gson.JsonArray
import com.google.gson.JsonPrimitive
import ru.herobrine1st.ocbridge.data.CallStackEntry
import ru.herobrine1st.ocbridge.data.CodeEntry
import ru.herobrine1st.ocbridge.data.FunctionEntry
import ru.herobrine1st.ocbridge.data.RequestStructure
import ru.herobrine1st.ocbridge.network.Service
import java.lang.IllegalArgumentException

data class PreviousEntryResult(val entryIndex: Int, val resultIndex: Int = 1)


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
            args += when (it) {
                is String -> JsonPrimitive(it)
                is Number -> JsonPrimitive(it)
                is Boolean -> JsonPrimitive(it)
                is PreviousEntryResult -> JsonPrimitive("$${it.entryIndex}[${it.resultIndex}]")
                else -> throw IllegalArgumentException("Only Json primitives and PreviousEntryResult allowed here")
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