package me.aberrantfox.aegeus.services

import com.github.salomonbrys.kotson.typeToken
import com.google.gson.Gson
import org.joda.time.DateTime
import java.io.File
import java.util.*

data class PoolRecord(val sender: String, val dateTime: DateTime, val message: String, val avatarURL: String)

enum class AddResponse {
    Accepted, UserFull, PoolFull
}

class UserElementPool(val userLimit: Int = 3, val poolLimit: Int = 20, val poolName: String) {
    private val saveLocation = File("pools/$poolName.json")
    private val pool: Queue<PoolRecord> = LinkedList()
    private val gson = Gson()

    init {
        if(saveLocation.exists()) {
            val records = gson.fromJson(saveLocation.readText(), pool::class.java)
            records.forEach { pool.add(it) }
        }
    }

    fun addRecord(sender: String, avatarURL: String, message: String): AddResponse {
        if(totalInPool(sender) == userLimit) {
            return AddResponse.UserFull
        }

        if(pool.size == poolLimit) {
            return AddResponse.PoolFull
        }

        pool.add(PoolRecord(sender, DateTime.now(), message, avatarURL))
        save()
        return AddResponse.Accepted
    }

    fun top() = pool.poll()

    fun entries() = pool.size

    fun isEmpty() = pool.isEmpty()

    fun peek(): PoolRecord = pool.peek()

    private fun save() {
        val json = gson.toJson(pool)

        if( !(saveLocation.exists()) ) {
            saveLocation.mkdirs()
        }

        saveLocation.writeText(json)
    }

    private fun totalInPool(userID: String) = pool.count { it.sender == userID }
}