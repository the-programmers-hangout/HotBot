registerCommand({
    name: "whatpfp",
    expect: [argType.User],
    execute: (event) => {
        const userPfp = event.args[0].effectiveAvatarUrl
        const reverseUrl = "https://www.google.com/searchbyimage?&image_url="
        event.respond("<${reverseUrl}${userPfp}>")
    },
    help: {
        name: "whatpfp",
        description: "Returns the reverse image url of a users profile picture.",
        category: "moderation",
        structure: "{user}",
        example: "@Fox#0001"
    }
})
