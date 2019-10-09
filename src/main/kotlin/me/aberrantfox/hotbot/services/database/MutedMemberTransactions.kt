package me.aberrantfox.hotbot.services.database

import me.aberrantfox.hotbot.utility.MuteRecord
import me.aberrantfox.kjdautils.api.annotation.Service
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction

@Service
class MutedMemberTransactions {
    fun insertMutedMember(record: MuteRecord) =
            transaction {
                MutedMember.insert {
                    it[unmuteTime] = record.unmuteTime
                    it[reason] = record.reason
                    it[moderator] = record.moderator
                    it[member] = record.user
                    it[guildId] = record.guildId
                }
            }

    fun deleteMutedMember(record: MuteRecord) =
            transaction {
                MutedMember.deleteWhere {
                    Op.build {
                        (MutedMember.member eq record.user) and (MutedMember.guildId eq record.guildId)
                    }
                }
            }

    fun deleteMutedMember(userId: String, guildId:String) =
            transaction {
                MutedMember.deleteWhere {
                    Op.build {
                        (MutedMember.member eq userId) and (MutedMember.guildId eq guildId)
                    }
                }
            }

    fun getAllMutedMembers() =
            transaction {
                val mutedMembers = mutableListOf<MuteRecord>()
                val membersInDb = MutedMember.selectAll()

                membersInDb.forEach {
                    mutedMembers.add(MuteRecord(
                            it[MutedMember.unmuteTime],
                            it[MutedMember.reason],
                            it[MutedMember.moderator],
                            it[MutedMember.member],
                            it[MutedMember.guildId]
                    ))
                }
                mutedMembers
            }

    // Not to be used directly. Use the wrapper in MuteService
    fun isMemberMuted(user: String, guildId: String) =
            transaction {
                MutedMember.select {(MutedMember.member eq user) and (MutedMember.guildId eq guildId) }
                        .count() > 0
            }

    fun getUnmuteRecord(user: String, guildId: String) =
            transaction {
                val select = MutedMember.select {
                    Op.build { (MutedMember.member eq user) and (MutedMember.guildId eq guildId)}
                }.first()
                select[MutedMember.unmuteTime]
            }
}


