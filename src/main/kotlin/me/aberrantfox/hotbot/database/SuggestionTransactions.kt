package me.aberrantfox.hotbot.database

import me.aberrantfox.hotbot.commandframework.commands.SuggestionStatus
import me.aberrantfox.hotbot.services.PoolRecord
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import org.joda.time.DateTime

data class SuggestionRecord(val messageID: String, val status: SuggestionStatus,
                            val poolInfo: PoolRecord,
                            val idea: String = poolInfo.message, val date: DateTime = poolInfo.dateTime,
                            val member: String = poolInfo.sender, val avatarURL: String = poolInfo.avatarURL)

fun trackSuggestion(suggestion: SuggestionRecord) =
        transaction {
            Suggestions.insert {
                it[Suggestions.id] = suggestion.messageID
                it[Suggestions.date] = suggestion.date
                it[Suggestions.idea] = suggestion.idea
                it[Suggestions.member] = suggestion.member
                it[Suggestions.status] = suggestion.status
                it[Suggestions.avatarURL] = suggestion.avatarURL
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