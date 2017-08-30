package me.aberrantfox.aegeus.services.database

import me.aberrantfox.aegeus.commandframework.commands.SuggestionStatus
import me.aberrantfox.aegeus.services.Configuration
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.StdOutSqlLogger
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.transactions.transaction


fun setupDatabaseSchema(config: Configuration) {
    Database.connect(
            url = "jdbc:mysql://localhost/hotbot?useUnicode=true&useJDBCCompliantTimezoneShift=true&useLegacyDatetimeCode=false&serverTimezone=UTC&nullNamePatternMatchesAll=true",
            driver = "com.mysql.cj.jdbc.Driver",
            password = config.databaseCredentials.password,
            user = config.databaseCredentials.username
    )

    transaction {
        SchemaUtils.create(Strikes, Suggestions)
        logger.addLogger(StdOutSqlLogger)
    }
}

object Strikes : Table() {
    val id = integer("id").autoIncrement().primaryKey()
    val moderator = varchar("moderator", 18)
    val member = varchar("member", 18)
    val strikes = integer("strikes")
    val reason = text("reason")
    val date = date("date")
}

object Suggestions : Table() {
    val id = varchar("id", 18).primaryKey()
    val avatarURL = varchar("url", 1024)
    val member = varchar("member", 18)
    val status = enumeration("status", SuggestionStatus::class.java)
    val idea = text("idea")
    val date = date("date")
}