package ru.herobrine1st.ocbridge.data

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

class FunctionEntry(val function: List<String>, val args: List<JsonPrimitive>): CallStackEntry(Type.FUNCTION)
class CodeEntry(val code: String): CallStackEntry(Type.CODE)
data class AuthenticationData(val username: String?, val password: String?)


class RootStructure(val type: Type, val hash: Long, val call_stack: List<CallStackEntry>?) {
    enum class Type {
        PING, EXECUTE
    }
}