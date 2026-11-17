package integration;

import command.Command;
import command.commands.Help;
import command.commands.economy.*;
import command.commands.blackjack.*;
import command.commands.misc.Profile;
import command.commands.bang.Daily;
import command.commands.bang.BangWeek;
import net.dv8tion.jda.api.entities.User;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.io.PrintWriter;

import static org.junit.Assert.*;

/**
 * Runs the real command classes against a local database with fake Discord
 * messages and checks what the bot replies. Skipped without -Ddb.url.
 */
public class CommandSmokeTest extends DbTestBase {

    private static final long BOTS = 111L;
    private static File config;

    @BeforeClass
    public static void writeConfig() throws Exception {
        org.junit.Assume.assumeTrue(System.getProperty("db.url") != null);
        config = new File(".rolebotconfig");
        if (config.exists()) {
            // Use the developer's own config, but never let tests touch a non-local database.
            config = null;
            boolean local = false;
            for (String line : java.nio.file.Files.readAllLines(new File(".rolebotconfig").toPath())) {
                if (line.startsWith("DB_ADDR=")) {
                    String addr = line.substring("DB_ADDR=".length()).trim();
                    local = addr.startsWith("127.0.0.1") || addr.startsWith("localhost");
                }
            }
            org.junit.Assume.assumeTrue("DB_ADDR in .rolebotconfig is not local; skipping", local);
            return;
        }
        try (PrintWriter w = new PrintWriter(config)) {
            w.println("TOKEN=offline");
            w.println("GUILD=1");
            w.println("CHANNEL=all");
            w.println("DB_ADDR=127.0.0.1:33306");
            w.println("DB_USER=root");
            w.println("DB_PASS=dev");
        }
    }

    @AfterClass
    public static void removeConfig() {
        if (config != null) config.delete();
    }

    private String run(Command c, long user, String content, User... mentions) {
        FakeDiscord f = FakeDiscord.say(user, "Tester" + user % 1000, content, main.Server.BOTS_CHANNEL_ID, mentions);
        assertTrue(content + " does not match " + c.getKey(), c.keyMatches(content));
        c.start(f.event);
        String said = f.said();
        System.out.println(">>> " + content + "\n" + said);
        assertFalse(content + " raised an error: " + said, said.contains("An error occurred"));
        assertTrue(content + " said nothing", !said.trim().isEmpty() || content.startsWith("!duelanswer"));
        return said;
    }

    @Test
    public void walletComesByDirectMessage() throws Exception {
        long u = newUser();
        FakeDiscord f = FakeDiscord.say(u, "Ann", "!wallet", main.Server.BOTS_CHANNEL_ID);
        new Wallet().start(f.event);
        assertTrue(f.said(), f.said().contains("Spendable"));
        assertFalse(f.directMessages.isEmpty());
    }

    @Test
    public void economyCommandsRespond() throws Exception {
        long a = newUser(), b = newUser();
        User ub = FakeDiscord.user(b, "Bea");
        economy.EconomyService.getInstance().credit(a, 95, "t", null, "c" + a);
        run(new Daily(), a, "!daily");
        assertTrue(run(new Gift(), a, "!gift <@" + b + "> 3", ub).contains("Sent 3 gc"));
        assertTrue(run(new Gift(), a, "!gift <@" + b + "> 2 wrap hello there", ub).contains("Sent 2 gc"));
        assertEquals(5 + 3 + 2, economy.EconomyService.getInstance().getBalance(b));
        run(new PaidPlay(), a, "!paidplay");
        run(new GoalCommand(), a, "!goal");
        run(new BountyCommand(), a, "!bounty");
        run(new BountyCommand(), a, "!bounty post 5 test my thing");
        run(new AchievementsCommand(), a, "!achievements");
        run(new ShopCommand(), a, "!shop");
        run(new ShopCommand(), a, "!shop owned");
        run(new BoardCommand.Board(), a, "!board arcade");
        run(new BoardCommand.Board(), a, "!board collection");
        run(new BoardCommand.OptOut(), a, "!optout");
        run(new Help(), a, "!help");
        run(new Help(), a, "!help games");
        run(new BangWeek(), a, "!bangweek");
    }

