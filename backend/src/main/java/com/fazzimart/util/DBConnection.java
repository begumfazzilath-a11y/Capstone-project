package com.fazzimart.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;
import java.util.regex.Pattern;

/**
 * Central MySQL / JDBC helper (same approach as DhanyaMart's DBConnection).
 *
 * <p>Every DAO obtains its {@link Connection} from {@link #getConnection()}.
 * On the very first connection it also auto-runs the schema script
 * {@code /sql/fazzi_mart_schema.sql} (all statements are
 * {@code CREATE ... IF NOT EXISTS}, so it is safe to run repeatedly).</p>
 *
 * <p>Configuration is resolved with the following precedence
 * (highest first):</p>
 * <ol>
 *   <li>JVM system properties: {@code fazzimart.db.url / user / password}</li>
 *   <li>Environment variables: {@code DB_URL / DB_USER / DB_PASSWORD}
 *       (also {@code FAZZIMART_DB_URL} etc. as an alias)</li>
 *   <li>{@code db.properties} on the classpath (see src/main/resources/db.properties)</li>
 *   <li>Defaults: localhost:3306, database {@code fazzimart}, user {@code root},
 *       empty password</li>
 * </ol>
 *
 * <p>{@link #getConnection()} returns the shared thread-local connection while a
 * transaction is open (see {@link #beginTransaction()} / {@link #commitTransaction()}
 * / {@link #rollbackTransaction()}), so a {@code placeOrder} flow can run atomically
 * across several DAOs. The shared connection is wrapped so a DAO's own
 * try-with-resources {@code close()} is a no-op until the transaction ends.</p>
 */
public final class DBConnection {

    public static final String DEFAULT_URL =
            "jdbc:mysql://localhost:3306/fazzimart?createDatabaseIfNotExist=true"
                    + "&useSSL=false&allowPublicKeyRetrieval=true"
                    + "&serverTimezone=UTC&characterEncoding=UTF-8";

    private static final String SCHEMA_RESOURCE = "/sql/fazzi_mart_schema.sql";
    private static final Pattern COMMENT_LINE = Pattern.compile("^\\s*(--.*)?$");

    private static final String url;
    private static final String user;
    private static final String password;

    /** Guards the one-time schema initialisation. */
    private static boolean schemaReady = false;

    /** The (optional) connection of the currently open transaction on this thread. */
    private static final ThreadLocal<TxContext> TX = new ThreadLocal<>();

    private DBConnection() {
    }

