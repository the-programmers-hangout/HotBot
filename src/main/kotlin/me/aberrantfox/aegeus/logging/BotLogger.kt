package me.aberrantfox.aegeus.logging

import net.dv8tion.jda.core.JDA
import net.dv8tion.jda.core.entities.TextChannel


interface BotLogger {
    fun info(message: String)
    fun command(message: String)
    fun error(message: String)
    fun alert(message: String)
    fun warning(message: String)
}

data class ChannelIdHolder(val info: String = "insert-id-here",
                           val command: String = "insert-id-here",
                           val error: String = "insert-id-here",
                           val alert: String = "insert-id-here",
                           val warning: String = "insert-id-here")

fun convertChannels(holder: ChannelIdHolder, jda: JDA) =
    Channels (
        jda.getTextChannelById(holder.info),
        jda.getTextChannelById(holder.command),
        jda.getTextChannelById(holder.error),
        jda.getTextChannelById(holder.alert),
        jda.getTextChannelById(holder.warning))

data class Channels(val info: TextChannel,
                    val command: TextChannel,
                    val error: TextChannel,
                    val alert: TextChannel,
                    val warning: TextChannel)

class ChannelLogger(private val channels: Channels) : BotLogger {
    override fun info(message: String) = channels.info.sendMessage(message).queue()

    override fun command(message: String) = channels.command.sendMessage(message).queue()

    override fun error(message: String) = channels.error.sendMessage(message).queue()

    override fun alert(message: String) = channels.alert.sendMessage(message).queue()

    override fun warning(message: String) = channels.warning.sendMessage(message).queue()
}


