const argType = Java.type("me.aberrantfox.hotbot.commandframework.parsing.ArgumentType")
const listener = Java.type("net.dv8tion.jda.core.hooks.ListenerAdapter")
const EventListener = Java.extend(listener)

function createCommand(name) {
    return container.command(name, function(){})
}

// const log = {
//     warn: (message) => container.log.warning(message),
//     info: (message) => container.log.info(message),
//     cmd: (message) => container.log.cmd(message),
//     error: (message) => container.log.error(message),
//     voice: (message) => container.log.voice(message),
//     history: (message) => container.log.history(message)
// }