    static {
        Properties fileProps = loadPropertiesFile();

        url = firstNotNull(
                System.getProperty("fazzimart.db.url"),
                System.getenv("DB_URL"),
                System.getenv("FAZZIMART_DB_URL"),
                fileProps.getProperty("db.url"),
                DEFAULT_URL);
        user = firstNotNull(
                System.getProperty("fazzimart.db.user"),
                System.getenv("DB_USER"),
                System.getenv("FAZZIMART_DB_USER"),
                fileProps.getProperty("db.user"),
                "root");
        password = firstNotNull(
                System.getProperty("fazzimart.db.password"),
                System.getenv("DB_PASSWORD"),
                System.getenv("FAZZIMART_DB_PASSWORD"),
                fileProps.getProperty("db.password"),
                "");

        // Explicitly load the driver (Connector/J also self-registers via SPI).
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException(
                    "MySQL JDBC driver (com.mysql.cj.jdbc.Driver) not found on classpath.", e);
        }
    }

    /**
     * Returns a JDBC connection to the fazzimart MySQL database.
     * On the first call the schema is created automatically (idempotent).
     * If a transaction is open on this thread the shared connection is
     * returned instead of opening a new one.
     *
     * @throws SQLException when the database cannot be reached. Check that
     *                      MySQL 8 is running and that db.properties matches
     *                      your MySQL username/password.
     */
    public static Connection getConnection() throws SQLException {
        TxContext tx = TX.get();
        if (tx != null) {
            return tx.connection; // shared, close() is a no-op while txn open
        }
        Connection conn = openConnection();
        initSchema(conn);
        return conn;
    }

    /**
     * Opens a new JDBC connection with auto-commit disabled and binds it to
     * this thread. Every {@link #getConnection()} call on this thread now
     * returns the same connection until the transaction is committed or
     * rolled back. Returns the (close-guarded) connection.
     */
    public static Connection beginTransaction() throws SQLException {
        Connection real = openConnection();
        initSchema(real);
        real.setAutoCommit(false);
        TxContext ctx = new TxContext(real);
        TX.set(ctx);
        return ctx.connection;
    }

    /** Commits the current thread's transaction (if any) and releases it. */
    public static void commitTransaction() throws SQLException {
        TxContext ctx = TX.get();
        if (ctx == null) {
            return;
        }
        TX.remove();
        try {
            ctx.real.commit();
        } finally {
            ctx.real.setAutoCommit(true);
            ctx.real.close();
        }
    }

    /** Rolls back the current thread's transaction (if any) and releases it. */
    public static void rollbackTransaction() throws SQLException {
        TxContext ctx = TX.get();
        if (ctx == null) {
            return;
        }
        TX.remove();
        try {
            ctx.real.rollback();
        } finally {
            ctx.real.setAutoCommit(true);
            ctx.real.close();
        }
    }

    /**
     * Creates the required tables on the very first connection.
     * Uses the same authoritative script the reviewer can run by hand.
     */
    private static void initSchema(Connection conn) {
        if (schemaReady) {
            return;
        }
        synchronized (DBConnection.class) {
            if (schemaReady) {
                return;
            }
            String script = readResource(SCHEMA_RESOURCE);
            try (Statement stmt = conn.createStatement()) {
                for (String statement : splitStatements(script)) {
                    stmt.execute(statement);
                }
                schemaReady = true;
            } catch (SQLException e) {
                throw new IllegalStateException(
                        "Could not initialise the fazzimart schema on MySQL: " + e.getMessage(), e);
            }
        }
    }

    private static Connection openConnection() throws SQLException {
        return DriverManager.getConnection(url, user, password);
    }

    /** Loads db.properties from the classpath (missing file -> empty Properties). */
    private static Properties loadPropertiesFile() {
        Properties props = new Properties();
        try (InputStream in = DBConnection.class.getResourceAsStream("/db.properties")) {
            if (in != null) {
                props.load(in);
            }
        } catch (IOException e) {
            // Ignore: defaults are used later.
        }
        return props;
    }

    /** Reads a classpath resource as UTF-8 text. */
    private static String readResource(String resource) {
        try (InputStream in = DBConnection.class.getResourceAsStream(resource)) {
            if (in == null) {
                throw new IllegalStateException("Schema resource not found: " + resource);
            }
            StringBuilder sb = new StringBuilder();
            try (BufferedReader reader =
                         new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line).append('\n');
                }
            }
            return sb.toString();
        } catch (IOException e) {
            throw new IllegalStateException("Could not read schema resource: " + resource, e);
        }
    }

    /**
     * Splits a SQL script into individual statements on ";" lines.
     * Strips "-- " comment lines first.
     */
    private static String[] splitStatements(String script) {
        StringBuilder clean = new StringBuilder();
        for (String line : script.split("\\r?\\n")) {
            if (!COMMENT_LINE.matcher(line).matches()) {
                clean.append(line).append('\n');
            }
        }
        String[] parts = clean.toString().split(";");
        java.util.List<String> statements = new java.util.ArrayList<>();
        for (String part : parts) {
            String trimmed = part.trim();
            if (!trimmed.isEmpty()) {
                statements.add(trimmed);
            }
        }
        return statements.toArray(new String[0]);
    }

    /** Returns the first non-null value. */
    private static String firstNotNull(String... values) {
        for (String value : values) {
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    /**
     * Holds the real JDBC connection plus a dynamic proxy that ignores
     * {@code close()} - so DAO try-with-resources blocks never kill the
     * shared transaction connection before commit/rollback.
     */
    private static final class TxContext {
        private final Connection real;
        private final Connection connection;

        TxContext(Connection real) {
            this.real = real;
            this.connection = (Connection) Proxy.newProxyInstance(
                    Connection.class.getClassLoader(),
                    new Class<?>[]{Connection.class},
                    (proxy, method, args) -> {
                        if ("close".equals(method.getName()) && method.getParameterCount() == 0) {
                            return null; // no-op until commit/rollback
                        }
                        return method.invoke(real, args);
                    });
        }
    }
}