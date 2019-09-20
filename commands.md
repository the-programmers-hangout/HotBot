# Commands

## Key
| Symbol     | Meaning                    |
| ---------- | -------------------------- |
| (Argument) | This argument is optional. |

## BanCommands
| Commands     | Arguments                                                           | Description                                                                           |
| ------------ | ------------------------------------------------------------------- | ------------------------------------------------------------------------------------- |
| ban          | Lower Ranked User, (Message Deletion Days [Default: 1]), Ban Reason | Bans a member for the passed reason, deleting a given number of days messages.        |
| getbanreason | User                                                                | Get the ban reason of someone logged who does not have a ban reason in the audit log. |
| setbanreason | User, Ban Reason                                                    | Set the ban reason of someone logged who does not have a ban reason in the audit log. |

## MessageConfiguration
| Commands    | Arguments             | Description                                                                                                                                |
| ----------- | --------------------- | ------------------------------------------------------------------------------------------------------------------------------------------ |
| get         | Message Name          | Get configured message for the given key. Available keys: botdescription, gagresponse, karmamessage, serverdescription, welcomedescription |
| messagekeys | <none>                | List message keys.                                                                                                                         |
| set         | Message Name, Message | Set message for the given key. Available keys: botdescription, gagresponse, karmamessage, serverdescription, welcomedescription            |

## MuteCommands
| Commands      | Arguments                              | Description                                                                                                                              |
| ------------- | -------------------------------------- | ---------------------------------------------------------------------------------------------------------------------------------------- |
| badpfp        | Lower Ranked Member                    | Notifies the user that they should change their profile pic. If they don't do that within 30 minutes, they will be automatically banned. |
| gag           | Lower Ranked Member                    | Temporarily mute a user for 5 minutes so that you can deal with something.                                                               |
| mute          | Lower Ranked Member, Time, Mute Reason | Mute a member for a specified amount of time with the given reason.                                                                      |
| remainingmute | <none>                                 | Return the remaining time of a mute                                                                                                      |
| selfmute      | (Time)                                 | Need to study and want no distractions? Mute yourself! (Length defaults to 1 hour)                                                       |
| unmute        | Lower Ranked Member                    | Unmute a previously muted member                                                                                                         |

## Notes
| Commands     | Arguments                       | Description                                                                       |
| ------------ | ------------------------------- | --------------------------------------------------------------------------------- |
| cleansenotes | Lower Ranked User               | Wipes all notes from a user (permanently deleted).                                |
| editnote     | Note ID, New Note Content       | Edits a note by ID (listed in history), replacing the content with the given text |
| note         | Lower Ranked User, Note Content | Add a note to a user's history                                                    |
| removenote   | Note ID                         | Removes a note by ID (listed in history)                                          |

## fun
| Commands | Arguments                   | Description                                               |
| -------- | --------------------------- | --------------------------------------------------------- |
| flip     | (Choice 1 | Choice 2 | ...) | Flips a coin. Optionally, print one of the choices given. |

## giveaway
| Commands       | Arguments                       | Description                                        |
| -------------- | ------------------------------- | -------------------------------------------------- |
| giveawayend    | Embed Message ID, (TextChannel) | Force end a giveaway                               |
| giveawayreroll | Embed Message ID, (TextChannel) | Reroll an ended giveaway to get a new winner       |
| giveawaystart  | Time, Giveaway Prize            | Starts a giveaway in the channel it is invoked in. |

## infractions
| Commands     | Arguments                                        | Description                                                                            |
| ------------ | ------------------------------------------------ | -------------------------------------------------------------------------------------- |
| cleanse      | Lower Ranked User                                | Completely wipe a user of all infractions. (Permanently deletes them)                  |
| history      | User                                             | Display a user's infraction history.                                                   |
| removestrike | Infraction ID                                    | Delete a particular strike by ID (listed in the history).                              |
| selfhistory  | <none>                                           | See your own infraction history.                                                       |
| strike       | Lower Ranked Member, (Weight), Infraction Reason | Give a member a weighted infraction for the given reason, defaulting to a weight of 1. |
| warn         | Lower Ranked Member, Warn Reason                 | Warn a member, giving them a 0 strike infraction with the given reason.                |

## invite
| Commands          | Arguments  | Description                              |
| ----------------- | ---------- | ---------------------------------------- |
| unwhitelistinvite | Invite URL | Remove an invitation from the whitelist. |
| whitelistinvite   | Invite URL | Allow an invite to be posted.            |

## karmacmds
| Commands    | Arguments                  | Description                                                                       |
| ----------- | -------------------------- | --------------------------------------------------------------------------------- |
| leaderboard | <none>                     | View the users with the most Karma                                                |
| setkarma    | Lower Ranked User, Integer | Set the karma of a user to a new amount                                           |
| viewkarma   | (User)                     | View the karma of a specified user. Defaults to the user who invokes the command. |

## macros
| Commands           | Arguments               | Description                                                                           |
| ------------------ | ----------------------- | ------------------------------------------------------------------------------------- |
| addmacro           | Name, Category, Message | Add a macro which will respond with the given message when invoked by the given name. |
| editmacro          | Macro, New Message      | Change a macro's response message                                                     |
| listmacros         | <none>                  | List all of the currently available macros.                                           |
| removemacro        | Macro                   | Removes a macro with the given name                                                   |
| removemacros       | Category                | Removes a whole category of macros                                                    |
| renamemacro        | Macro, New Name         | Change a macro's name, keeping the original response                                  |
| setmacrocategories | Macro..., Category      | Move one or many macros to a category.                                                |

