Version 2.2
# Purpose
This is a Discord bot intended to provide entertainment to users of the
University of Guelph Bachelor of Computer Science Discord server.

# Technologies
Written in Java with JDA API and built with Gradle.  
Using MariaDB to store user info, game data, and message logs.  
Hosted on a Google Cloud server with 24/7 uptime.

# Features

### Play Russian Roulette
![Russian Roulette Demo](demo/bot_bang.gif)

### Earn money in black jack
![Black Jack Demo](demo/bot_bj.gif)

### Track your scores
![Score Tracking Demo](demo/bot_profile.PNG)

### Share the love
![Gifting Demo](demo/bot_gift.PNG)

### Spend money on profile customization
![Market and Shopping Demo](demo/bot_market.gif)

### Instantly create and join private channels
![Roles Demo](demo/bot_roles.gif)

### Log Messages and File Uploads
![Message Logging Demo](demo/bot_logging.PNG)

### And much much more
![Help Demo](demo/bot_help.PNG)

# Reading the code? Where to start:
In src lies 3 package: command, database, and main.

command: Defines all commands used by the bot.  
database: Defines the connectors to the database
and any methods which may be used to send SQL instructions to it.  
main: Required startup code, EventListener overrides, and reading
data from a configuration file

Each package and its subpackages contain a package-info file
explaining its contents.

# Have Questions or Want to Contribute?
All inquiries should be sent to Wink#0001 on Discord.
I am always open to improving the bot in any way!
