const command = createCommand("jsecho")

command.expect(argType.Sentence)
command.execute((event) => {
    const arg1 = event.args[0]
    event.respond(arg1)
})