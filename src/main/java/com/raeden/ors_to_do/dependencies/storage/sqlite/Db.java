package com.raeden.ors_to_do.dependencies.storage.sqlite;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Owns the single long-lived {@link Connection} the storage layer uses.
 *
 * <p>The storage redesign calls for "one long-lived connection for the app session rather than
 * per-write". That's exactly what this class manages, applying the WAL / NORMAL / foreign_keys
 * pragmas the doc prescribes for crash-safety + writer-doesn't-block-readers semantics.</p>
 *
 * <p>JDBC connections are not thread-safe, so storage callers should synchronize on this
 * instance's {@link #lock()} whenever they touch the connection. The desktop UI's write rate is
 * low enough that a single guard is far simpler than a pool.</p>
 */
public final class Db implements AutoCloseable {

    private final String url;
    private Connection connection;
    private final Object lock = new Object();

    public Db(String jdbcUrl) {
        this.url = jdbcUrl;
    }

    /** Opens the connection and applies durability pragmas. Safe to call more than once. */
    public synchronized void open() throws SQLException {
        if (connection != null && !connection.isClosed()) return;
        connection = DriverManager.getConnection(url);
        try (Statement st = connection.createStatement()) {
            // WAL: crash-safe, readers don't block the writer.
            st.execute("PRAGMA journal_mode = WAL");
            // NORMAL: good balance — sync at commit, not on every page write.
            st.execute("PRAGMA synchronous = NORMAL");
            // Defensive: foreign_keys default OFF in SQLite. We don't use them yet, but enabling
            // this catches future schema mistakes early.
            st.execute("PRAGMA foreign_keys = ON");
        }
    }

    /** Returns the live connection. Throws if {@link #open()} hasn't been called. */
    public Connection connection() {
        if (connection == null) throw new IllegalStateException("Db.open() must be called first");
        return connection;
    }

    /** Mutex callers should synchronize on before touching {@link #connection()}. */
    public Object lock() {
        return lock;
    }

    /** Path / JDBC URL the connection was opened against. Surfaces in error messages and tests. */
    public String url() {
        return url;
    }

    @Override
    public synchronized void close() {
        if (connection == null) return;
        try {
            connection.close();
        } catch (SQLException ignore) {
            // Closing a connection should never fail in a way the user can act on.
        }
        connection = null;
    }
}
