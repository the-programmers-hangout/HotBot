package me.aberrantfox.aegeus.services

import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.SchemaUtils.create

fun insertInfraction(member: String, moderator: String, strikes: Int, reason: String, config: Configuration) {
    transaction {
        Strikes.insert {
            it[Strikes.member] = member
            it[Strikes.moderator] = moderator
            it[Strikes.strikes] = strikes
            it[Strikes.reason] = reason
        }
    }
}

fun setupDatabaseSchema(config: Configuration) {
    Database.connect (
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

private object Strikes : Table() {
    val id = integer("id").autoIncrement().primaryKey()
    val moderator = varchar("moderator", 18)
    val member = varchar("member", 18)
    val strikes = integer("strikes")
    val reason = text("reason")
}