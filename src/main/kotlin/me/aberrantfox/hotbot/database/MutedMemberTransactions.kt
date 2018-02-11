package me.aberrantfox.hotbot.database

import me.aberrantfox.hotbot.extensions.MuteRecord
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction


fun insertMutedMember(record: MuteRecord) =
    transaction {
        MutedMember.insert {
            it[MutedMember.unmuteTime] = record.unmuteTime
            it[MutedMember.reason] = record.reason
            it[MutedMember.moderator] = record.moderator
            it[MutedMember.member] = record.user
            it[MutedMember.guildId] = record.guildId
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
