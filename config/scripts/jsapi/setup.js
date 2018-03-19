const argType = Java.type("me.aberrantfox.hotbot.commandframework.parsing.ArgumentType")
const listener = Java.type("net.dv8tion.jda.core.hooks.ListenerAdapter")
const EventListener = Java.extend(listener)
const urlUtilities = Java.type("me.aberrantfox.hotbot.javautilities.UrlUtilities")

function createCommand(name) {
    return container.command(name, function(){})
}

const log = container.log