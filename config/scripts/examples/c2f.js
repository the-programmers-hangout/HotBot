const command = createCommand("c2f")

command.expect(IntegerArg)

command.execute((event) => {
    const arg1 = event.args[0]
    const response = arg1 * (9/5) + 32
    event.respond(response)
})