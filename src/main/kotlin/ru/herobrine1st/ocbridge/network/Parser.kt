package ru.herobrine1st.ocbridge.network

import com.google.gson.Gson;
import com.google.gson.JsonObject

object Parser {
    @JvmStatic
    fun parse(json: String) {
        val root = Gson().fromJson(json, JsonObject::class.java)
    }
}