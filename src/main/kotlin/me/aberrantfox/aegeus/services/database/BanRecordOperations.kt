package me.aberrantfox.aegeus.services.database

import org.jetbrains.exposed.sql.Op
import org.jetbrains.exposed.sql.insert

import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update


fun getReason(id: String) =
    transaction {
        if(hasReason(id)) {
            val select = BanRecords.select {
                Op.build { BanRecords.id eq id}
            }.first()

            select[BanRecords.reason]
        } else {
            null
        }
    }

fun updateOrSetReason(id: String, reason: String): Boolean =
    transaction {
        if(hasReason(id)) {

            BanRecords.update({BanRecords.id eq id}) {
                it[BanRecords.id] = id
                it[BanRecords.reason] = reason
            }
        } else {
            BanRecords.insert {
                it[BanRecords.id] = id
                it[BanRecords.reason] = reason
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