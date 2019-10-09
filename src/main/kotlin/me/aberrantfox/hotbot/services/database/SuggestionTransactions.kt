package me.aberrantfox.hotbot.services.database

import me.aberrantfox.hotbot.commands.utility.SuggestionStatus
import me.aberrantfox.hotbot.utility.dataclasses.PoolRecord
import me.aberrantfox.kjdautils.api.annotation.Service
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import org.joda.time.DateTime

data class SuggestionRecord(val messageID: String, val status: SuggestionStatus,
                            val poolInfo: PoolRecord,
                            val idea: String = poolInfo.message, val date: DateTime = poolInfo.dateTime,
                            val member: String = poolInfo.sender, val avatarURL: String = poolInfo.avatarURL)

@Service
class SuggestionTransactions {
    fun trackSuggestion(suggestion: SuggestionRecord) =
            transaction {
                Suggestions.insert {
                    it[id] = suggestion.messageID
                    it[date] = suggestion.date
                    it[idea] = suggestion.idea
                    it[member] = suggestion.member
                    it[status] = suggestion.status
                    it[avatarURL] = suggestion.avatarURL
                }
            }

    fun updateSuggestion(id: String, status: SuggestionStatus) =
            transaction {
                Suggestions.update ({Suggestions.id eq id }) {
                    it[Suggestions.status] = status
                }
            }

    fun deleteSuggestion(member: String, idea: String) =
            transaction {
                Suggestions.deleteWhere {
                    Op.build { (Suggestions.member eq member) and (Suggestions.idea eq idea) }
                }
            }

    fun obtainSuggestion(id: String) =
            transaction {
                val row = Suggestions.select {
                    Op.build { Suggestions.id eq id }
                }.first()

                val poolInfo = PoolRecord(row[Suggestions.member], row[Suggestions.date], row[Suggestions.idea], row[Suggestions.avatarURL])
                SuggestionRecord(row[Suggestions.id], row[Suggestions.status], poolInfo)
            }

    fun isTracked(id: String) =
            transaction {
                Suggestions.select {
                    Op.build { Suggestions.id eq id }
                }.count() > 0
            }
}
