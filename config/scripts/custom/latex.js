(function(){
    const command = createCommand("latex")

    command.expect(argType.Sentence)

    command.execute((event) => {
        const text = event.args[0]
        processLatex(text, event.channel)
    })

    function processLatex(text, channel){
        const url = "http://chart.apis.google.com/chart?cht=tx&chl=${encodeURIComponent(text)}"
        const image = urlUtilities.sendImageToChannel(url, "latex-processed.png", "Could not process latex", channel)
      }
}())