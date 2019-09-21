package me.aberrantfox.hotbot

import me.aberrantfox.kjdautils.api.startBot
import kotlin.system.exitProcess

fun main(args: Array<String>) {
    val token = args.firstOrNull()

    if(token == null || token == "UNSET") {
        println("You must specify the token with the -e flag when running via docker, or as the first command line param.")
        exitProcess(-1)
    }

    start(token)
}

fun start(token: String) = startBot(token) {
    configure {
        prefix = "+"
        globalPath = "me.aberrantfox.hotbot"
    }
}
