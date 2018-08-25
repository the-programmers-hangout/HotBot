package me.aberrantfox.hotbot.database

import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction


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

fun getHistoryCount(member: String): Int {
    if(!hasHistoryCount(member)) return 0

    var historyCount = 0

    transaction {
        val select = HistoryCount.select {
            Op.build { HistoryCount.member eq member }
        }.first()

        historyCount = select[HistoryCount.historyCount]
    }

    return historyCount
}

fun hasHistoryCount(member: String) =
        transaction {
            HistoryCount.select {
                Op.build { HistoryCount.member eq member }
            }.count() > 0
        }

fun resetHistoryCount(member: String) {
    if (hasHistoryCount(member))
        transaction {
            HistoryCount.update({ HistoryCount.member eq member}) {
                it[HistoryCount.historyCount] = 0
            }
        }
}