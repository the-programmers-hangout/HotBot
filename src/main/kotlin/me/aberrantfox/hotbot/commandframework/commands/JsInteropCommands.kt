package me.aberrantfox.hotbot.commandframework.commands

import me.aberrantfox.hotbot.commandframework.parsing.ArgumentType
import me.aberrantfox.hotbot.dsls.command.CommandEvent
import me.aberrantfox.hotbot.dsls.command.CommandSet
import me.aberrantfox.hotbot.dsls.command.CommandsContainer
import me.aberrantfox.hotbot.dsls.command.commands
import me.aberrantfox.hotbot.services.Configuration
import me.aberrantfox.hotbot.services.configPath
import net.dv8tion.jda.core.JDA
import java.io.File
import javax.script.Invocable
import javax.script.ScriptEngine
import javax.script.ScriptEngineManager


object EngineContainer {
    var engine: ScriptEngine? = null

    fun setupScriptEngine(jda: JDA, container: CommandsContainer, config: Configuration): ScriptEngine {
        val manager = ScriptEngineManager()
        val engine = manager.getEngineByName("nashorn")
        engine.put("jda", jda)
        engine.put("container", container)
        engine.put("config", config)

        val setupScripts = File(configPath("scripts${File.separator}jsapi"))
        val custom =  File(configPath("scripts${File.separator}scripts"))

        walkDirectory(setupScripts, engine)
        walkDirectory(custom, engine)

        return engine
    }

    private fun walkDirectory(dir: File, engine: ScriptEngine) = dir.walk()
        .filter { !it.isDirectory }
        .map { it.readText() }
        .forEach { engine.eval(it) }
}

@CommandSet
fun jsCommands() = commands {
    command("eval") {
        expect(ArgumentType.Sentence)
        execute {
            val script = it.args.component1() as String
            executeJS(createVoidFunction(script), it)
        }
    }

    command("evalresponse") {
        expect(ArgumentType.Sentence)
        execute {
            val script = it.args.component1() as String
            val result = executeJS(createReturningFunction(script), it)

            it.respond(result)
        }
    }
}

private fun executeJS(script: String, event: CommandEvent): String {
    EngineContainer.engine?.eval(script)
    EngineContainer.engine?.put("event", event)

    val result = (EngineContainer.engine as Invocable).invokeFunction("evalCommandOperationReturn")
    return "$result"
}

private fun createVoidFunction(scriptBody: String) =
    """
        function evalCommandOperation() {
            $scriptBody
        };
    """.trimIndent()

private fun createReturningFunction(scriptBody: String) =
    """
        function evalCommandOperationReturn() {
            return $scriptBody
        };
    """.trimIndent()