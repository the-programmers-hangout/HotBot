package me.aberrantfox.hotbot.services

import com.google.gson.Gson
import java.io.File

data class Messages(val onJoin: ArrayList<String> = ArrayList(),
                    val names: ArrayList<String> = ArrayList(),
                    var serverDescription: String = "Insert Server Description here!",
                    var botDescription: String = "Insert bot description here",
                    var permanentInviteLink: String = "discord.gg/programming",
                    var gagResponse: String = "You've been muted temporarily so that a mod can handle something.")


private const val messageFileLocation = "responses.json"

class MService {
    private val messageFile: File = File(messageFileLocation)
    var messages = Messages()

    init {
        if(messageFile.exists()) {
            messages = loadMessages()
        } else {
            writeMessages()
        }
    }

    fun writeMessages() = messageFile.writeText(Gson().toJson(messages))
    private fun loadMessages() = Gson().fromJson(File(messageFileLocation).readText(), Messages::class.java)
}
