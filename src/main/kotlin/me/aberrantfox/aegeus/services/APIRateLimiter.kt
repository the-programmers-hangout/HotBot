package me.aberrantfox.aegeus.services

import com.google.gson.Gson
import java.io.File

private data class Datum(var current: Int)

data class APIRateLimiter(private val limit: Int, private var current: Int, val name: String) {
    private val gson = Gson()
    private val file = File("ratelimit/$name.json")

    init {
        val parent = file.parentFile

        if( !(parent.exists()) ) {
            parent.mkdirs()
        }

        if(file.exists()) {
            val datum = gson.fromJson(file.readText(), Datum::class.java)
            current = datum.current
        }
    }

    fun increment() {
        current++
        val text = gson.toJson(Datum(current))
        file.writeText(text)
    }

    fun left() = limit - current

    fun canCall() = limit != current && limit > current
}