## management
| Commands | Arguments  | Description                                                    |
| -------- | ---------- | -------------------------------------------------------------- |
| lockdown | <none>     | Force the bot to ignore anyone who isn't the owner.            |
| prefix   | New Prefix | Set the bot prefix to the specified string (Cannot be a space) |

## moderation
| Commands | Arguments                           | Description                                                                                                                                      |
| -------- | ----------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------ |
| badname  | Lower Ranked Member, Rename Reason  | Auto-nick a user with a bad name, for the given reason.                                                                                          |
| ignore   | User | TextChannel                  | Drop and don't respond to anything from the given id (user or channel)                                                                           |
| joindate | Member                              | See when a member joined the guild.                                                                                                              |
| nick     | Lower Ranked Member, (New Nickname) | Nickname a user. If no name is specified, reset the nickname.                                                                                    |
| nuke     | (TextChannel), (User...), Integer   | Delete 2 - 99 past messages in the given channel (default is the invoked channel). If users are given, only messages from those will be deleted. |

## permissions
| Commands            | Arguments                          | Description                                                                                         |
| ------------------- | ---------------------------------- | --------------------------------------------------------------------------------------------------- |
| getPermission       | Command                            | Get the current required permission level for a particular command.                                 |
| listavailable       | <none>                             | View commands available to you, based on your permission level.                                     |
| roleids             | <none>                             | Display each role in the server with its corresponding ID                                           |
| setPermission       | Command, Permission Level          | Set the permission level of the given command to the given permission level.                        |
| setPermissions      | Command Category, Permission Level | Set the permission level of all commands in a category to the given permission level.               |
| setRoleLevel        | Role, Permission Level             | Set the PermissionLevel of a particular role.                                                       |
| setallPermissions   | Permission Level                   | Set the permission of all commands to a specific permission level. Only available to the bot owner. |
| viewRoleAssignments | <none>                             | View the permission levels any roles have been assigned.                                            |
| viewpermissions     | <none>                             | View all of the commands by category, listed with their associated permission level.                |

## ranks
| Commands            | Arguments                 | Description                    |
| ------------------- | ------------------------- | ------------------------------ |
| grant               | Role, Lower Ranked Member | Grant a role to a user.        |
| listgrantableroles  | <none>                    | List grantable roles           |
| makerolegrantable   | Role                      | Allow a role to be granted.    |
| makeroleungrantable | Role Name                 | Disallow granting of this role |
| revoke              | Role, Lower Ranked Member | Revoke a role from a user      |

## security
| Commands       | Arguments | Description                                                                                                         |
| -------------- | --------- | ------------------------------------------------------------------------------------------------------------------- |
| freeAllRaiders | <none>    | For freeing all raiders from the raid queue, unmuting them as well.                                                 |
| freeRaider     | User      | Free a raider by an ID, unmuting them. Do this rather than ban people as it'll exit them from the raidView as well. |
| togglewelcome  | <none>    | Turn the welcome embed on or off.                                                                                   |
| viewRaiders    | <none>    | See what raiders are in the raidView                                                                                |

## suggestions
| Commands | Arguments                            | Description                                                                                                         |
| -------- | ------------------------------------ | ------------------------------------------------------------------------------------------------------------------- |
| pool     | Response                             | Accept or deny the suggestion at the top of the pool. If accepted, move to the community review stage               |
| poolinfo | <none>                               | Display how many suggestions are in the staging area.                                                               |
| pooltop  | <none>                               | See the suggestion in the pool next in line for review.                                                             |
| respond  | Message ID, Status, Response Message | Respond to a suggestion in the review stage, given the target id, response (accepted, denied, review), and reason.  |
| suggest  | Suggestion Message                   | Send a suggestion to the pre-lim pool. Suggestions are reviewed by a mod before they are reviewed by the community. |

## utility
| Commands           | Arguments              | Description                                                                          |
| ------------------ | ---------------------- | ------------------------------------------------------------------------------------ |
| alias              | Original, Alias        | Create an alias for the given command                                                |
| author             | <none>                 | Display project authors -- this is updated via maven filtering.                      |
| botinfo            | <none>                 | Display the bot information.                                                         |
| colour             | Hex Color              | Shows an embed with the given hex colour code                                        |
| echo               | (TextChannel), Message | Echo a message to a channel                                                          |
| exit               | <none>                 | Exit, saving configurations.                                                         |
| help               | (Word)                 | Display a help menu                                                                  |
| kill               | <none>                 | Exit, without saving configurations.                                                 |
| latex              | LaTeX Text             | A command that will parse latex                                                      |
| remindme           | Time, Reminder Message | A command that'll remind you about something after the specified time.               |
| removeAlias        | Alias                  | Remove an alias                                                                      |
| saveconfigurations | <none>                 | Save the configuration of the bot. You may want to do this if you change the prefix. |
| serverinfo         | <none>                 | Display a message giving basic server information                                    |
| uploadtext         | Text                   | Uploads the given block of text/code to hastebin and removes the invocation          |
| uptime             | <none>                 | Displays how long you have kept me, HOTBOT, AWAKE!                                   |
| version            | <none>                 | Display the bot version -- this is updated via maven filtering.                      |
| viewcreationdate   | User                   | See when a user was created                                                          |
| whatpfp            | User                   | Returns the reverse image url of a users profile picture.                            |

