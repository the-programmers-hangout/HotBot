package me.aberrantfox.hotbot.commandframework.commands

import me.aberrantfox.hotbot.commandframework.parsing.ArgumentType
import me.aberrantfox.hotbot.dsls.command.CommandSet
import me.aberrantfox.hotbot.dsls.command.CommandsContainer
import me.aberrantfox.hotbot.dsls.command.commands
import me.aberrantfox.hotbot.services.Configuration
import me.aberrantfox.hotbot.services.configPath
import net.dv8tion.jda.core.JDA
import java.io.File
import javax.script.Invocable
import javax.script.ScriptEngine
import jdk.nashorn.api.scripting.NashornScriptEngineFactory

object EngineContainer {
    var engine: ScriptEngine? = null

    fun setupScriptEngine(jda: JDA, container: CommandsContainer, config: Configuration): ScriptEngine {
        val engine = NashornScriptEngineFactory().getScriptEngine("--language=es6", "-scripting")
        engine.put("jda", jda)
        engine.put("container", container)
        engine.put("config", config)

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

private const val functionName = "functionScope"

@CommandSet
fun jsCommands() = commands {
    command("eval") {
        expect(ArgumentType.Sentence)
        execute {
            val script = it.args.component1() as String
            val functionContext = createFunctionContext(script)

            try {
                EngineContainer.engine?.eval(functionContext)
                (EngineContainer.engine as Invocable).invokeFunction(functionName, it)
            } catch (e: Exception) {
                it.respond("${e.message} - **cause** - ${e.cause}")
            }
        }
    }
}

private fun createFunctionContext(scriptBody: String) =
    """
        function $functionName(event) {
            $scriptBody
        };
    """.trimIndent()