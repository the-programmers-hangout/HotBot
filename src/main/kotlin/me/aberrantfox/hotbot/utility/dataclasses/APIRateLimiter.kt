package me.aberrantfox.hotbot.utility.dataclasses

import com.google.gson.Gson
import me.aberrantfox.hotbot.services.configPath
import org.joda.time.DateTime
import java.io.File
import kotlin.concurrent.timer

private data class Datum(var current: Int)

data class APIRateLimiter(private val limit: Int, private var current: Int, val name: String) {
    private val gson = Gson()
    private val file = File(configPath("ratelimit/$name.json"))

    init {
        val parent = file.parentFile

        if( !(parent.exists()) ) {
            parent.mkdirs()
        }

        if(file.exists()) {
            val datum = gson.fromJson(file.readText(), Datum::class.java)
            current = datum.current
        }

        timer(name, false, 0.toLong(), 60 * 1000 * 60 * 24) { checkReset() }
    }

    fun increment() {
        current++
        val text = gson.toJson(Datum(current))
        file.writeText(text)
    }

    fun left() = limit - current

    fun canCall() = limit != current && limit > current

    private fun checkReset() =
        if(DateTime.now().toLocalDate().dayOfMonth == 25) {
            file.writeText(gson.toJson(Datum(0)))
            current = 0
        } else { Unit }
}
