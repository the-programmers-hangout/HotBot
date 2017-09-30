package me.aberrantfox.aegeus.services

import com.google.gson.Gson
import me.aberrantfox.aegeus.extensions.randomListItem
import java.io.File

private data class Messages(val onJoin: List<String>, val names: List<String>)

enum class MessageType {
    Join, Name
}

object MessageService {
    private val messages = loadMessages()

    fun getMessage(type: MessageType) =
            when (type) {
                MessageType.Join -> randomListItem(messages.onJoin)
                MessageType.Name -> randomListItem(messages.names)
            }

    private fun loadMessages(): Messages {
        val gson = Gson()
        val data = gson.fromJson(File("responses.json").readText(), Messages::class.java)

        return data
    }
}
