package me.aberrantfox.hotbot.listeners

import com.google.common.eventbus.Subscribe
import me.aberrantfox.hotbot.commands.utility.Polls
import me.aberrantfox.hotbot.commands.utility.emoteMap
import net.dv8tion.jda.core.events.message.react.MessageReactionAddEvent


class PollListener {
    @Subscribe
    fun onMessageReactionAdd(e: MessageReactionAddEvent) {
        if(e.user.id == e.jda.selfUser.id) {
            return
        }

        val emote = e.reaction.reactionEmote.name

        if (!(Polls.map.containsKey(e.messageId))) {
            return
        }

        if (!(emoteMap.containsKey(emote))) {
            return
        }

        e.textChannel.getMessageById(e.messageId).queue {
            it.reactions
                .filter { it.reactionEmote.name != emote }
                .forEach { it.removeReaction(e.user).queue() }
        }

    }
}