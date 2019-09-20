package me.aberrantfox.hotbot.commands.administration

import me.aberrantfox.hotbot.arguments.LowerUserArg
import me.aberrantfox.hotbot.database.insertNote
import me.aberrantfox.hotbot.database.removeAllNotesByUser
import me.aberrantfox.hotbot.database.removeNoteById
import me.aberrantfox.hotbot.database.replaceNote
import me.aberrantfox.kjdautils.api.dsl.CommandSet
import me.aberrantfox.kjdautils.api.dsl.commands
import me.aberrantfox.kjdautils.extensions.jda.descriptor
import me.aberrantfox.kjdautils.internal.arguments.IntegerArg
import me.aberrantfox.kjdautils.internal.arguments.SentenceArg
import net.dv8tion.jda.api.entities.User

@CommandSet("Notes")
fun createCommandSet() = commands {
    command("note") {
        description = "Add a note to a user's history"
        expect(LowerUserArg, SentenceArg("Note Content"))
        execute {
            val target = it.args.component1() as User
            val note = it.args.component2() as String
            val noteId = insertNote(target.id, it.author.id, note)

            it.respond("Note added. ID: $noteId")
        }
    }

    command("removenote") {
        description = "Removes a note by ID (listed in history)"
        expect(IntegerArg("Note ID"))
        execute {
            val noteId = it.args.component1() as Int
            val notesRemoved = removeNoteById(noteId)

            it.respond("Number of notes removed: $notesRemoved")
        }
    }

    command("cleansenotes") {
        description = "Wipes all notes from a user (permanently deleted)."
        expect(LowerUserArg)
        execute {
            val target = it.args.component1() as User
            val amount = removeAllNotesByUser(target.id)

            it.respond("Notes for ${target.descriptor()} have been wiped. Total removed: $amount")
        }
    }

    command("editnote") {
        description = "Edits a note by ID (listed in history), replacing the content with the given text"
        expect(IntegerArg("Note ID"), SentenceArg("New Note Content"))
        execute {
            //get user id that note is placed on, use that in insertNote part. If possible, try to replace note at ID with a different note, rather than a different ID.
            val noteId = it.args.component1() as Int
            val note = it.args.component2() as String
            replaceNote(noteId, note, it.author.id)
            it.respond("Note $noteId has been updated.")
        }
    }
}