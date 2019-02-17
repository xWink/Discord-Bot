# Purpose
Manage Computer Science Discord elective roles and channels automatically.

## Usage
1. Change the <token> in TestBot.java to match your bot's token
2. Change the directory of variable path (line 30) in MyEventListener.java
3. Run the bot

# Commands
### Owner only
!totalchatwipe - clones current text channel and deletes the original  
!cleanelectives - deletes all text channels in the Electives category  
!cleanroles - deletes all non-essential roles  

### Moderator+
!giverole [mentioned user] [role name] - if the target is not a moderator, the role is assigned to the target  
!takerole [mentioned user] [role name] - same as !giverole, but removes the role  

### Anyone
!ping - returns "pong!" and your latency in ms  
!join [role name] - stores the user's id and requested role in a CSV file. If enough users apply for the same role, the role and private text channel is created and all previous applicants are assigned the role. If the role already exists, the user is given the role
