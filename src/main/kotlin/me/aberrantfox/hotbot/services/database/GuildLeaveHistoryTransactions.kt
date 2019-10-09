package me.aberrantfox.hotbot.services.database

import me.aberrantfox.kjdautils.api.annotation.Service
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import org.joda.time.DateTime


data class LeaveHistoryRecord(val member: String,
                              val joinDate: DateTime,
                              val leaveDate: DateTime,
                              val ban: Boolean)

@Service
class GuildLeaveHistoryTransactions {
    fun insertLeave(member: String, joinDate: DateTime, leaveDate: DateTime, guildId: String, ban: Boolean = false) =
            transaction {
                GuildLeaveHistory.insert {
                    it[GuildLeaveHistory.member] = member
                    it[GuildLeaveHistory.joinDate] = joinDate
                    it[GuildLeaveHistory.leaveDate] = leaveDate
                    it[GuildLeaveHistory.guildId] = guildId
                    it[GuildLeaveHistory.ban] = ban
                }
            }

    fun markLastRecordAsBan(member: String, guildId: String) =
            transaction {
                GuildLeaveHistory
                        .select { (GuildLeaveHistory.member eq member) and (GuildLeaveHistory.guildId eq guildId) }
                        .orderBy(GuildLeaveHistory.leaveDate to false, GuildLeaveHistory.id to false)
                        .firstOrNull()
                        ?.let { it[GuildLeaveHistory.id] }
                        ?.let { id ->
                            GuildLeaveHistory.update({ GuildLeaveHistory.id eq id }) { it[ban] = true }
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

    fun hasLeaveHistory(member: String, guildId: String) =
            transaction {
                GuildLeaveHistory.select {
                    Op.build { (GuildLeaveHistory.member eq member) and (GuildLeaveHistory.guildId eq guildId) }
                }.count() > 0
            }

}