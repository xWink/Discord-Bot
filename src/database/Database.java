package database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import main.Config;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Owns the connection pool and runs versioned schema migrations.
 * Code that needs transactional guarantees borrows one connection per
 * transaction through {@link #inTransaction(TransactionBody)}.
 */
public final class Database {

    private static final String MIGRATIONS_DIR = "migrations/";

    private static HikariDataSource pool;

    private Database() { }

    /**
     * Work executed inside a single database transaction.
     *
     * @param <T> the type of value produced by the work
     */
    @FunctionalInterface
    public interface TransactionBody<T> {
        T run(Connection connection) throws SQLException;
    }

    /**
     * Lazily creates the pool.
     *
     * @return the shared pool
     */
    public static synchronized HikariDataSource getPool() {
        if (pool == null) {
            HikariConfig config = new HikariConfig();
            config.setJdbcUrl("jdbc:mysql://" + Config.getDbAddr() + "/discord_bot");
            config.setUsername(Config.getDbUser());
            config.setPassword(Config.getDbPass());
            config.setDriverClassName("org.mariadb.jdbc.Driver");
            config.setMaximumPoolSize(8);
            config.setAutoCommit(false);
            config.setTransactionIsolation("TRANSACTION_REPEATABLE_READ");
            pool = new HikariDataSource(config);
        }
        return pool;
    }

    /**
     * Runs the body in one transaction on its own pooled connection.
     * Commits on success and rolls back if the body throws.
     *
     * @param body the work to run
     * @param <T> the result type
     * @return whatever the body returns
     * @throws SQLException if the work or the commit fails
     */
    public static <T> T inTransaction(TransactionBody<T> body) throws SQLException {
        try (Connection connection = getPool().getConnection()) {
            try {
                T result = body.run(connection);
                connection.commit();
                return result;
            } catch (SQLException | RuntimeException e) {
                connection.rollback();
                throw e;
            }
        }
    }

    /**
     * Applies every migration listed in migrations/index.txt that has not
     * yet been recorded in the schema_migrations table. Aborts startup if a
     * migration fails so the bot never runs against a half-built schema.
     */
    public static void migrate() {
        try {
            inTransaction(connection -> {
                try (Statement st = connection.createStatement()) {
                    st.execute("CREATE TABLE IF NOT EXISTS schema_migrations ("
                            + "version VARCHAR(80) PRIMARY KEY, applied_at BIGINT NOT NULL)");
                }
                return null;
            });

            Set<String> applied = new HashSet<>();
            inTransaction(connection -> {
                try (Statement st = connection.createStatement();
                     ResultSet rs = st.executeQuery("SELECT version FROM schema_migrations")) {
                    while (rs.next()) applied.add(rs.getString(1));
                }
                return null;
            });

            for (String name : readLines(MIGRATIONS_DIR + "index.txt")) {
                if (applied.contains(name)) continue;
                List<String> statements = splitStatements(readResource(MIGRATIONS_DIR + name));
                // DDL implicitly commits in MariaDB, so record the version last.
                inTransaction(connection -> {
                    for (String sql : statements) {
                        try (Statement st = connection.createStatement()) {
                            st.execute(sql);
                        }
                    }
                    try (PreparedStatement ps = connection.prepareStatement(
                            "INSERT INTO schema_migrations (version, applied_at) VALUES (?, ?)")) {
                        ps.setString(1, name);
                        ps.setLong(2, System.currentTimeMillis());
                        ps.executeUpdate();
                    }
                    return null;
                });
                System.out.println("Applied migration " + name);
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Database migration failed. Shutting down...");
            System.exit(-1);
        }
    }

    static List<String> splitStatements(String script) {
        List<String> statements = new ArrayList<>();
        for (String part : script.split(";\\s*(\\r?\\n|$)")) {
            String sql = part.trim();
            if (!sql.isEmpty()) statements.add(sql);
        }
        return statements;
    }

    private static List<String> readLines(String resource) throws IOException {
        List<String> lines = new ArrayList<>();
        for (String line : readResource(resource).split("\\r?\\n")) {
            if (!line.trim().isEmpty()) lines.add(line.trim());
        }
        return lines;
    }

    private static String readResource(String resource) throws IOException {
        try (InputStream in = Database.class.getClassLoader().getResourceAsStream(resource)) {
            if (in == null) throw new IOException("Missing resource " + resource);
            StringBuilder sb = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) sb.append(line).append('\n');
            }
            return sb.toString();
        }
    }
}