    @Test
    public void gamesRespondAndSettle() throws Exception {
        long u = newUser();
        economy.EconomyService.getInstance().credit(u, 95, "t", null, "c" + u);
        run(new Dice(), u, "!dice high");
        run(new Dice(), u, "!dice seven 2");
        run(new Dice(), u, "!dice");
        run(new HiLo(), u, "!hilo 2");
        String r = run(new HiLo.Move("!higher"), u, "!higher");
        if (r.contains("Correct")) run(new HiLo.Move("!cashout"), u, "!cashout");
        run(new Bet(), u, "!bet 3");
        if (command.util.game.BlackJackList.getUserGame(u) != null) {
            run(new MyHand(), u, "!myhand");
            run(new Stand(), u, "!stand");
        }
        assertEquals("no stakes left held", 0, economy.EconomyService.getInstance().getReserved(u)
                - (open(u) ? 3 : 0));
    }

    private boolean open(long u) {
        return command.util.game.BlackJackList.getUserGame(u) != null;
    }

    @Test
    public void arcadeAndQuizzesRespond() throws Exception {
        arcade.QuestionBank.loadBundled();
        long u = newUser();
        run(new Arcade(), u, "!arcade");
        run(new Answer(), u, "!answer a");
        run(new ArcadeScores(), u, "!arcadescores");
        run(new QuizCommands.Weekly(), u, "!weekly");
        run(new QuizCommands.Weekly(), u, "!weekly code");
        for (int i = 0; i < 3; i++) run(new Answer(), u, "!answer a");
        run(new QuizCommands.Gauntlet(), u, "!gauntlet");
        for (int i = 0; i < 3; i++) run(new Answer(), u, "!answer a");
        run(new QuizCommands.Coop(), u, "!coop");
        run(new PersonalTriviaCommands.Start(), u, "!ptrivia");
    }

    @Test
    public void victoryEffectShowsAfterEquipping() throws Exception {
        arcade.QuestionBank.loadBundled();
        long u = newUser();
        economy.EconomyService.getInstance().credit(u, 50, "t", null, "c" + u);
        // buy (featured items need a second confirming call) and wear it
        String bought = run(new ShopCommand(), u, "!shop buy victory-fx");
        if (!bought.contains("Bought")) bought = run(new ShopCommand(), u, "!shop buy victory-fx");
        assertTrue(bought, bought.contains("Bought"));
        assertTrue(run(new EquipCommand(), u, "!equip victory-fx").contains("Equipped"));
        assertFalse(economy.Cosmetics.extras(u).effect.isEmpty());

        arcade.ArcadeService.Round round = arcade.ArcadeService.start(u);
        char right = (char) ('a' + round.question.getCorrectIndex());
        String said = run(new Answer(), u, "!answer " + right);
        assertTrue(said, said.contains("Victory!"));
    }

    @Test
    public void duelFlowRuns() throws Exception {
        arcade.QuestionBank.loadBundled();
        long a = newUser(), b = newUser();
        User ub = FakeDiscord.user(b, "Bea");
        run(new DuelCommands.Challenge(), a, "!duel <@" + b + "> 0", ub);
        run(new DuelCommands.Accept(), b, "!accept");
        run(new DuelCommands.DuelAnswer(), a, "!duelanswer a");
        run(new DuelCommands.DuelAnswer(), b, "!duelanswer b");
        run(new DuelCommands.Board(), a, "!duelboard");
    }

    @Test
    public void eachEffectGivesItsOwnWinCard() throws Exception {
        arcade.QuestionBank.loadBundled();
        long u = newUser();
        economy.EconomyService.getInstance().credit(u, 60, "t", null, "c" + u);
        java.util.Set<Integer> seen = new java.util.HashSet<>();
        String[][] effects = {{"victory-fx", "Victory!", "\uD83C\uDF89"}, {"fx-fireworks", "Fireworks!", "\uD83C\uDF86"},
                {"fx-trophy", "Champion!", "\uD83C\uDFC6"}, {"fx-stars", "Starfall!", "\u2B50"}};
        for (String[] fx : effects) {
            economy.CosmeticShop.getInstance().buy(u, fx[0], "fx:" + fx[0] + u);
            assertEquals("effect", economy.Cosmetics.equip(u, fx[0]));
            arcade.ArcadeService.Round round = arcade.ArcadeService.start(u);
            char right = (char) ('a' + round.question.getCorrectIndex());
            FakeDiscord f = FakeDiscord.say(u, "Ann", "!answer " + right, main.Server.BOTS_CHANNEL_ID);
            new Answer().start(f.event);
            assertEquals("a styled card, not plain text: " + f.said(), 1, f.embeds.size());
            assertTrue(f.said(), f.said().contains(fx[1]));
            assertEquals("the mascot picture is attached", 1, f.files.size());
            java.awt.image.BufferedImage art = javax.imageio.ImageIO.read(new java.io.ByteArrayInputStream(f.files.get(0)));
            assertEquals(256, art.getWidth());
            assertTrue("each effect has its own art", seen.add(java.util.Arrays.hashCode(f.files.get(0))));
        }
    }

