package me.aberrantfox.hotbot.database

import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction

data class HistoryCountTransactions(val member: String,
                                    val historyCount: String)


fun incrementOrSetHistoryCount(member: String): Boolean =
    transaction {
        if(hasHistoryCount(member)){
            HistoryCount.update({HistoryCount.member eq member}) {
                with(SqlExpressionBuilder){
                    it.update(HistoryCount.historyCount, HistoryCount.historyCount+ 1)
                }
            }
        }else{
            HistoryCount.insert {
                it[HistoryCount.member] = member
                it[HistoryCount.historyCount] = 1
            }
        }
        true
    }

fun hasHistoryCount(member: String) =
        transaction {
            HistoryCount.select {
                Op.build { HistoryCount.member eq member }
            }.count() > 0
        }