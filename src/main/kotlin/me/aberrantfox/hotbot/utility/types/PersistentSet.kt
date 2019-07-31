package me.aberrantfox.hotbot.utility.types

import com.google.common.reflect.TypeToken
import com.google.gson.Gson
import java.io.File

class PersistentSet(location: String) : HashSet<String>() {
    private val gson = Gson()
    private val file = File(location)

    init {
        if(file.exists()) {
            val type = object : TypeToken<ArrayList<String>>() {}.type
            addAll(gson.fromJson<ArrayList<String>>(file.readText(), type))
        }
    }

    override fun add(data: String): Boolean {
        val result = super.add(data)
        save()

        return result
    }

    override fun remove(data: String): Boolean {
        val result = super.remove(data)
        save()

        return result
    }

    override fun clear() {
        super.clear()
        save()
    }

    private fun save() = file.writeText(gson.toJson(this as HashSet<String>))
}