package me.aberrantfox.hotbot.services

import net.dv8tion.jda.core.entities.Member
import net.dv8tion.jda.core.entities.Message

private val thanks = setOf("thanks", "thank you", "ty")

sealed class KarmaResult

object Negative : KarmaResult()
data class Positive(val member: Member) : KarmaResult()


class KarmaService {
    fun isKarmaMessage(message: Message): KarmaResult {
        val content = message.contentRaw.trim().toLowerCase()

        if(message.mentionedMembers.size != 1) return Negative

        if(message.mentionsEveryone()) return Negative

        if(message.mentionedRoles.size > 0) return Negative

        val target = message.mentionedMembers.first()

        if(target.user == message.author) return Negative

        val trimmedContent = content.replace(target.asMention, "").trim()

        if ( !thanks.contains(trimmedContent) ) return Negative

        return Positive(target)
    }

    fun grantKarma(member: Member, amount: Int) {

    }
}