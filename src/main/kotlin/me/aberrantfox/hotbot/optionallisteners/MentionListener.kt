package me.aberrantfox.hotbot.optionallisteners

import com.google.common.eventbus.Subscribe
import com.michaelwflaherty.cleverbotapi.CleverBotQuery
import me.aberrantfox.hotbot.permissions.PermissionManager
import me.aberrantfox.hotbot.services.APIRateLimiter
import me.aberrantfox.hotbot.services.Configuration
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent


class MentionListener(val config: Configuration, private val selfName: String, val manager: PermissionManager) {
    private val rateLimiter = APIRateLimiter(config.apiConfiguration.cleverBotApiCallLimit, 0, "CleverBot")
    private val pattern = Regex("(\\s|^)$selfName(\\s|$)")

    @Subscribe
    fun onGuildMessageReceived(event: GuildMessageReceivedEvent) {
        if( !(rateLimiter.canCall()) ) return

        if(event.author.isBot) return

        if(config.security.ignoredIDs.contains(event.channel.id)) return

        if(config.security.ignoredIDs.contains(event.author.id)) return

        if(!manager.canUseCleverbotInChannel(event.author, event.channel.id)) return

        if(event.message.contentRaw.toLowerCase().contains(pattern)
            || event.message.isMentioned(event.jda.selfUser)) {

            rateLimiter.increment()
            event.message.addReaction("\uD83D\uDC40").queue()
            event.channel.sendMessage(cleverResponse(makeStatement(selfName, event.message.contentRaw))).queue()
        }
    }

    private fun makeStatement(botname: String, eventMessage: String) = eventMessage.toLowerCase()
            .replace(botname, "cleverbot")
            .replace("@$botname", "cleverbot")
            .replace("@everyone", "cleverbot")
            .replace("@here", "cleverbot")

    private fun cleverResponse(input: String): String {
        val query = CleverBotQuery(config.apiConfiguration.cleverbotAPIKey, input)

        query.sendRequest()
        return query.response
    }
}