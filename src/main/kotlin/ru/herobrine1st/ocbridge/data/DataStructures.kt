@file:Suppress("unused")

package ru.herobrine1st.ocbridge.data

import com.google.gson.JsonArray
import com.google.gson.JsonPrimitive

/*
так блет
структура значит

{
    "type": String тут типа тип, либо ping либо execute
    "hash": Long тут типа рандомное число, просто чтобы легко определить к какому запросу что принадлежит
    "call_stack": { null, если type=ping
        {
            "type": "code", // Выполнить код
            "code": "код для выполнения"
            // Результат выполнения сохранится под индексом элемента в этом массиве
            // Так же необходимо реализовать метод getFromStack, который выдаст результат выполнения под нужным индексом
        }
        {
            "type": "function", // выполнить функцию
            "args": ["os_entDetector"], // аргументы, так же существует специальный параметр $индекс[другой хуиндекс], который подставит
            значение из стека, для значения $ нужно написать $$ как строку
            "function": ["component", "list"], // Путь к функции от package.loaded,
        }
    }
}

 */

// Authorization


class AuthorizationRequired {
    val type = "AUTHORIZATION_REQUIRED"
}

class ServiceBusy {
    val type = "SERVICE_BUSY"
}

class NotFound {
    val type = "SERVICE_NOT_FOUND"
}

class WrongPassword {
    val type = "WRONG_PASSWORD"
}



open class CallStackEntry(open val type: Type) {
    enum class Type { CODE, FUNCTION }
}

class FunctionEntry(val function: Collection<String>, val args: Collection<JsonPrimitive>): CallStackEntry(Type.FUNCTION)
class CodeEntry(val code: String): CallStackEntry(Type.CODE)
data class AuthenticationData(val type: String?, val name: String?, val password: String?)


open class RequestStructure(val type: Type, hash: Long, open val call_stack: List<CallStackEntry>?) {
    val hash: String = hash.toString()
    @Transient var timestamp = System.nanoTime()
    enum class Type {
        PING, EXECUTE
    }
}

class PingRequest(hash: Long): RequestStructure(Type.PING, hash, null) {
    @Transient override val call_stack: Nothing? = null
}

class ResponseStructure(val type: Type?, val result: JsonArray?, val hash: String?, val success: Boolean?,
                        val message: String?, val events: JsonArray?) {
    enum class Type {
        PONG, RESULT, MESSAGE, EVENT
    }
}