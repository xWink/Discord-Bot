Version 1.7.1
# Purpose
Manage Computer Science Discord elective roles and channels automatically.

# Commands
### Anyone
`!help` - gives examples and instructions for using `!join` and `!leave`

`!ping` - returns "pong!" and your latency in ms. Keeps track of best/worst scores

`!score` or `!scores` - responds with best and worst latency scores and the users who got them

`!bang` - play Russian roulette

`!bangscore` or `!bangscores` - Shows high scores for attempts, deaths, survival rate, and death rate

`!roles` - Shows a list of available roles to join

`!join [role name]` - if the role already exists, it is assigned to the user. Else, the user's id and requested role are stored in a CSV file. If enough users apply for the same role, the role and a private text channel are created and all applicants are assigned to it

`!leave [role name]` - unassigns role from user and removes their application for that role from the CSV file

### Moderator+
`!giverole [mentioned user] [role name]` - if the target is not a moderator, the role is assigned to the target  

`!takerole [mentioned user] [role name]` - same as `!giverole`, but removes the role

### Owner only
`!totalchatwipe` - clones current text channel and deletes the original (effectively wipes chat history)

`!cleanelectives` - deletes all text channels in the Electives category

`!cleanroles` - deletes all non-essential roles
