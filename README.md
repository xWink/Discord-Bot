Version 1.11
# Purpose
Manage Computer Science Discord elective roles and channels and entertain users with games.

# Commands
### Anyone
`!help` - Displays other commands that can be used.

`!ping` - Returns "pong!" and your latency in ms. Keeps track of user's best/worst scores.

`!bang` - Play Russian roulette.

`!mybang` and `!daily` - Shows player's bang scores and when their daily reward resets.

`!bangscore` or `!bangscores` - Shows high scores for attempts, deaths, survival rate, and death rate.

`!bet`, `!hit`, `!stand` - Play blackjack against the computer.

`!market` and `!buy <item #>` - View and purchase roles/colours.

`!roles` - Shows a list of available roles to join.

`!join [role name]` - If the role already exists, it is assigned to the user. Otherwise, the user's id and requested role are stored in a database. If enough users apply for the same role, the role and a private text channel are created and all applicants are assigned to it.

`!leave [role name]` - Unassigns the role from user and removes their application for that role from the database.

### Moderator+
`!giverole [mentioned user] [role name]` - If the target is not a moderator, the role is assigned to the target.

`!takerole [mentioned user] [role name]` - Same as `!giverole`, but removes the role.

### Owner only
`!totalchatwipe` - Clones the current text channel and deletes the original (effectively wipes chat history).

`!cleanelectives` - Deletes all text channels in the Electives category.

`!cleanroles` - Deletes all roles except Moderator and Verified Students.
