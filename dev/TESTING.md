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
| Blackjack limits | `!bet 11`, `!paidplay off` then `!bet 1` | Refused with a reason |
| Blackjack restart | Start a hand, stop the bot, start it again, `!myhand` | Hand is still there; leave it 2 min and it auto-stands |
| Dice, hilo | `!dice high`, `!dice seven 2`, `!hilo 2`, `!higher`, `!cashout` | Free play works; stakes reserved/settled |
| Arcade | `!arcade`, `!answer b`, again, `!arcadescores` | First round pays daily; repeats are free |
| Weekly, gauntlet | `!weekly code`, `!gauntlet`, `!gauntlet prize` | 3 questions; prize run holds 3 GC then spends it |
| Duel | `!duel @alt 5`, alt `!accept`, both `!duelanswer a` | Winner +4 GC (1 GC fee); `!duelboard` needs 5 matches |
| Shop | `!shop`, `!shop buy flair-day`, `!shop owned`, `!profile` | Cosmetics appear on the profile |
| Featured price | `!shop buy bundle-weekend` twice | First quotes, second confirms |
| Colours | `!market`, `!buy 1`, `!swap blue` | Role granted before the success message |
| Goal | `!economy goal new 20 Trivia night` (admin), `!goal`, `!contribute 5`, `!contribute 5 confirm` | Progress bar moves |
| Bounty | `!bounty post 5 test my feature`, alt `!bounty claim 1`, you `!bounty accept 1` | Reward moves to the alt |
| Event | `!event start night` (admin), `!event confirm @alt`, `!event end` | Alt gets 5 GC (and the weekly bonus) |
| Boards | `!board arcade`, `!bangweek`, `!optout on` | Opted-out accounts disappear; `!bangweek` also shows best streaks |
| Achievements | `!achievements` after your first correct arcade answer | One item ticked; `!board collection` counts it |
| Equip | `!shop buy title-week`, `!equip title-week`, `!profile`, `!arcadescores` | Title after your name, badge under "Equipped" |
| Victory effect | `!shop buy victory-fx`, `!equip victory-fx`, then a correct `!answer` | Confetti after the result |
| Team banner | `!banner request @SomeRole`, a member of that role says `!banner approve 1` | 45 GC held until approved, refunded if declined |
| Personal trivia | `!ptrivia` (lists categories), `!ptrivia geography 5`, friends `!panswer b` | Host pays 5 GC; friends join free and get normal arcade credit |
| Private wallet | `!wallet` in a channel | The embed arrives by DM (📬 reaction); if DMs are closed it posts in the channel |

Give yourself coins with the admin command: `!economy adjust @you +100 testing`. Look up any receipt with
`!economy receipt <id>`.

Decay: `!economy decay 1` publishes a first cutoff one week out (preview). To see a charge sooner, shift the
setting in the database:

```sh
docker exec gryph-dev-db mariadb -uroot -pdev discord_bot -e \
  "UPDATE economy_settings SET value = value - 1 WHERE name = 'decay_first_week'"
```

then run `!wallet` with a balance over 100.

## 4. Failure drills

- **Grant failure:** drag the bot's role below the colour roles and `!buy 1`: you should be refunded.
- **Kill mid-game:** stop the bot during a blackjack hand, a duel, or `!gauntlet prize`; after restart the stakes
  are restored or released (check `!wallet` reserved).
- **Reset everything:** `./dev/start-db.sh` recreates the database from scratch.
