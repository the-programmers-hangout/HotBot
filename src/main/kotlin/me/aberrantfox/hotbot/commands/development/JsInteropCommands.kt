package me.aberrantfox.hotbot.commands.development

import me.aberrantfox.hotbot.services.ScriptEngineService
import me.aberrantfox.kjdautils.api.dsl.*
import me.aberrantfox.kjdautils.internal.arguments.SentenceArg
import javax.script.Invocable

private const val functionName = "functionScope"

@CommandSet("api")
fun jsCommands(scriptEngineService: ScriptEngineService) = commands {
    command("eval") {
        description = "Evaluate Nashorn JavaScript code - without an automatic response."
        expect(SentenceArg("Nashorn JavaScript Code"))
        execute {
            val script = it.args.component1() as String
            val functionContext = createFunctionContext(script)

            try {
                scriptEngineService.engine.eval(functionContext)
                (scriptEngineService.engine as Invocable).invokeFunction(functionName, it)
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