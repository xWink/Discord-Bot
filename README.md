Version 1.3
# Purpose
Manage Computer Science Discord elective roles and channels automatically.

# Commands
### Owner only
`!totalchatwipe` - clones current text channel and deletes the original (effectively wipes chat history)

`!cleanelectives` - deletes all text channels in the Electives category  

`!cleanroles` - deletes all non-essential roles  

### Moderator+
`!giverole [mentioned user] [role name]` - if the target is not a moderator, the role is assigned to the target  

`!takerole [mentioned user] [role name]` - same as `!giverole`, but removes the role  

### Anyone
`!ping` - returns your latency in ms. Keeps track of best/worst scores

`!bang` - play Russian roulette. Tracks number of attempts and deaths for each player.

`!join [role name]` - if the role already exists, it is assigned to the user. Else, the user's id and requested role are stored in a CSV file. If enough users apply for the same role, the role and a private text channel are created and all applicants are assigned to it

`!leave [role name]` - unassigns role from user and removes their application for that role from the CSV file

`!help` - gives examples and instructions for using `!join` and `!leave`

`!score` or `!scores` - responds with best and worst latency scores and the users who got them