    @Test
    public void wrongAnswersAndUnequippedPlayersGetPlainText() throws Exception {
        arcade.QuestionBank.loadBundled();
        long u = newUser();
        arcade.ArcadeService.Round round = arcade.ArcadeService.start(u);
        char right = (char) ('a' + round.question.getCorrectIndex());
        FakeDiscord f = FakeDiscord.say(u, "Ann", "!answer " + right, main.Server.BOTS_CHANNEL_ID);
        new Answer().start(f.event);
        assertTrue(f.embeds.isEmpty());
        assertFalse(f.texts.isEmpty());
    }

    @Test
    public void titleAndMarksAppearNextToNames() throws Exception {
        long u = newUser();
        economy.EconomyService.getInstance().credit(u, 60, "t", null, "c" + u);
        economy.CosmeticShop.getInstance().buy(u, "title-week", "t1" + u);
        economy.CosmeticShop.getInstance().buy(u, "flair-day", "t2" + u);
        economy.Cosmetics.equip(u, "title-week");
        economy.Cosmetics.equip(u, "flair-day");
        assertTrue(economy.Cosmetics.tag(u).contains("the Scholar"));
        assertTrue(economy.Cosmetics.nameWithTitle(u, "Ann").startsWith("Ann the Scholar"));
        assertTrue(economy.Cosmetics.nameWithTitle(u, "Ann").contains("\u2728"));
    }

    @Test
    public void profileSendsACardPictureThatChangesWithCosmetics() throws Exception {
        long u = newUser();
        economy.EconomyService.getInstance().credit(u, 60, "t", null, "c" + u);
        FakeDiscord plain = FakeDiscord.say(u, "Ann", "!profile", main.Server.BOTS_CHANNEL_ID);
        new Profile().start(plain.event);
        assertEquals(plain.said(), 1, plain.files.size());
        java.awt.image.BufferedImage img = javax.imageio.ImageIO.read(new java.io.ByteArrayInputStream(plain.files.get(0)));
        assertEquals(command.util.misc.ProfileCard.WIDTH, img.getWidth());

        economy.CosmeticShop.getInstance().buy(u, "title-week", "pc1" + u);
        economy.CosmeticShop.getInstance().buy(u, "fx-fireworks", "pc2" + u);
        economy.Cosmetics.equip(u, "title-week");
        economy.Cosmetics.equip(u, "fx-fireworks");
        FakeDiscord dressed = FakeDiscord.say(u, "Ann", "!profile", main.Server.BOTS_CHANNEL_ID);
        new Profile().start(dressed.event);
        assertEquals(1, dressed.files.size());
        assertFalse("wearing items must change the picture", java.util.Arrays.equals(plain.files.get(0), dressed.files.get(0)));
    }

