package me.aberrantfox.hotbot.database

import me.aberrantfox.hotbot.commandframework.commands.SuggestionStatus
import me.aberrantfox.hotbot.services.PoolRecord
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction


fun trackSuggestion(suggestion: PoolRecord, status: SuggestionStatus, messageID: String) =
        transaction {
            Suggestions.insert {
                it[Suggestions.id] = messageID
                it[Suggestions.date] = suggestion.dateTime
                it[Suggestions.idea] = suggestion.message
                it[Suggestions.member] = suggestion.sender
                it[Suggestions.status] = status
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

            PoolRecord(row[Suggestions.member], row[Suggestions.date], row[Suggestions.idea], row[Suggestions.avatarURL])
        }

fun isTracked(id: String) =
        transaction {
            Suggestions.select {
                Op.build { Suggestions.id eq id }
            }.count() > 0
        }