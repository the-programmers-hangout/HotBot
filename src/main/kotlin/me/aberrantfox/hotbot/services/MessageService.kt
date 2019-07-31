package me.aberrantfox.hotbot.services

import com.google.gson.Gson
import me.aberrantfox.kjdautils.api.annotation.Service
import java.io.File

data class Messages(var onJoin: ArrayList<String> = ArrayList(),
                    var names: ArrayList<String> = ArrayList(),
                    var serverDescription: String = "Insert Server Description here!",
                    var botDescription: String = "A neat bot for administrating servers.",
                    var gagResponse: String = "You've been muted temporarily so that a mod can handle something.",
                    var welcomeDescription: String = "This will be displayed underneath the greeting.",
                    var karmaMessage: String = "Well done %mention%, you have earned a karma point.")


private val messageFileLocation = configPath("responses.json")

@Service
class MessageService {
    private val messageFile: File = File(messageFileLocation)
    var messages = Messages()

    init {
        if(messageFile.exists()) {
            messages = loadMessages()
        } else {
            writeMessages()
        }
    }

    private fun writeMessages() = messageFile.writeText(Gson().toJson(messages))
    private fun loadMessages() = Gson().fromJson(File(messageFileLocation).readText(), Messages::class.java)
}
