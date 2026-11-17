package integration;

import database.Database;
import org.junit.Assume;
import org.junit.BeforeClass;

import java.sql.Statement;

/**
 * Base for tests that need a real MariaDB. They are skipped unless
 * {@code -Ddb.url=...} is passed (see dev/start-db.sh).
 */
public abstract class DbTestBase {

    @BeforeClass
    public static void setUpDatabase() throws Exception {
        Assume.assumeTrue("db.url not set", System.getProperty("db.url") != null);
        Database.runMigrations();
        Database.runMigrations(); // running twice must be a no-op
    }

    /** Fresh user ID per test so tests never share wallets. */
    protected static long newUser() {
        return 9_000_000_000L + (long) (Math.random() * 1_000_000_000L);
    }

    protected static void exec(String sql) throws Exception {
        Database.inTransaction(c -> {
            try (Statement st = c.createStatement()) {
                st.execute(sql);
            }
            return null;
        });
    }
}
