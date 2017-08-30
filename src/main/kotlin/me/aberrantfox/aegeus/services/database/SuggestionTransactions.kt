package me.aberrantfox.aegeus.services.database

import me.aberrantfox.aegeus.commandframework.commands.Suggestion
import me.aberrantfox.aegeus.commandframework.commands.SuggestionStatus
import org.jetbrains.exposed.sql.Op
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update


fun trackSuggestion(suggestion: Suggestion, status: SuggestionStatus) =
        transaction {
            Suggestions.insert {
                it[Suggestions.date] = suggestion.timeOf
                it[Suggestions.idea] = suggestion.idea
                it[Suggestions.member] = suggestion.member
                it[Suggestions.status] = status
            }
        }

fun updateSuggestion(id: Int, status: SuggestionStatus) =
        transaction {
            Suggestions.update ({Suggestions.id eq id }) {
                it[Suggestions.status] = status
            }
        }

fun deleteSuggestion(id: Int) =
        transaction {
            Suggestions.deleteWhere {
                Op.build { Suggestions.id eq id }
            }
        }