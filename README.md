Version 1.11 -> 2.0
# Purpose
Manage Computer Science Discord elective roles and channels and entertain users with games

# Commands
### Anyone
`!help` - displays other commands that can be used

`!ping` - returns "pong!" and your latency in ms. Keeps track of user's best/worst scores

`!bang` - play Russian roulette

`!mybang` and `!daily` - shows player's bang scores and when their daily reward resets

`!bangscore` or `!bangscores` - shows high scores for attempts, deaths, survival rate, and death rate

`!bet`, `!hit`, `!stand` - play blackjack against the computer

`!market` and `!buy <item #>` - view and purchase roles/colours

`!roles` - shows a list of available roles to join

`!join [role name]` - if the role already exists, it is assigned to the user. Else, the user's id and requested role are stored in a database. If enough users apply for the same role, the role and a private text channel are created and all applicants are assigned to it

`!leave [role name]` - unassigns role from user and removes their application for that role from the database

`!info [course ID]` - displays relevant information about a specified course at UoGuelph

`!karma` - shows a user's karma (upvotes and downvotes)

### Owner only
`!totalchatwipe` - clones current text channel and deletes the original (effectively wipes chat history)

`!cleanelectives` - deletes all text channels in the Electives category

`!cleanroles` - deletes all roles except Moderator and Verified Students
