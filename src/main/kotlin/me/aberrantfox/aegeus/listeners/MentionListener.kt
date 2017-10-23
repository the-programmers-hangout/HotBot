package me.aberrantfox.aegeus.listeners

import com.michaelwflaherty.cleverbotapi.CleverBotQuery
import me.aberrantfox.aegeus.services.APIRateLimiter
import me.aberrantfox.aegeus.services.Configuration
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent
import net.dv8tion.jda.core.hooks.ListenerAdapter


class MentionListener(val config: Configuration, val selfName: String) : ListenerAdapter() {
    private val rateLimiter = APIRateLimiter(config.cleverBotApiCallLimit, 0, "CleverBot")

    override fun onGuildMessageReceived(event: GuildMessageReceivedEvent) {
        if( !(rateLimiter.canCall()) ) return

        if(event.author.isBot) return

        if(config.ignoredIDs.contains(event.channel.id)) return

        if(config.ignoredIDs.contains(event.author.id)) return

        if(event.message.rawContent.toLowerCase().contains(event.jda.selfUser.name.toLowerCase())
            || event.message.isMentioned(event.jda.selfUser)) {

            rateLimiter.increment()
            event.message.addReaction("\uD83D\uDC40").queue()
            event.channel.sendMessage(cleverResponse(makeStatement(selfName, event.message.rawContent))).queue()
        }
    }

    private fun makeStatement(botname: String, eventMessage: String) = eventMessage.toLowerCase()
            .replace(botname, "cleverbot")
            .replace("@$botname", "cleverbot")
            .replace("@everyone", "cleverbot")
            .replace("@here", "cleverbot")

    private fun cleverResponse(input: String): String {
        val query = CleverBotQuery(config.cleverbotAPIKey, input)

        query.sendRequest()
        return query.response
    }
}