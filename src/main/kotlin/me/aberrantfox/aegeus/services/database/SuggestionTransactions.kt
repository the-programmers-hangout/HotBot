package me.aberrantfox.aegeus.services.database

import me.aberrantfox.aegeus.commandframework.commands.Suggestion
import me.aberrantfox.aegeus.commandframework.commands.SuggestionStatus
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction


fun trackSuggestion(suggestion: Suggestion, status: SuggestionStatus, messageID: String) =
        transaction {
            Suggestions.insert {
                it[Suggestions.id] = messageID
                it[Suggestions.date] = suggestion.timeOf
                it[Suggestions.idea] = suggestion.idea
                it[Suggestions.member] = suggestion.member
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

            Suggestion(row[Suggestions.member], row[Suggestions.idea], row[Suggestions.date], row[Suggestions.avatarURL])
        }

fun isTracked(id: String) =
        transaction {
            Suggestions.select {
                Op.build { Suggestions.id eq id }
            }.count() > 0
        }