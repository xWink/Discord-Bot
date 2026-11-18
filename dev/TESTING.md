# Testing the economy on Discord

Everything here runs on your machine against a throwaway database. Nothing touches production.

## 1. Automated checks (no Discord needed)

```sh
./dev/start-db.sh            # MariaDB in Docker on 127.0.0.1:33306 (needs Docker Desktop running)
sh gradlew test \
  -Ddb.url="jdbc:mariadb://127.0.0.1:33306/discord_bot" -Ddb.user=root -Ddb.pass=dev
```

Without `-Ddb.url` the database tests are skipped and only the pure unit tests run. Add `-Dlive.net=true`
to also hit the real Open Trivia DB once.

## 2. A private Discord test server

1. https://discord.com/developers/applications → New Application → Bot → copy the token.
2. On the Bot page enable **Server Members Intent**, **Presence Intent** and **Message Content Intent** (the bot requests all
   three). Without Message Content the bot cannot read `!` commands: it connects and shows online but never replies.
3. OAuth2 → URL Generator: scope `bot`, permissions Manage Roles, Send Messages, Embed Links, Add Reactions,
   Read Message History, Attach Files. Invite it to a **new empty server you own**.
4. In that server create text channels for bots and spam, and roles named exactly
   `orange`, `blue`, `green`, `purple`, `pink` (the market looks them up by name) and, optionally,
   `Season Champion`. Move the bot's own role **above** those colour roles, or role grants will fail.
5. Turn on Developer Mode (Discord settings → Advanced), then right-click to copy IDs.
6. Copy `.rolebotconfig.example` to `.rolebotconfig` and fill it in: your token, the server ID (`GUILD`), and the
   channel and role IDs (`ID_...` lines). You no longer need to edit any source file. Any ID you leave out keeps the
   live server's value, so that feature will not work on your test server.
7. Make sure the database is running (`./dev/start-db.sh`).
8. `./dev/run.sh` (checks your config, starts the database if needed, then runs the bot) and wait for "Applied migration ..." lines, then message the bot in your spam/bots channel.

Slash commands are registered to your test server (instantly) each time the bot starts, so no extra setup is
needed. If they do not appear, re-invite the bot with the `applications.commands` scope.

Optional JVM flags (add with `JAVA_OPTS` or `applicationDefaultJvmArgs`): `-Dtrivia.noncommercial=true` enables the
second trivia provider (CC BY-NC data), `-Dpuzzles.repo=owner/name` syncs puzzle packs from GitHub.

## 3. What to try, in order

Use two accounts (your own and an alt, or a friend) for the social flows.

