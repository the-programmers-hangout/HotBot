package me.aberrantfox.hotbot.database

import org.jetbrains.exposed.sql.Op
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import org.joda.time.DateTime

data class NoteRecord(val id: Int,
                      val moderator: String,
                      val member: String,
                      val note: String,
                      val dateTime: DateTime)

fun insertNote(member: String, moderator: String, note: String) =
        transaction {
            Notes.insert {
                it[Notes.member] = member
                it[Notes.moderator] = moderator
                it[Notes.note] = note
                it[Notes.date] = DateTime.now()
            }
        }

fun getNotesByUser(userId: String) =
        transaction {
            val notes = mutableListOf<NoteRecord>()

            val selectedNotes = Notes.select {
                Notes.member eq userId
            }

            selectedNotes.forEach {
                notes.add(NoteRecord(it[Notes.id],
                        it[Notes.moderator],
                        it[Notes.member],
                        it[Notes.note],
                        it[Notes.date]))
            }

            notes
        }

fun removeNoteById(noteId: Int) =
        transaction {
            val numberDeleted = Notes.deleteWhere {
                Notes.id eq noteId
            }

            numberDeleted
        }

fun removeAllNotesByUser(userId: String) =
        transaction {
            val numberDeleted = Notes.deleteWhere {
                Notes.member eq userId
            }

            numberDeleted
        }