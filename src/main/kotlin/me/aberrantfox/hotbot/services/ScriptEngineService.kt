package me.aberrantfox.hotbot.services

import jdk.nashorn.api.scripting.NashornScriptEngineFactory
import me.aberrantfox.kjdautils.api.annotation.Service
import me.aberrantfox.kjdautils.api.dsl.CommandsContainer
import me.aberrantfox.kjdautils.internal.logging.BotLogger
import net.dv8tion.jda.core.JDA
import java.io.File
import javax.script.ScriptEngine

@Service
class ScriptEngineService(jda: JDA, commandsContainer: CommandsContainer, config: Configuration, logger: BotLogger) {
    val engine: ScriptEngine = setupScriptEngine(jda, commandsContainer, config, logger)

    private fun setupScriptEngine(jda: JDA, container: CommandsContainer, config: Configuration, logger: BotLogger): ScriptEngine {
        val engine = NashornScriptEngineFactory().getScriptEngine("--language=es6", "-scripting")
        engine.put("jda", jda)
        engine.put("container", container)
        engine.put("config", config)
        engine.put("log", logger)

        val setupScripts = File(configPath("scripts${File.separator}jsapi"))
        val custom =  File(configPath("scripts${File.separator}custom"))

        walkDirectory(setupScripts, engine)
        walkDirectory(custom, engine)

        return engine
    }

    private fun walkDirectory(dir: File, engine: ScriptEngine) = dir.walk()
        .filter { !it.isDirectory }
        .map { it.readText() }
        .forEach { engine.eval(it) }
}