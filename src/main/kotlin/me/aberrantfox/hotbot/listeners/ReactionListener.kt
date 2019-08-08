package me.aberrantfox.hotbot.listeners.antispam

import com.google.common.eventbus.Subscribe
import me.aberrantfox.hotbot.services.Configuration
import me.aberrantfox.kjdautils.extensions.jda.fullName
import me.aberrantfox.kjdautils.internal.logging.BotLogger
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.MessageChannel
import net.dv8tion.jda.api.entities.MessageReaction
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent
import net.dv8tion.jda.api.events.message.react.MessageReactionRemoveEvent

class ReactionListener(val config: Configuration, val log: BotLogger) {
    @Subscribe
    fun onMessageReactionAdd(event: MessageReactionAddEvent?) {
        if (event != null) handleReaction(event.member, event.reactionEmote, event.reaction, event.channel, "added")
    }

    @Subscribe
    fun onMessageReactionRemove(event: MessageReactionRemoveEvent?) {
        if (event != null) handleReaction(event.member, event.reactionEmote, event.reaction, event.channel, "removed")
    }

    private fun handleReaction(author: Member?, emote: MessageReaction.ReactionEmote, reaction: MessageReaction, channel: MessageChannel, verb: String) {
        if (author == null || author.user.isBot) return

        val id = author.user.id

        if (author.roles.map { it.name }.contains(config.security.mutedRole)) {
            reaction.removeReaction(author.user).queue()
            log.alert("${author.fullName()} reacted using ${emote.name} while muted and it has been removed.")
        }

        if (config.security.verboseLogging) {
            log.alert("${author.fullName()} (id: $id) $verb the emote \"${emote.name}\" in #${channel.name} (message: ${reaction.messageId}).")
        }
    }
}
