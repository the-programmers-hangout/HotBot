package me.aberrantfox.hotbot.extensions

import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.TextChannel

fun TextChannel.safeDeleteMessages(messages: List<Message>, action: (TextChannel) -> Unit = {}, error: (TextChannel) -> Unit = {}) =
        try {
            deleteMessages(messages).queue {
                action.invoke(this)
            }
        } catch (e: IllegalArgumentException) { // some messages older than 2 weeks => can't mass delete
            messages.forEach {
                it.delete().queue {
                    error.invoke(this)
                }
            }
        }