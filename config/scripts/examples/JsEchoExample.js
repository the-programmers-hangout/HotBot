var command = createCommand("jsecho")
command.expect(argType.Sentence)
command.execute( function(event) {
    var arg1 = event.args[0]
    event.respond(arg1)
});