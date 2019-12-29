Version 2.0
# Purpose
This is a Discord bot intended to provide entertainment to users of the
University of Guelph Bachelor of Computer Science Discord server.

# Features
The initial purpose of the bot was to provide a method by which students taking the same
electives could instantly create or join private message channels within the server
via commands and help each other prepare for tests/assignments while keeping the server
clutter-free for everyone else.

The bot comes with a series of games, including black jack,
Russian Roulette, and tic tac toe. These games reward users with a currency
known as GryphCoins that can be spent on cosmetic changes to their profile.

There is also a karma system, allowing users to upvote and downvote each
other's messages based on productivity.

Scorekeeping, karma data, and other information about how users interact with
the bot is all stored on a MariaDB server hosted on Google Cloud Platform.
Updates to the database are made using SQL instructions.

# Commands
### Anyone

`!bang` - Play Russian roulette.

`!bangscores` - Shows high scores for attempts, deaths, survival rate, and death rate.

`!bet`, `!hit`, `!stand` - Play blackjack against the computer.

`!daily` - Shows when player's daily reward resets.

`!flip` - Flips a coin, displaying the result.

`!info [course ID]` - Shows relevant information about a specified course at UoGuelph.

`!join [role name]` - If the role already exists, it is assigned to the user. Otherwise, the user's id and requested role are stored in a database. If enough users apply for the same role, the role and a private text channel are created and all applicants are assigned to it.

`!help` - Displays other commands that can be used.

`!karma` - Shows user's upvotes, downvotes, and total karma

`!leave [role name]` - Unassigns the role from user and removes their application for that role from the database.

`!market` and `!buy <item #>` - View and purchase items listed on the market

`!mybang` - Shows player's bang scores.

`!ping` - Returns "pong!" and your latency in ms. Keeps track of user's best/worst scores.

`!roles` - Shows a list of available roles to join.
