registerCommand({
    name: "latex",
    description: "A command that will parse latex",
    category: "fun",
    expect: [SentenceArg],
    execute: (event) => {
        const text = event.args[0]
        processLatex(text, event.channel)
    }
})

function processLatex(text, channel){
    const url = "http://chart.apis.google.com/chart?cht=tx&chl=${encodeURIComponent(text)}"
    const image = urlUtilities.sendImageToChannel(url, "latex-processed.png", "Could not process latex", channel)
}
