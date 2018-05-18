package me.aberrantfox.hotbot.database

import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction

fun forEachIgnoredID(action: (String) -> Unit) =
        transaction {
            IgnoredIDs.selectAll()
                      .map { it[IgnoredIDs.id] }
                      .forEach({ action.invoke(it) })
        }

fun insertIgnoredID(id: String) =
        transaction {
            IgnoredIDs.insert {
                it[IgnoredIDs.id] = id
            }
        }

fun deleteIgnoredID(id: String) =
        transaction {
            IgnoredIDs.deleteWhere {
                Op.build { IgnoredIDs.id eq id }
            }
        }
