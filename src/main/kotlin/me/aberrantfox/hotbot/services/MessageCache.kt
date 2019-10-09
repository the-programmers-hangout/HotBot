package me.aberrantfox.hotbot.services

import me.aberrantfox.hotbot.utility.types.LimitedList
import me.aberrantfox.kjdautils.api.annotation.Service
import net.dv8tion.jda.api.entities.Message
import org.apache.commons.text.similarity.LevenshteinDistance
import org.joda.time.DateTime

@Service
class MessageCache : IdTracker<LimitedList<AccurateMessage>>(1) {
    private val calc = LevenshteinDistance()

    fun addMessage(acMsg: AccurateMessage): Int {
        val who = acMsg.message.author.id

        val msgs = map.getOrPut(who) { LimitedList(20) }

        val matches = msgs.map { calc.apply(it.message.contentRaw, acMsg.message.contentRaw) }
            .filter { it <=  2 }
            .count()

        msgs.add(acMsg)

        return matches
    }

    fun count(who: String) = map.getOrDefault(who, LimitedList(20)).size

    fun removeUser(who: String) = map.remove(who)

    fun list(who: String) = map[who]
}

data class AccurateMessage(val time: DateTime, val message: Message)