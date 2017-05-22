package me.aberrantfox.aegeus.businessobjects

import com.github.salomonbrys.kotson.fromJson
import com.google.gson.Gson
import me.aberrantfox.aegeus.commandframework.Permission
import java.io.File
import java.lang.reflect.Method

data class Configuration(val token: String = "insert-token",
                         val ownerID: String = "insert-id",
                         val prefix: String = "insert-prefix",
                         val permissionMap: MutableMap<String, Permission> = HashMap())

fun produceConfigOrFail(commandMap: HashMap<String, Method>): Configuration {
    val configFile: File = File("config.json")
    val gson: Gson = Gson()

    if(!configFile.exists()) {
        val jsonData: String = gson.toJson(Configuration())
        configFile.printWriter().use { it.print(jsonData) }

        println("The default configuration has been generated." +
                " Please fill in this configuration in order to use the bot.")
        System.exit(0)
    }

    val json: String = configFile.readLines().stream().reduce("", { a: String, b: String -> a + b })
    val configuration = gson.fromJson<Configuration>(json)

    commandMap.keys.filter { !configuration.permissionMap.containsKey(it) }
            .forEach { configuration.permissionMap[it] = Permission.ADMIN }

    return configuration
}