| Area | Try | Expect |
| --- | --- | --- |
| Wallet | `!wallet` | Spendable, reserved, decay note, receipts |
| Gift | `!gift @alt 3` then `!gift @alt 999` | Moves coins; second says you cannot afford it |
| Gift wrap | `!gift @alt 2 wrap happy birthday` | Alt gets 2, you pay 5, card embed |
| Daily | `!bang` then `!daily` | 5 GC once; second claim shows the next time |
| Blackjack | `!bet 5`, then `!wallet` mid-hand | 5 GC shows as reserved. `!hit`/`!stand` settle once |
| Blackjack stakes | `!bet 3` with a 5 GC balance; `!paidplay off` then `!bet 1` | A stake over half your balance shows a warning but still plays (no cap). With paid play off it is refused |
| Blackjack restart | Start a hand, stop the bot, start it again, `!myhand` | Hand is still there; leave it 2 min and it auto-stands |
| Slots, flip | `!slots 3`, `!flip`, `!flip heads 2` | A GIF plays (reels stop one by one, coin spins and lands), then the text and an "again" button replace "Spinning…". Stakes settle once. `!slots` pays about 95% back |
| Roulette, crash, scratch | `!roulette red 2`, `!roulette 17 1`, `!crash 2 1.5`, `!scratch 2` | Each plays a GIF (wheel, climbing curve, scratching cells), then the text and an "again" button replace the placeholder. `!roulette purple 1` shows the usage |
| Mines | `!mines 5 3`, press tiles, then **Cash out**; also `!mine 5`, `!minecash`; leave one idle 5 minutes | A 4x4 button grid. Gems disable their tile and raise the payout; a mine ends the round with a board picture showing every mine. Idle rounds cash out at their standing. A restart releases open stakes |
| Coin duel | `!coinduel @alt 4`, alt presses **Accept** (or `!coinaccept`); try **Decline**; try waiting a minute | Both stakes are held, a coin GIF decides, the winner gains 4 GC and the loser loses 4. Declined, expired or unaffordable duels charge nothing |
| Trivia race | `!race 3`, then alt presses an answer button | First correct answer from someone other than the host wins the 3 GC. A wrong answer knocks that player out. After 45 seconds with no winner the host is refunded and the answer is posted |
| Wordle | `!wordle`, `!wordle crane`, keep guessing, solve it | A board GIF flips the newest row. Solving pays 3 GC once and shows a streak; six misses reveal the word. The same word for everyone each day |
| Rate limit | Run `!slots 1` twice within three seconds | The second says to wait a few seconds and plays nothing. This applies across all animated games and buttons, per player |
| Busy bot | Have several people play slots, roulette and bang at once | Each shows "Spinning…" straight away and the GIF follows; the bot keeps answering other commands while GIFs draw. If the drawing queue is full or a GIF fails, the result arrives as plain text and the stake is still settled |
| Course info | `!info cis3750` (animated banner), `!prereq cis3750` (the tree; after `!completed add ...` its boxes turn green for finished, blue for ready, amber for ready but part unchecked, grey for not yet, with a key and "Next up" under it), `!info cis6020` (graduate), `!info acct3230` (equivalent course), `!info cis1300`, `!info CIS*1300`, `!info intro programming`, `!info cis`, `!info zzzz`; `/info` and start typing | A coloured entry (offered terms, hours, credits, about, prerequisites, restrictions) with a button per prerequisite, a **Join** button and a department button. Words give up to five suggestions as buttons, a department lists its courses, and nothing found says how to search. `/info` autocompletes courses as you type. Long descriptions (over 1000 characters) are cut with an ellipsis instead of failing |
| Course planner | `!whynot 4820` (a number several departments share: it asks which, CIS first, instead of guessing), `!completed add cis1300 math1200`, `!completed list`, `!next`, `!next cis 3`, `!whynot cis3750`, `!completed remove cis1300`, `!completed clear`; also the **What can I take next?** and **Can I take it?** buttons | The list is saved per person. `!next` shows courses whose prerequisites you meet (and "maybe" ones with a part that cannot be checked, such as credit counts). `!whynot` names what is missing, with buttons for the missing courses. Restrictions are shown but never checked |
| Reminders | `!remind 2m test`, `!remind here 1m hello`, `!remind tomorrow 5pm lab`, `!remind friday study`, `!reminders`, **Cancel** button, `!remind cancel 3`; try `@everyone` in the text | A confirmation with a Discord timestamp and a Cancel button. At the time the bot DMs the text (or posts in the channel for `here`; if DMs are closed it posts in the channel). It never pings anyone but you. After a restart late reminders say they are a little late. At most 25 can wait |
| Academic dates | `!dates`, `!dates 15`, `!dates drop`, `!dates exam`, press a 🔔 button | The next dates from the calendar (veterinary dates hidden unless you add `dvm`) with days to go. A bell sets a reminder for 9:00 the day before. Refresh the dates with `python3 res/scrape.py --dates-only` |
| Course ratings | `!rate cis1300 3 3` before `!completed add cis1300` (refused, with an **I finished it** button), then `!rate cis1300 3 4 fair marking`, `!rate cis 1300 2 2`, `!reviews cis1300`, press **Report #n** (three people), `!rate remove cis1300`; as a mod `!review hide 5` (hides the comment, scores still count) and `!review remove 5` (also takes the scores out of the averages, even if they rate again); `!info cis1300` | Ratings are anonymous and one per person per course (rating again replaces it). `!reviews` shows an animated bar chart of difficulty and workload with the latest comments; the course entry shows the averages once three people have rated. Three different reports hide a comment, a mod can hide one at once, and the scores still count. Comments cannot ping anyone |
| Reduce motion | `!motion`, `!motion off`, then `!flip`, `!slots 1`, `!info cis1300`, `!prereq cis3750`, `!profile`; `!motion on` | With motion off every game, lookup and the profile card answer in plain text or a still image, never a GIF; with it on the animations are back. The setting is per person and survives a restart |
| Bot status (mods) | `!botstatus` as a mod, then as a member | Uptime, gateway ping, memory, commands run (top five), animations drawn (average and slowest time, how many are drawing or waiting, failed, dropped when busy, skipped for people with motion off), database connections and the last five errors. Members get no reply |
| Taking and classmates | `!taking add cis2750 math1200`, `!taking list`, `!taking done cis2750` (or **Finished** button), `!taking done all`, `!taking remove`, `!taking share on`, `!classmates cis2750` as another person | Current courses are separate from finished ones; done moves a course to the finished list. Nobody is shown in `!classmates` until they say `!taking share on`; others are counted but not named. At most 12 current courses |
| Grade tracker | `!grade cis1300 lab1 10 85`, `!grade cis1300 final 60` (no mark yet), `!grade cis1300 final 60 78`, `!grades`, `!grades cis1300`, `!need cis1300 80`, `!grade remove cis1300 lab1`, `!grade clear cis1300` | Everything shows by DM only; the channel just says it sent it, and never shows a mark. Items without a mark count as still to come. `!need` says the average needed on the rest, or that it is secured or out of reach. If DMs are closed it says so and shows nothing |
| Due dates | `!due add cis2500 lab3 friday 5pm`, `!due add cis2500 "final project" 2026-12-20 17:00`, `!due`, `!due cis2500`, **Done** button, `!due done 3`, `!due remove 3` | Items sorted soonest first with overdue marked. Adding sets a reminder the day before (an hour before if that is too late; none if it is under an hour away). Done or remove also cancels that reminder. At most 60 open |
| Morning digest | `!digest on 7:30`, `!digest`, `!digest now`, `!digest off`; wait for the time | A DM at the chosen time (Guelph time), once a day, only on days with something due within a week, reminders in the next day or calendar dates in the next three days. After downtime a digest more than three hours late is skipped |
| Degree roadmap | `!plan cis3750`, `!plan cis3750 3`, `!plan 4820`, with and without `!completed` / `!taking` lists, and with `!motion off` | An animated route, a column per term (earliest left), arrows from what is needed to what needs it, gap terms marked "nothing fits", blue for what you can take first and gold for the target; the same plan in text with the earliest finish and notes for anything not checked (credit counts). Courses go only in terms the catalogue says they run; a ? marks one it does not say. Courses you are taking are assumed finished this term |
| Dice, hilo | `!dice high`, `!dice seven 2`, `!hilo 2`, `!higher`, `!cashout` | Dice tumble as a GIF; HiLo shows the card as a picture. Free play works; stakes reserved/settled |
| Arcade | `!arcade`, `!answer b`, again, `!arcadescores` | First round pays daily; repeats are free |
| Weekly, gauntlet | `!weekly code`, `!gauntlet`, `!gauntlet prize` | 3 questions; prize run holds 3 GC then spends it |
| Bang card | `!bang` a few times in the spam channel | An animation (cylinder spins, hammer waits, shot, click or jam) that settles on a picture with the cylinder (spent chambers, the pulled one, unknown ones), your mascot, your streak and a **Pull again** button. Greyed mascot on a death, clovers on a jam |
| Duel | `!duel @alt 5`, alt `!accept`, both `!duelanswer a` | Winner +4 GC (1 GC fee); `!duelboard` needs 5 matches |
| Shop | `!shop`, `!shop buy flair-day`, `!shop owned`, `!profile` | Cosmetics appear on the profile |
| Featured price | `!shop buy bundle-weekend` twice | First quotes, second confirms |
| Colours | `!market`, `!buy 1`, `!swap blue` | Role granted before the success message |
| Goal | `!economy goal new 20 Trivia night` (admin), `!goal`, `!contribute 5`, `!contribute 5 confirm` | Progress bar moves |
| Bounty | `!bounty post 5 test my feature`, alt `!bounty claim 1`, you `!bounty accept 1` | Reward moves to the alt |
| Event | `!event start night` (admin), `!event confirm @alt`, `!event end` | Alt gets 5 GC (and the weekly bonus) |
| Boards | `!board arcade`, `!bangweek`, `!optout on` | Opted-out accounts disappear; `!bangweek` also shows best streaks |
| Achievements | `!achievements` after your first correct arcade answer | One item ticked; `!board collection` counts it |
| Profile card | `!profile` before and after `!equip` | A picture card on a neutral slate background; titles, badges, banners and effects change how it looks. A "WEARING" line names what the mascot has on, and survival never clips (try 100.0%) |
| Emoji art | `sh gradlew exportArt`, then upload `art-out/*_128.png` as server emoji | 14 mascot variants in the server colours |
| Alumni | `!alumni grant @alt thanks for everything` (mod), then `!profile` as the alt, `!alumni list` | Gold ribbon, cap and diploma on the card, 🎓 next to their name everywhere |
| Equip | `!shop buy title-week`, `!equip title-week`, `!profile`, `!arcadescores` | Title after your name, badge under "Equipped" |
| Victory effect | `!shop buy victory-fx`, `!equip victory-fx`, then a correct `!answer` | Confetti after the result |
| Team banner | `!banner request @SomeRole`, a member of that role says `!banner approve 1` | 45 GC held until approved, refunded if declined |
| Personal trivia | `!ptrivia` (lists categories), `!ptrivia geography 5`, friends `!panswer b` | Host pays 5 GC; friends join free and get normal arcade credit |
| Slash commands | Type `/` in the server, then `/wallet`, `/bet amount:2`, `/gift who:@alt amount:3 note:hi`, `/shop buy code:flair-day` | Same result as the `!` version; the slash reply stays and says `Ran !...`, and the bot answers in the channel |
| Buttons | `!bet 5` then press **Hit** / **Stand**; `!hilo 2` then **Higher**; `!arcade` then **A-D**; `!duel @alt 2` as alt press **Accept** | Same as typing the command. Someone else pressing your buttons gets a private "not for you" |
| Confirm buttons | `!contribute 5`, `!shop buy bundle-weekend` | A Confirm button replaces typing `confirm` / repeating the command |
| Animated profile | `!profile gif on`, then `!profile`; press **Stop animation**; `/profile animation:off` | A looping GIF (mascot bobs, stars twinkle) while on, the normal picture when off. The setting survives a restart |
| Private wallet | `!wallet` in a channel | The embed arrives by DM (📬 reaction); if DMs are closed it posts in the channel |

Give yourself coins with the admin command: `!economy adjust @you +100 testing`. Look up any receipt with
`!economy receipt <id>`.

Decay: `!economy decay 1` publishes a first cutoff one week out (preview). To see a charge sooner, shift the
setting in the database:

```sh
docker exec gryph-dev-db mariadb -uroot -pdev discord_bot -e \
  "UPDATE economy_settings SET value = value - 1 WHERE name = 'decay_first_week'"
```

then run `!wallet` with a balance of 150 or more. Decay is 2% of the part above 100 GC, rounded down to whole coins, so 149 GC or less pays nothing.

## 4. Failure drills

- **Grant failure:** drag the bot's role below the colour roles and `!buy 1`: you should be refunded.
- **Kill mid-game:** stop the bot during a blackjack hand, a duel, or `!gauntlet prize`; after restart the stakes
  are restored or released (check `!wallet` reserved).
- **Reset everything:** `./dev/start-db.sh` recreates the database from scratch.
