package com.raeden.ors_to_do.dependencies.storage.sqlite;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Creates and stamps the SQLite schema described by {@code storage_redesign.md}.
 *
 * <p>The schema is the JSON-payload-on-top-of-relational-columns pattern: each table that
 * represents a model object has a {@code payload} TEXT column holding the full Gson serialization
 * plus a handful of denormalized columns that exist purely for the UI's hot filter/sort paths
 * (section grouping, archive split, deadline countdowns, pinned-on-top).</p>
 *
 * <p>{@link #ensure(Db)} is idempotent — safe to call on every app launch.</p>
 */
public final class SchemaManager {

    /** Bump when the schema changes and a migration is needed. */
    public static final int CURRENT_VERSION = 1;

    private SchemaManager() { }

    /**
     * Creates tables/indexes if missing and stamps {@code schema_version} so the importer knows
     * a brand-new database from one that's already been bootstrapped.
     *
     * @return {@code true} if this call wrote the initial schema (caller may want to trigger the
     *         JSON migration importer), {@code false} when the schema was already in place.
     */
    public static boolean ensure(Db db) throws SQLException {
        synchronized (db.lock()) {
            Connection c = db.connection();

            boolean alreadyInitialized = hasTable(c, "schema_version");
            if (alreadyInitialized) return false;

            try (Statement st = c.createStatement()) {
                // Tasks: row-per-task. The payload column is the source of truth; the named
                // columns are denormalized projections used by the UI's filter/sort code paths.
                st.executeUpdate("CREATE TABLE IF NOT EXISTS tasks (" +
                        " id TEXT PRIMARY KEY," +
                        " section_id TEXT," +
                        " text_content TEXT," +
                        " is_finished INTEGER NOT NULL DEFAULT 0," +
                        " is_archived INTEGER NOT NULL DEFAULT 0," +
                        " is_favorite INTEGER NOT NULL DEFAULT 0," +
                        " is_pinned INTEGER NOT NULL DEFAULT 0," +
                        " date_created TEXT," +
                        " date_completed TEXT," +
                        " deadline TEXT," +
                        " reward_points INTEGER NOT NULL DEFAULT 0," +
                        " payload TEXT NOT NULL" +
                        ")");
                st.executeUpdate("CREATE INDEX IF NOT EXISTS idx_tasks_section  ON tasks(section_id)");
                st.executeUpdate("CREATE INDEX IF NOT EXISTS idx_tasks_archived ON tasks(is_archived)");
                st.executeUpdate("CREATE INDEX IF NOT EXISTS idx_tasks_deadline ON tasks(deadline)");
                st.executeUpdate("CREATE INDEX IF NOT EXISTS idx_tasks_pinned   ON tasks(is_pinned)");

                // App config — single row, JSON payload. Logs are split out into their own
                // tables below so they don't bloat the object loaded on every launch.
                st.executeUpdate("CREATE TABLE IF NOT EXISTS app_meta (" +
                        " id INTEGER PRIMARY KEY CHECK (id = 1)," +
                        " payload TEXT NOT NULL" +
                        ")");

                st.executeUpdate("CREATE TABLE IF NOT EXISTS history_log (" +
                        " day TEXT PRIMARY KEY," +
                        " completion REAL" +
                        ")");

                st.executeUpdate("CREATE TABLE IF NOT EXISTS advanced_history_log (" +
                        " day TEXT PRIMARY KEY," +
                        " payload TEXT" +
                        ")");

                st.executeUpdate("CREATE TABLE IF NOT EXISTS deleted_task_history (" +
                        " rowid INTEGER PRIMARY KEY AUTOINCREMENT," +
                        " entry TEXT," +
                        " ts TEXT" +
                        ")");

                st.executeUpdate("CREATE TABLE schema_version (version INTEGER NOT NULL)");
                st.executeUpdate("INSERT INTO schema_version(version) VALUES (" + CURRENT_VERSION + ")");
            }
            return true;
        }
    }

    private static boolean hasTable(Connection c, String name) throws SQLException {
        try (Statement st = c.createStatement();
             ResultSet rs = st.executeQuery("SELECT name FROM sqlite_master WHERE type='table' AND name='" + name + "'")) {
            return rs.next();
        }
    }
}
