package me.aberrantfox.aegeus.commandframework.commands

import me.aberrantfox.aegeus.commandframework.ArgumentType
import me.aberrantfox.aegeus.commandframework.Command
import me.aberrantfox.aegeus.commandframework.CommandEvent
import me.aberrantfox.aegeus.extensions.idToName
import me.aberrantfox.aegeus.services.AddResponse
import me.aberrantfox.aegeus.services.UserElementPool


private val RejectionMessage = "There are no profiles in the pool."
private val profileRegex = "(\\*\\*Who are you\\?\\*\\*)(.|\\n)*(\\*\\*What is your experience\\?*\\*)(.|\\n)*(\\*\\*What languages are you really, really comfortable with\\?\\*\\*)(.|\\n)*(\\*\\*What other languages have you used/do you know of\\?\\*\\*)(.|\\n)*(\\*\\*Anything else you'd like to say\\?\\*\\*)(.|\\n)*".toRegex()

object Profiles {
    val pool: UserElementPool = UserElementPool(userLimit = 1, poolName = "Profiles")
}

@Command(ArgumentType.Joiner)
fun submitProfile(event: CommandEvent) {
    val profileText = event.args[0] as String

    if( !(profileText.matches(profileRegex)) ) {
        event.respond("Your submitted profile does not follow the format. Please re-submit a profile using the format, findable in the FAQ channel.")
        return
    }

    val response = Profiles.pool.addRecord(event.author.id, event.author.avatarUrl, profileText)

    when(response) {
        AddResponse.Accepted -> event.respond("Your profile has been added to the review pool. An administrator will review it shortly.")
        AddResponse.UserFull -> event.respond("You've already submitted a profile...")
        AddResponse.PoolFull -> event.respond("The pool is currently full. Try again in a few days.")
    }
}

@Command
fun viewProfile(event: CommandEvent) {
    val record = Profiles.pool.peek()

    if(record == null) {
        event.respond(RejectionMessage)
        return
    }

    event.respond(record.describe(event.jda, "Profile"))
}

@Command
fun acceptProfile(event: CommandEvent) {
    val record = Profiles.pool.top()

    if(record == null) {
        event.respond(RejectionMessage)
        return
    }

    val target = event.jda.getTextChannelById(event.config.profileChannel)
    target.sendMessage(
        record.prettyPrint(event.jda, "Profile")
            .setTitle("")
            .setAuthor(record.sender.idToName(event.jda), record.avatarURL,record.avatarURL)
            .build())
        .queue()
}

@Command
fun declineProfile(event: CommandEvent) {
    val top = Profiles.pool.top()

    if(top == null) {
        event.respond(RejectionMessage)
        return
    }

    event.respond(top.describe(event.jda, "Rejected Profile"))
}