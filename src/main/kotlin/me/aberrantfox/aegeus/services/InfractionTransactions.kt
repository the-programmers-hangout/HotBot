package me.aberrantfox.aegeus.services

import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.SchemaUtils.create

fun insertInfraction(member: String, moderator: String, strikes: Int, reason: String) =
        transaction {
            Strikes.insert {
                it[Strikes.member] = member
                it[Strikes.moderator] = moderator
                it[Strikes.strikes] = strikes
                it[Strikes.reason] = reason
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
                        it[Strikes.reason]))
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

fun setupDatabaseSchema(config: Configuration) {
    Database.connect(
            url = "jdbc:mysql://localhost/hotbot?useUnicode=true&useJDBCCompliantTimezoneShift=true&useLegacyDatetimeCode=false&serverTimezone=UTC&nullNamePatternMatchesAll=true",
            driver = "com.mysql.cj.jdbc.Driver",
            password = config.databaseCredentials.password,
            user = config.databaseCredentials.username
    )

    transaction {
        create(Strikes)
        logger.addLogger(StdOutSqlLogger)
    }
}

data class StrikeRecord(val id: Int, val moderator: String, val member: String, val strikes: Int, val reason: String)

private object Strikes : Table() {
    val id = integer("id").autoIncrement().primaryKey()
    val moderator = varchar("moderator", 18)
    val member = varchar("member", 18)
    val strikes = integer("strikes")
    val reason = text("reason")
}