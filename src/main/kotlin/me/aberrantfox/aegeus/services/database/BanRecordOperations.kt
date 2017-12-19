package me.aberrantfox.aegeus.services.database

import org.jetbrains.exposed.sql.Op
import org.jetbrains.exposed.sql.insert

import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update

data class BanRecord(val mod: String, val reason: String)

fun getReason(id: String) =
    transaction {
        if(hasReason(id)) {
            val select = BanRecords.select {
                Op.build { BanRecords.id eq id}
            }.first()

            BanRecord(select[BanRecords.moderator], select[BanRecords.reason])
        } else {
            null
        }
    }

fun updateOrSetReason(id: String, reason: String, moderator: String): Boolean =
    transaction {
        if(hasReason(id)) {
            BanRecords.update({BanRecords.id eq id}) {
                it[BanRecords.id] = id
                it[BanRecords.reason] = reason
                it[BanRecords.moderator] = moderator
            }
        } else {
            BanRecords.insert {
                it[BanRecords.id] = id
                it[BanRecords.reason] = reason
                it[BanRecords.moderator] = moderator
            }
        }
        true
    }

fun hasReason(id: String) =
    transaction {
        BanRecords.select {
            Op.build { BanRecords.id eq id }
        }.count() > 0
    }