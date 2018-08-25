package me.aberrantfox.hotbot.database


import net.dv8tion.jda.core.entities.User
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction

data class KarmaRecord(val who: String, val karma: Int)

fun leaderBoard() = transaction {
    KarmaTable.selectAll().orderBy(KarmaTable.karma, false).limit(10).toList()
            .map { KarmaRecord(it[KarmaTable.member], it[KarmaTable.karma]) }
}

fun addKarma(user: User, amount: Int) = transaction {
    val karma = getKarma(user) + amount
    setKarma(user, karma)
}

fun getKarma(user: User) = transaction {
    if(hasKarma(user)) {
        KarmaTable.select(Op.build { KarmaTable.member eq user.id }).first()[KarmaTable.karma]
    } else {
        setKarma(user, 0)
        0
    }
}

fun setKarma(user: User, amount: Int): Unit = transaction {
        if( !(hasKarma(user)) ) {
            KarmaTable.insert {
                it[KarmaTable.member] = user.id
                it[KarmaTable.karma] = amount
            }
        } else {
            KarmaTable.update({ KarmaTable.member eq user.id }) {
                with(SqlExpressionBuilder) {
                    it[KarmaTable.karma] = amount
                }
            }
        }
    }

fun hasKarma(user: User) = transaction {
    KarmaTable.select(Op.build { KarmaTable.member eq user.id }).count() > 0
}

fun removeKarma(user: User) = transaction {
    KarmaTable.deleteWhere { Op.build { KarmaTable.member eq user.id } }
}