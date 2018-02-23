package me.aberrantfox.hotbot.services

import com.fatboyindustrial.gsonjodatime.Converters
import com.google.common.reflect.TypeToken
import org.joda.time.DateTime
import java.io.File
import java.util.*
import com.google.gson.GsonBuilder
import me.aberrantfox.hotbot.extensions.stdlib.idToName
import net.dv8tion.jda.core.EmbedBuilder
import net.dv8tion.jda.core.JDA
import net.dv8tion.jda.core.entities.MessageEmbed
import org.joda.time.format.DateTimeFormat
import java.time.LocalDateTime


data class PoolRecord(val sender: String, val dateTime: DateTime, val message: String, val avatarURL: String) {
    fun describe(jda: JDA, datumName: String): MessageEmbed =
        EmbedBuilder()
            .setTitle("$datumName by ${sender.idToName(jda)}")
            .setDescription(message)
            .addField("Time of Creation", formatConstructionDate(), false)
            .addField("Member ID", sender, false)
            .build()

    fun prettyPrint(jda: JDA, datumName: String): EmbedBuilder =
        EmbedBuilder()
            .setTitle("${sender.idToName(jda)}'s $datumName")
            .addField("Time of Creation", formatConstructionDate(), false)
            .addField("Content", message, false)
            .setThumbnail(avatarURL)
            .setTimestamp(LocalDateTime.now())

    private fun formatConstructionDate() = dateTime.toString(DateTimeFormat.forPattern("dd/MM/yyyy"))
}

enum class AddResponse {
    Accepted, UserFull, PoolFull
}

class UserElementPool(val userLimit: Int = 3, val poolLimit: Int = 20, val poolName: String) {
    private val saveLocation = File(configPath("pools/$poolName.json"))
    private val pool: Queue<PoolRecord> = LinkedList<PoolRecord>()
    private val gson = Converters.registerDateTime(GsonBuilder()).create()


    init {
        if(saveLocation.exists()) {
            val type = object : TypeToken<LinkedList<PoolRecord>>(){}.type
            val records = gson.fromJson<LinkedList<PoolRecord>>(saveLocation.readText(), type)
            records.forEach { pool.add(it) }
        } else {
            File(saveLocation.parent).mkdirs()
            saveLocation.createNewFile()
        }
    }

    fun addRecord(sender: String, avatarURL: String, message: String): AddResponse {
        if(totalInPool(sender) == userLimit)  return AddResponse.UserFull

        if(pool.size == poolLimit) return AddResponse.PoolFull

        pool.add(PoolRecord(sender, DateTime.now(), message, avatarURL))
        save()

        return AddResponse.Accepted
    }

    fun top(): PoolRecord? {
        val record = pool.poll()
        save()

        return record
    }

    fun entries() = pool.size

    fun isEmpty() = pool.isEmpty()

    fun peek(): PoolRecord? = pool.peek()

    private fun save() {
        val json = gson.toJson(pool)
        saveLocation.writeText(json)
    }

    private fun totalInPool(userID: String) = pool.count { it.sender == userID }
}