    @Test
    public void onlyModsCanBestowAlumniAndItShowsEverywhere() throws Exception {
        long mod = newUser(), member = newUser(), nobody = newUser();
        User um = FakeDiscord.user(member, "Bea");

        // a normal member cannot grant
        FakeDiscord denied = FakeDiscord.say(nobody, "Cy", "!alumni grant <@" + member + "> thanks", main.Server.BOTS_CHANNEL_ID, um);
        new AlumniCommand().start(denied.event);
        assertTrue(denied.said(), denied.said().contains("Only mods"));
        assertNull(economy.AlumniService.getInstance().get(member));

        // a mod can
        FakeDiscord ok = FakeDiscord.sayAsOwner(mod, "Mod", "!alumni grant <@" + member + "> founding member", main.Server.BOTS_CHANNEL_ID, um);
        new AlumniCommand().start(ok.event);
        assertTrue(ok.said(), ok.said().contains("alumnus"));
        assertEquals("the sticker is attached", 1, ok.files.size());
        economy.AlumniService.Record r = economy.AlumniService.getInstance().get(member);
        assertEquals("founding member", r.note);

        // granting again updates, not duplicates
        FakeDiscord again = FakeDiscord.sayAsOwner(mod, "Mod", "!alumni grant <@" + member + "> still here", main.Server.BOTS_CHANNEL_ID, um);
        new AlumniCommand().start(again.event);
        assertTrue(again.said(), again.said().contains("is alumni"));

        // visible: no equipping needed
        assertTrue(economy.Cosmetics.tag(member).contains("\uD83C\uDF93"));
        assertTrue(economy.Cosmetics.nameWithTitle(member, "Bea").contains("\uD83C\uDF93"));
        assertTrue(economy.Cosmetics.loadout(member).alumni);
        assertEquals("no year was given, so none is shown", 0, economy.Cosmetics.loadout(member).alumniYear);
        assertEquals("", r.classLabel());
        assertTrue(economy.ProfileExtras.pills(member).get(0).startsWith("Alumni"));
        FakeDiscord self = FakeDiscord.say(member, "Bea", "!alumni", main.Server.BOTS_CHANNEL_ID);
        new AlumniCommand().start(self.event);
        assertTrue(self.said(), self.said().contains("You are alumni") && !self.said().contains("class of"));

        FakeDiscord list = FakeDiscord.say(nobody, "Cy", "!alumni list", main.Server.BOTS_CHANNEL_ID);
        new AlumniCommand().start(list.event);
        assertTrue(list.said(), list.said().contains("<@" + member + ">"));

        // the profile picture changes
        FakeDiscord before = FakeDiscord.say(nobody, "Cy", "!profile", main.Server.BOTS_CHANNEL_ID);
        new Profile().start(before.event);
        FakeDiscord after = FakeDiscord.say(member, "Bea", "!profile", main.Server.BOTS_CHANNEL_ID);
        new Profile().start(after.event);
        assertFalse(java.util.Arrays.equals(before.files.get(0), after.files.get(0)));

        // a year is optional but can be given, and re-granting without one keeps it
        FakeDiscord withYear = FakeDiscord.sayAsOwner(mod, "Mod", "!alumni grant <@" + member + "> 2019 captain of the chess club",
                main.Server.BOTS_CHANNEL_ID, um);
        new AlumniCommand().start(withYear.event);
        assertTrue(withYear.said(), withYear.said().contains("class of 2019"));
        economy.AlumniService.Record dated = economy.AlumniService.getInstance().get(member);
        assertEquals(2019, dated.classYear());
        assertEquals("captain of the chess club", dated.note);
        assertEquals(2019, economy.Cosmetics.loadout(member).alumniYear);
        assertTrue(economy.ProfileExtras.pills(member).get(0).contains("Class of 2019"));
        FakeDiscord noYear = FakeDiscord.sayAsOwner(mod, "Mod", "!alumni grant <@" + member + "> updated note", main.Server.BOTS_CHANNEL_ID, um);
        new AlumniCommand().start(noYear.event);
        assertEquals(2019, economy.AlumniService.getInstance().get(member).classYear());
        FakeDiscord notAYear = FakeDiscord.sayAsOwner(mod, "Mod", "!alumni grant <@" + member + "> 12345 a long number", main.Server.BOTS_CHANNEL_ID, um);
        new AlumniCommand().start(notAYear.event);
        assertEquals("a non-year number is part of the note", "12345 a long number",
                economy.AlumniService.getInstance().get(member).note);

        // revoking removes it
        FakeDiscord gone = FakeDiscord.sayAsOwner(mod, "Mod", "!alumni revoke <@" + member + ">", main.Server.BOTS_CHANNEL_ID, um);
        new AlumniCommand().start(gone.event);
        assertNull(economy.AlumniService.getInstance().get(member));
        assertFalse(economy.Cosmetics.tag(member).contains("\uD83C\uDF93"));
    }
}
