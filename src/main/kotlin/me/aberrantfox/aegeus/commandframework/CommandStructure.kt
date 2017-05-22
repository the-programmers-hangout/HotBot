package me.aberrantfox.aegeus.commandframework

import org.reflections.Reflections
import org.reflections.scanners.MethodAnnotationsScanner
import java.lang.reflect.Method

enum class Permission {
    GUEST, MODERATOR, ADMIN, OWNER;

    fun matchStringToItem(choice: String): Permission? =
            when(choice.toLowerCase()) {
                "guest" -> GUEST
                "moderator" -> MODERATOR
                "admin" -> ADMIN
                "owner" -> OWNER
                 else -> null
            }
}

annotation class Command

fun produceCommandMap(): HashMap<String, Method> {
    val reflections = Reflections("me.aberrantfox.aegeus.commandframework.commands", MethodAnnotationsScanner())
    val commands = reflections.getMethodsAnnotatedWith(Command::class.java)
    val map: HashMap<String, Method> = HashMap()

    commands.forEach { map[it.name] = it }

    return map
}