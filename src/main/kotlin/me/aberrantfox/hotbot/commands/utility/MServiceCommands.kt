package me.aberrantfox.hotbot.commands.utility

import me.aberrantfox.hotbot.services.*
import me.aberrantfox.kjdautils.api.dsl.*
import me.aberrantfox.kjdautils.internal.arguments.*
import java.awt.Color
import kotlin.reflect.KMutableProperty
import kotlin.reflect.full.*

object MessageConfigArg : ChoiceArg("Message Name", *messages.keys.toTypedArray())

@CommandSet("MessageConfiguration")
fun messageConfiguration(messageService: Messages) = commands {
    command("messagekeys") {
        description = "List message keys."
        execute {
            it.respond("Available keys: ${messages.keys.joinToString(", ")}")
        }
    }

    command("set") {
        description = "Set message for the given key. Available keys: ${messages.keys.joinToString(", ")}"
        expect(MessageConfigArg, SentenceArg("Message"))
        execute {
            val key = it.args[0] as String
            val message = it.args[1] as String

            messages.getValue(key).setter.call(messages, message)

            it.respond(embed {
                title = "Message configuration changed"
                color = Color.CYAN
                field {
                    name = key
                    value = message
                }
            })
        }
    }

    command("get") {
        description = "Get configured message for the given key. Available keys: ${messages.keys.joinToString(", ")}"
        expect(MessageConfigArg)
        execute {
            val key = it.args[0] as String

            it.respond(embed {
                title = "Message configuration"
                color = Color.CYAN
                field {
                    name = key
                    value = messages.getValue(key).getter.call(messages) as String
                }
            })
        }
    }
}

private val messages = Messages::class.declaredMemberProperties
        .filter { it.returnType == String::class.createType() }
        .filterIsInstance<KMutableProperty<*>>()
        .associateBy { it.name.toLowerCase() }