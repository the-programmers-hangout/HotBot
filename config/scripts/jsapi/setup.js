const argType = Java.type("me.aberrantfox.hotbot.commandframework.parsing.ArgumentType")
const listener = Java.type("net.dv8tion.jda.core.hooks.ListenerAdapter")
const EventListener = Java.extend(listener)
const urlUtilities = Java.type("me.aberrantfox.hotbot.javautilities.UrlUtilities")
const ArgumentTypeArray = Java.type("me.aberrantfox.hotbot.commandframework.parsing.ArgumentType[]")

function createCommand(name) {
    return container.command(name, function(){})
}

function registerCommand(definition) {
    if(!definition.name) {
        throw new Error("definition.name must be defined.")
    } 

    if(!definition.execute) {
        throw new Error("definition.execute must be defined.")
    }

    if(typeof definition.execute !== "function") {
        throw new Error("definition.execute must be a function")
    }
    
    if(typeof definition.name !== "string") {
        throw new Error("definition.name must be a string.")
    }

    const command = createCommand(definition.name)
    
    if(definition.expect && typeof definition.expect === "object") {
        command.expect(Java.to(definition.expect, ArgumentTypeArray))
    }
    
    command.execute = definition.execute

    if(definition.help) {
        help.add(definition.help.name, definition.help.description, definition.help.category, definition.help.structure, definition.help.example)
    }
}

const log = container.log
