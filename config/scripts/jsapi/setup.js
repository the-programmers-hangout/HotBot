const argType = Java.type("me.aberrantfox.hotbot.commandframework.parsing.ArgumentType")
const listener = Java.type("net.dv8tion.jda.core.hooks.ListenerAdapter")
const EventListener = Java.extend(listener)
const urlUtilities = Java.type("me.aberrantfox.hotbot.javautilities.UrlUtilities")
const ArgumentTypeArray = Java.type("me.aberrantfox.hotbot.commandframework.parsing.ArgumentType[]")

function createCommand(name) {
    return container.command(name, function(){})
}

function registerCommand(definition) {
    if(!definition.name || !definition.execute || typeof definition.execute !== "function" || typeof definition.name !== "string") {
        throw new Error("Command definitions must specify both a name property (String) and an execute property (function)")
    }

    const command = createCommand(definition.name)
    
    if(definition.expect && typeof definition.expect === 'object') {
        command.expect(Java.to(definition.expect, ArgumentTypeArray))
    }
    
    command.execute = definition.execute

    if(definition.help) {
        help.add(definition.name, definition.description, definition.category, definition.structure, definition.example)
    }
}

const log = container.log