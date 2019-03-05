# Purpose
Manage Computer Science Discord elective roles and channels automatically.

## Usage
1. Change the token in TestBot.java to match your bot's token
2. Change the directory of variable path in MyEventListener.java (line 30)
3. Change the package in both files to your package name
4. Run the bot

# Commands
### Owner only
!totalchatwipe - clones current text channel and deletes the original (effectively wipes chat history)

!cleanelectives - deletes all text channels in the Electives category  

!cleanroles - deletes all non-essential roles  

### Moderator+
!giverole [mentioned user] [role name] - if the target is not a moderator, the role is assigned to the target  

!takerole [mentioned user] [role name] - same as !giverole, but removes the role  

### Anyone
!ping - returns "pong!" and your latency in ms  

!join [role name] - if the role already exists, it is assigned to the user. Else, the user's id and requested role are stored in a CSV file. If enough users apply for the same role, the role and a private text channel are created and all applicants are assigned to it

!leave [role name] - unassigns role from user and removes their application for that role from the csv file

!help - gives examples and instructions for using !join and !leave
