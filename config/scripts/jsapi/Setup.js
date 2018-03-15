var argType = Java.type("me.aberrantfox.hotbot.commandframework.parsing.ArgumentType")
var listener = Java.type("net.dv8tion.jda.core.hooks.ListenerAdapter")
var EventListener = Java.extend(listener)

function createCommand(name) {
    return container.command(name, function(){})
}