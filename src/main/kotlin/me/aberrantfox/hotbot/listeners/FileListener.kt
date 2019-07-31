package me.aberrantfox.hotbot.listeners

import com.google.common.eventbus.Subscribe
import me.aberrantfox.kjdautils.internal.logging.BotLogger
import me.aberrantfox.hotbot.services.PermissionService
import me.aberrantfox.hotbot.services.Configuration
import me.aberrantfox.kjdautils.internal.logging.BotLogger
import net.dv8tion.jda.core.entities.Message
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent
import org.apache.tika.Tika

data class FileMetadata(val name: String,
                        val type: String,
                        val typeAlias: String,
                        val isAllowed: Boolean,
                        val onlineAlternative: String?)

class FileListener (val config: Configuration, val manager: PermissionService, val log: BotLogger){
    @Subscribe fun onMessageReceived(event: GuildMessageReceivedEvent) {
        val message = event.message

        if (event.author.isBot) return

        if (manager.canPerformAction(event.author,config.permissionedActions.sendUnfilteredFiles)) return

        if (message.attachments.isEmpty()) return

        val metadata = message.attachments.map { metadataOf(it) }
        val containsIllegalAttachment = metadata.any { !it.isAllowed }

        if (containsIllegalAttachment){
            message.delete().queue()
            val user = event.author.asMention
            event.channel.sendMessage(responseFor(user, metadata)).queue()
            val files = formatList(metadata.map { "${it.name} (${it.type})" })
            log.alert("$user attempted to send the illegal file(s) $files in ${event.channel.asMention}")
        }
    }

    private fun responseFor(author: String, allFileMetadata: List<FileMetadata>): String {
        val allAliases = formatList(allFileMetadata.map { it.typeAlias })
        val allAlternatives = formatList(allFileMetadata.mapNotNull { it.onlineAlternative })
        val scolding = "Please don't send $allAliases here $author"
        val alternative = ", use an online service (such as $allAlternatives)"
        return if(allAlternatives.isEmpty())
            scolding
        else
            scolding + alternative
    }

    private fun metadataOf(attachment: Message.Attachment): FileMetadata {
        val type = Tika().detect(attachment.inputStream)
        return FileMetadata(attachment.fileName, type, commonAliasFor(type), isAllowed(type), onlineAlternativeFor(type))
    }

    private fun isAllowed (type: String): Boolean {
        return type.startsWith("image")
            || type.startsWith("video")
    }

    private fun onlineAlternativeFor(type: String): String? {
        return when {
            type.startsWith("text") -> "https://hastebin.com"
            else -> null
        }
    }

    private fun commonAliasFor(type: String): String {
        return when {
            type == "application/x-tika-ooxml" -> "documents"
            type == "application/pdf" -> "pdf files"
            type.startsWith("application") -> "binaries"
            type.startsWith("text") -> "documents or code"
            else -> "those types of files"
        }
    }

    private fun formatList(values: List<String>, conjunction: String = " and "): String {
        return when {
            values.isEmpty() -> ""
            values.size == 1 -> values.first()
            values.size == 2 -> "${values[0]}$conjunction${values[1]}"
            values.size > 2 -> values.dropLast(2).joinToString { ", " } + formatList(values.takeLast(2), conjunction)
            else -> "Cannot format list"
        }
    }
}
