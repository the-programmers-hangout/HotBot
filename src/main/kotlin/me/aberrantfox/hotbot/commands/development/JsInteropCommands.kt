package me.aberrantfox.hotbot.commands.development

import jdk.nashorn.api.scripting.NashornScriptEngineFactory
import me.aberrantfox.hotbot.services.Configuration
import me.aberrantfox.hotbot.services.configPath
import me.aberrantfox.kjdautils.api.dsl.CommandSet
import me.aberrantfox.kjdautils.api.dsl.CommandsContainer
import me.aberrantfox.kjdautils.api.dsl.commands
import me.aberrantfox.kjdautils.internal.command.arguments.SentenceArg
import me.aberrantfox.kjdautils.internal.logging.BotLogger
import net.dv8tion.jda.core.JDA
import java.io.File
import javax.script.Invocable
import javax.script.ScriptEngine

object EngineContainer {
    var engine: ScriptEngine? = null

    fun setupScriptEngine(jda: JDA, container: CommandsContainer, config: Configuration, logger: BotLogger): ScriptEngine {
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

private const val functionName = "functionScope"

@CommandSet("api")
fun jsCommands() = commands {
    command("eval") {
        description = "Evaluate Nashorn JavaScript code - without an automatic response."
        expect(SentenceArg)
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