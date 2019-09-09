const listener = Java.type("net.dv8tion.jda.api.hooks.ListenerAdapter")
const EventListener = Java.extend(listener)
const urlUtilities = Java.type("me.aberrantfox.hotbot.javautilities.UrlUtilities")
const ArgumentTypeArray = Java.type("me.aberrantfox.kjdautils.internal.command.ArgumentType[]")
const EmbedBuilder = Java.type("net.dv8tion.jda.api.EmbedBuilder")

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

    if (definition.description && typeof definition.description === "string") {
        command.description = definition.description
    }

    command.category = (definition.category && typeof definition.category === "string")
                        ? definition.category
                        : "uncategorized"

    command.execute = definition.execute
}
