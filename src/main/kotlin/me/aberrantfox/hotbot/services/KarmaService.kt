package me.aberrantfox.hotbot.services

import me.aberrantfox.kjdautils.api.annotation.Service
import net.dv8tion.jda.api.entities.*

private val thanks = setOf("thanks", "thank you", "ty")

sealed class KarmaResult

object Negative : KarmaResult()
data class Positive(val member: Member) : KarmaResult()

@Service
class KarmaService {
    fun isKarmaMessage(message: Message): KarmaResult {
        val content = message.contentRaw.trim().toLowerCase()

        if(message.mentionedMembers.size != 1) return Negative

        if(message.mentionsEveryone()) return Negative

        if(message.mentionedRoles.size > 0) return Negative

        val target = message.mentionedMembers.first()

        if(target.user == message.author) return Negative

        val trimmedContent = content.replace(target.asMention, "")
                                    .replace(target.user.asMention, "")
                                    .trim()

        if ( !thanks.contains(trimmedContent) ) return Negative

        if(target.user.isBot) return Negative

        return Positive(target)
    }
}