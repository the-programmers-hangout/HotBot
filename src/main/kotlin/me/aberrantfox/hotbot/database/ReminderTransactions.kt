package me.aberrantfox.hotbot.database

import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction

data class ReminderRecord(val id: Int,
                          val member: String,
                          val message: String,
                          val remindTime: Long)

fun forEachReminder(action: (ReminderRecord) -> Unit) =
        transaction {
            Reminder.selectAll()
                      .map { ReminderRecord(
                              it[Reminder.id],
                              it[Reminder.member],
                              it[Reminder.message],
                              it[Reminder.remindTime])}
                      .forEach({ action.invoke(it) })
        }

fun insertReminder(member: String, message: String, remindTime: Long) =
        transaction {
            Reminder.insert {
                it[Reminder.member] = member
                it[Reminder.message] = message
                it[Reminder.remindTime] = remindTime
            }
        }

fun deleteReminder(member: String, message: String) =
        transaction {
            Reminder.deleteWhere {
                Op.build { (Reminder.member eq member) and (Reminder.message eq message) }
            }
        }

