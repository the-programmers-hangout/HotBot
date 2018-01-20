package me.aberrantfox.hotbot.logging

import net.dv8tion.jda.core.JDA
import net.dv8tion.jda.core.entities.TextChannel


interface BotLogger {
    fun info(message: String)
    fun cmd(message: String)
    fun error(message: String)
    fun alert(message: String)
    fun warning(message: String)
    fun voice(message: String)
}

data class ChannelIdHolder(val info: String = "insert-id-here",
                           val cmd: String = "insert-id-here",
                           val error: String = "insert-id-here",
                           val alert: String = "insert-id-here",
                           val warning: String = "insert-id-here",
                           val voice: String = "insert-id-here")

fun convertChannels(holder: ChannelIdHolder, jda: JDA): BotLogger =
    ChannelLogger(Channels(
        jda.getTextChannelById(holder.info),
        jda.getTextChannelById(holder.cmd),
        jda.getTextChannelById(holder.error),
        jda.getTextChannelById(holder.alert),
        jda.getTextChannelById(holder.warning),
        jda.getTextChannelById(holder.voice)))

data class Channels(val info: TextChannel,
                    val cmd: TextChannel,
                    val error: TextChannel,
                    val alert: TextChannel,
                    val warning: TextChannel,
                    val voice: TextChannel)

class DefaultLogger : BotLogger {
    override fun info(message: String) {}
    override fun cmd(message: String) {}

    override fun error(message: String) {}

    override fun alert(message: String) {}

    override fun warning(message: String) {}

    override fun voice(message: String) {}
}

class ChannelLogger(private val channels: Channels) : BotLogger {
    override fun info(message: String) = channels.info.sendMessage(message).queue()

    override fun cmd(message: String) = channels.cmd.sendMessage(message).queue()

    override fun error(message: String) = channels.error.sendMessage(message).queue()

    override fun alert(message: String) = channels.alert.sendMessage(message).queue()

    override fun warning(message: String) = channels.warning.sendMessage(message).queue()

    override fun voice(message: String) = channels.voice.sendMessage(message).queue()
}


