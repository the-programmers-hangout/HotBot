package me.aberrantfox.hotbot.commands.utility

import me.aberrantfox.hotbot.services.MService
import me.aberrantfox.hotbot.services.Messages
import me.aberrantfox.kjdautils.api.dsl.CommandSet
import me.aberrantfox.kjdautils.api.dsl.commands
import me.aberrantfox.kjdautils.api.dsl.embed
import me.aberrantfox.kjdautils.internal.command.arguments.ChoiceArg
import me.aberrantfox.kjdautils.internal.command.arguments.SentenceArg
import java.awt.Color
import kotlin.reflect.KMutableProperty
import kotlin.reflect.full.createType
import kotlin.reflect.full.declaredMemberProperties

private val messages = Messages::class.declaredMemberProperties
        .filter { it.returnType == String::class.createType() }
        .filter { it is KMutableProperty<*> }
        .map { it as KMutableProperty<*> }
        .associateBy { it.name.toLowerCase() }

object MessageConfigArg : ChoiceArg(*messages.keys.toTypedArray()) {
    override val name = "Message"
}

@CommandSet("MessageConfiguration")
fun messageConfiguration(mService: MService) = commands {
    command("set") {
        description = "Set message for the given key. Available keys: ${messages.keys.joinToString(", ")}"
        expect(MessageConfigArg, SentenceArg)
        execute {
            val key = it.args[0] as String
            val message = it.args[1] as String

            messages[key]!!.setter.call(mService.messages, message)

            it.respond(embed {
                setTitle("Message configuration changed")
                setColor(Color.CYAN)
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
                setTitle("Message configuration")
                setColor(Color.CYAN)
                field {
                    name = key
                    value = messages[key]!!.getter.call(mService.messages) as String
                }
            })
        }
    }
}