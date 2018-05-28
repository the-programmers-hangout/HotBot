package me.aberrantfox.hotbot.database

import me.aberrantfox.hotbot.commandframework.commands.utility.SuggestionStatus
import me.aberrantfox.hotbot.services.Configuration
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.StdOutSqlLogger
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.transactions.transaction

fun setupDatabaseSchema(config: Configuration) {
    val dbParams = listOf(
            "useUnicode=true",
            "useJDBCCompliantTimezoneShift=true",
            "useLegacyDatetimeCode=true",
            "serverTimezone=UTC",
            "nullNamePatternMatchesAll=true",
            "useSSL=false"
    )

    val url = "jdbc:mysql://${config.databaseCredentials.hostname}/${config.databaseCredentials.database}?${dbParams.joinToString("&")}"

    Database.connect(
            url = url,
            driver = "com.mysql.jdbc.Driver",
            password = config.databaseCredentials.password,
            user = config.databaseCredentials.username
    )

    transaction {
        SchemaUtils.create(Strikes, HistoryCount, Suggestions, BanRecords, CommandPermissions,
                ChannelResources, Notes, MutedMember, IgnoredIDs, Reminder)
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

object HistoryCount: Table() {
    val member = varchar("member", 18).primaryKey()
    val historyCount = integer("historyCount")
}

object Notes: Table() {
    val id = integer("id").autoIncrement().primaryKey()
    val moderator = varchar("moderator", 18)
    val member = varchar("member", 18)
    val note = text("note")
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

object BanRecords : Table() {
    val id = varchar("id", 18).primaryKey()
    val reason = text("reason")
    val moderator = varchar("moderator", 18)
}

object CommandPermissions : Table() {
    val id = integer("id").autoIncrement().primaryKey()
    val roleID = varchar("roleID", 18)
    val commandName = varchar("name", 256)
}

object ChannelResources : Table() {
    val id = integer("id").autoIncrement().primaryKey()
    val channel = varchar("channel", 18)
    val section = varchar("section", 64)
    val info = varchar("info", 255)
}

object MutedMember : Table() {
    val id = integer("id").autoIncrement().primaryKey()
    val member = varchar("member", 18)
    val unmuteTime = long("unmuteTime")
    val reason = text("reason")
    val moderator = varchar("moderator", 18)
    val guildId = varchar("guildId", 18)
}

object IgnoredIDs : Table() {
    val id = varchar("id", 18).primaryKey()
}

object Reminder : Table() {
    val id = integer("id").autoIncrement().primaryKey()
    val member = varchar("member", 18)
    val message = text("message")
    val remindTime = long("remindTime")
}
