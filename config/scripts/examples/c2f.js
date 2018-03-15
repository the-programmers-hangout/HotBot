var command = createCommand("c2f")

command.expect(argType.Integer)

command.execute( function(event) {
    var arg1 = event.args[0]
    var response = arg1 * (9/5) + 32
    event.respond(response)
});