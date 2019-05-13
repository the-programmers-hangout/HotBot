package me.aberrantfox.hotbot.listeners

import com.google.common.eventbus.Subscribe
import me.aberrantfox.kjdautils.internal.logging.BotLogger
import me.aberrantfox.hotbot.services.PermissionService
import me.aberrantfox.hotbot.services.Configuration
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent

class FileListener (val config: Configuration, val manager: PermissionService, val log: BotLogger){
    @Subscribe fun onMessageReceived(event: GuildMessageReceivedEvent) {
        val message = event.message

        if (event.author.isBot) return

        if (manager.canPerformAction(event.author,config.permissionedActions.sendUnfilteredFiles)) return

        if (message.attachments.isEmpty()) return

        val containsIllegalAttachment = message.attachments.any { notAllowed(it.fileName) }

        if (containsIllegalAttachment){
            val fileNames = ArrayList<String>()
            for (i in message.attachments.stream()) fileNames.add(i.fileName)

            message.delete().queue()
            val user = event.author.asMention
            val files = fileNames.toString().substring(1,fileNames.toString().length-1)
            log.alert("$user attempted to send the illegal file(s) $files in ${event.channel.asMention}")

            val userResponse = "Please don't send that file type here $user, use an online service (such as https://hastebin.com)"
            event.channel.sendMessage(userResponse).queue()
        }

    }
    private val regex = "^.*\\.(jpg|jpeg|gif|png|mp4|webm|mov)\$".toRegex()

    private fun notAllowed (fileName: String): Boolean {
        return (!regex.matches(fileName))
    }
}
