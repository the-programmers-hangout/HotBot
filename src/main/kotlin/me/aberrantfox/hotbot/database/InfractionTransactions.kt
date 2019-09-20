package me.aberrantfox.hotbot.database

import me.aberrantfox.hotbot.commands.administration.StrikeRequest
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import org.joda.time.DateTime

data class StrikeRecord(val id: Int,
                        val moderator: String,
                        val member: String,
                        val strikes: Int,
                        val reason: String,
                        val dateTime: DateTime,
                        val isExpired: Boolean)

fun insertInfraction(strike: StrikeRequest) =
        transaction {
            Strikes.insert {
                it[member] = strike.target.id
                it[moderator] = strike.moderator.id
                it[strikes] = strike.amount
                it[reason] = strike.reason
                it[date] = DateTime.now()
            }
        }

fun getHistory(userId: String): List<StrikeRecord> =
        transaction {
            val select = Strikes.select {
                Op.build { Strikes.member eq userId }
            }

            val records = mutableListOf<StrikeRecord>()

            select.forEach {
                records.add(StrikeRecord(
                        it[Strikes.id],
                        it[Strikes.moderator],
                        it[Strikes.member],
                        it[Strikes.strikes],
                        it[Strikes.reason],
                        it[Strikes.date],
                        isExpired(it[Strikes.id])))
            }

            records
        }

fun removeInfraction(infractionID: Int): Int =
        transaction {
            val amountDeleted = Strikes.deleteWhere {
                Op.build { Strikes.id eq infractionID }
            }

            amountDeleted
        }

fun removeAllInfractions(userId: String): Int =
        transaction {
            val amountDeleted = Strikes.deleteWhere {
                Op.build { Strikes.member eq userId }
            }

            amountDeleted
        }

fun isExpired(infractionID: Int): Boolean =
        transaction {
            val rows = Strikes.select {
                Op.build { Strikes.id eq infractionID }
            }
            val date = rows.first()[Strikes.date]

            DateTime.now().isAfter(date.plusDays(30))
        }

fun getMaxStrikes(userId: String) = getHistory(userId)
        .filter { !it.isExpired }
        .map { it.strikes }
        .sum()