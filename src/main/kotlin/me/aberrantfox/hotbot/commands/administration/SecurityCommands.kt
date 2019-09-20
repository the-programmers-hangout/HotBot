package me.aberrantfox.hotbot.commands.administration

import me.aberrantfox.kjdautils.api.dsl.CommandSet
import me.aberrantfox.kjdautils.api.dsl.commands

var sendWelcome = true

@CommandSet("security")
fun securityCommands() = commands {
    command("togglewelcome"){
        description = "Turn the welcome embed on or off."
        execute {
            if(sendWelcome){
                sendWelcome = false
                it.respond("No longer sending welcome messages.")
            }else{
                sendWelcome = true
                it.respond("Sending welcome messages again.")
            }
        }
    }
}
