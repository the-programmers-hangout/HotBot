

#### Hotbot version 6.0.1

#### What is hotbot? 
At a glance, Hotbot is a discord bot built specifically for a programming server. It has many features, including fun, administration, channel management, an internal permissions system as well as some forms of anti-spam. 



##### User manual
This is TBD, since currently the only user of it (really) is the creator of this project -- that being said there will be a full end to end manual done in readthedocs, github pages or something similar to accommodate for people who want to set up their own instance. Please note, hotbot at this moment in time is a **single** server bot, this means that you will get unexpected behaviour if you run it on more than one guild. Multi-guild may come in 6.x.x but for now it is a far away feature. 


##### Developer documentation
- This bot depends primarily on JDA, you can find a link to JDA [here](https://github.com/DV8FromTheWorld/JDA) 
- In order to build the project, you will need maven installed. You can view more about maven [here](https://maven.apache.org/)
- Full documentation will be available later of the DSL's created and some of the other features seperated from the commands. Commands are   by in large self-documenting. 


##### Using the JavaScript API

You can add new commands and features to this bot yourself without recompiling. You can do this by writing some
simple JavaScript; this should be quite easy to do if you know how to code. 

###### Requirements
 - A text editor
 - Ability to read this: https://www.n-k.de/riding-the-nashorn/ (This JavaScript syntax is *slightly* different to regular Js.)

###### What the API exposes
The api exposes a few things:
 - The JDA object. You can use it like the JDA object found here https://github.com/DV8FromTheWorld/JDA
 - The commands container (Useful for implementing commands)
 - The configuration object (For grabbing any configuration information)

###### A basic command
Read the JavaScript code below (Please note, ES 5.1 syntax for now):
```js
//create a command object. The command will be invoked by typing (prefix)jsecho
const command = createCommand("jsecho")

//state that in order to use the command, the user must provide some arguments, e.g. ++jsecho Hi there, this is a test!
command.expect(argType.Sentence)

//Provide a function that is called when the command is executed (It is passed a CommandEvent)
command.execute((event) => {
    //Get the first argument out of the argument array.
    const arg1 = event.args[0]
    //respond to the channel that this command was called in with what they said
    event.respond(arg1)
});
```

Sample output:
```txt
++jsecho test
Bot: test
```

###### Where do I put the code?
In config/scripts/custom. This directory may not exist, create it.

More documentation to come!

##### Contributing

- Keep pull requests small and easily digestable, stick to one feature or one atomic change within the project.
- If you are going to submit a feature PR, make sure that you ask me if I want it in the bot first, since if I don't want it, it won't end   up in the bot, and you will have wasted your time (potentially, maybe you can still use it?)
- Try to keep commits somewhat atomic, if you need to make a feature which requires various changes to the code base, try to commit each     of these individually. 
 
