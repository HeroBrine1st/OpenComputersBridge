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
            "args": ["os_entDetector"], // аргументы, так же существует специальный параметр $индекс, который подставит
            значение из стека, для значения $ нужно написать $$ как строку
            "function": ["component", "list"], // Путь к функции от package.loaded,
            // Здесь так же существует параметр $индекс, только он должен быть всегда строковым
        }
    }
}

 */

open class CallStackEntry(open val type: Type) {
    enum class Type { CODE, FUNCTION }
}

class FunctionEntry(val function: Collection<String>, val args: Collection<JsonPrimitive>): CallStackEntry(Type.FUNCTION)
class CodeEntry(val code: String): CallStackEntry(Type.CODE)
data class AuthenticationData(val type: String?, val name: String?, val password: String?)


open class RootStructure(val type: Type, hash: Long, open val call_stack: List<CallStackEntry>?) {
    val hash: String = hash.toString()
    @Transient val timestamp = System.nanoTime()
    enum class Type {
        PING, EXECUTE
    }
}

class PingRequest(hash: Long): RootStructure(Type.PING, hash, null) {
    @Transient override val call_stack: Nothing? = null
}

class Response(val type: Type?, val result: JsonArray?, val hash: String?) {
    enum class Type {
        PONG, RESULT
    }
}