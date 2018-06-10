package me.aberrantfox.hotbot.database

import org.jetbrains.exposed.sql.Op
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import org.joda.time.DateTime


data class LeaveHistoryRecord(val member: String,
                              val joinDate: DateTime,
                              val leaveDate: DateTime,
                              val ban: Boolean)

fun insertLeave(member: String, joinDate: DateTime, guildId: String, ban: Boolean = false) =
        transaction {
            GuildLeaveHistory.insert {
                it[GuildLeaveHistory.member] = member
                it[GuildLeaveHistory.joinDate] = joinDate
                it[GuildLeaveHistory.leaveDate] = DateTime.now()
                it[GuildLeaveHistory.guildId] = guildId
                it[GuildLeaveHistory.ban] = ban
            }
        }

fun getLeaveHistory(member: String, guildId: String) =
        transaction {
            GuildLeaveHistory.select {
                Op.build { (GuildLeaveHistory.member eq member) and (GuildLeaveHistory.guildId eq guildId) }
            }.map {
                LeaveHistoryRecord(
                        it[GuildLeaveHistory.member],
                        it[GuildLeaveHistory.joinDate],
                        it[GuildLeaveHistory.leaveDate],
                        it[GuildLeaveHistory.ban])
            }
        }
