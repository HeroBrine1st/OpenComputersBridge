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
    /**
     * Executes the method. This method is asynchronous, so code will run on without waiting the response.
     * @param callback: callback to return the response.
     */
    fun execute(callback: (Response) -> Unit) {
        service.executeRequest(structure, callback)
    }
}

class OpenComputersError(msg: String): Exception(msg)

/**
 * @param success: true if has no errors
 * @param result: JsonArray with result of execution
 * @param request: the request the response replied to
 */
class Response(val success: Boolean, val result: JsonArray, val request: RequestStructure, timestamp: Long) {
    val time: Long = timestamp - request.timestamp


    /**
     * Throws an OpenComputersError if Response has errors.
     * @throws OpenComputersError
     */
    @Throws(OpenComputersError::class)
    fun throwIfError() {
        if(!success) {
            throw OpenComputersError(result.get(0)?.asString ?: "Unexpected error")
        }
    }
}

class RequestBuilder(private val hash: Long, private val service: Service) {
    private val stack = ArrayList<CallStackEntry>()

    /**
     * Adds some method with(out) arguments to call stack.
     * @param function: dot-joined method without braces. For eg ``computer.beep``.
     * @param arguments: vararg JSON primitive. Only String, Number, Boolean or PreviousEntryResult allowed here.
     * @return builder
     * @see PreviousEntryResult
     */
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


    /**
     * Adds code to call stack.
     * @param code: lua code
     * @return builder
     */
    fun addCode(code: String): RequestBuilder {
        stack += CodeEntry(code)
        return this
    }

    /**
     * Builds request ready to dispatch
     */
    fun build(): Request {
        return Request(RequestStructure(RequestStructure.Type.EXECUTE, hash, stack), service)
    }
}