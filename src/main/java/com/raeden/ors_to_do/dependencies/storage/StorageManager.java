package com.raeden.ors_to_do.dependencies.storage;

import com.raeden.ors_to_do.dependencies.models.AppStats;
import com.raeden.ors_to_do.dependencies.models.TaskItem;
import com.raeden.ors_to_do.dependencies.storage.sqlite.AppMetaRepository;
import com.raeden.ors_to_do.dependencies.storage.sqlite.Db;
import com.raeden.ors_to_do.dependencies.storage.sqlite.JsonImporter;
import com.raeden.ors_to_do.dependencies.storage.sqlite.SchemaManager;
import com.raeden.ors_to_do.dependencies.storage.sqlite.SnapshotManager;
import com.raeden.ors_to_do.dependencies.storage.sqlite.TaskRepository;

import java.io.File;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Persistence facade backed by SQLite ({@code storage_redesign.md}).
 *
 * <p>The public API keeps the names {@code saveTasks / loadTasks / saveStats / loadStats} that
 * call sites all over the app already use — so the storage swap is invisible to the rest of the
 * code. Underneath, those methods now drive the SQLite repositories. Three additional method
 * sets are exposed for callers that want to take advantage of the new backend:</p>
 * <ul>
 *   <li><b>Phase 2 surgical writes:</b> {@link #upsertTask(TaskItem)}, {@link #deleteTask(String)}.</li>
 *   <li><b>Phase 3 lazy slices:</b> {@link #loadActiveTasks()}, {@link #loadArchivedTasks()},
 *   {@link #loadTasksBySection(String)}.</li>
 *   <li><b>Folder access:</b> {@link #getDataDirectory()} (already used by the
 *   "Open Data Folder" button).</li>
 * </ul>
 *
 * <p>The DB is opened lazily on the first call to any method that touches it. On first access the
 * schema is created and the legacy {@code tasks.json}/{@code stats.json} files (if present) are
 * imported into the new database, then renamed to {@code *.imported}.</p>
 */
public class StorageManager {

    private static final String APP_DIR = System.getenv("APPDATA") + File.separator + "TaskTracker";
    private static final String DB_FILE_NAME = "tasktracker.db";

    /** The live DB connection. Initialised by {@link #ensureOpen()} on first use. */
    private static volatile Db db;

    /** Optional data-directory override — primarily for tests. {@code null} = use APP_DIR. */
    private static volatile File dataDirOverride;

    /**
     * Optional listener fired after any local data write. Used to trigger async cloud sync without
     * coupling the storage layer to the sync service. {@code null} = no listener.
     */
    private static volatile Runnable changeListener;

    /** Registers a callback invoked after each local data write (e.g. Google Drive sync). */
    public static void setChangeListener(Runnable listener) { changeListener = listener; }

    private static void notifyChanged() {
        Runnable l = changeListener;
        if (l != null) {
            try { l.run(); } catch (Throwable ignore) { /* sync must never break a save */ }
        }
    }

    /**
     * Optional listener invoked when a write to the local DB fails, so the UI can warn the user that
     * their latest change may not have been saved (instead of the failure only reaching stderr).
     */
    private static volatile java.util.function.Consumer<String> errorListener;
    private static volatile long lastErrorNotifyMs = 0;
    private static final long ERROR_NOTIFY_THROTTLE_MS = 15_000;

    /** Registers a callback invoked (throttled) when a local write fails. */
    public static void setErrorListener(java.util.function.Consumer<String> listener) { errorListener = listener; }

    private static void notifyError(String friendlyMessage, Throwable cause) {
        System.err.println("[StorageManager] " + friendlyMessage + (cause != null ? " (" + cause.getMessage() + ")" : ""));
        java.util.function.Consumer<String> l = errorListener;
        if (l == null) return;
        long now = System.currentTimeMillis();
        if (now - lastErrorNotifyMs < ERROR_NOTIFY_THROTTLE_MS) return; // don't spam on repeated failures
        lastErrorNotifyMs = now;
        try { l.accept(friendlyMessage); } catch (Throwable ignore) { }
    }

    /**
     * Active profile id. Each profile is a fully separate "world" backed by its own DB file. The
     * built-in {@code "default"} profile keeps the original {@code tasktracker.db} filename so
     * existing installs are unaffected; other profiles live in {@code tasktracker_<id>.db}.
     */
    private static volatile String activeProfileId = "default";

    /**
     * Switches the active profile. Closes the current DB so the next access opens the new profile's
     * database. Callers must reload tasks/stats and rebuild the UI afterwards.
     */
    public static synchronized void useProfile(String profileId) {
        close();
        activeProfileId = (profileId == null || profileId.isBlank()) ? "default" : profileId;
    }

    public static String getActiveProfileId() { return activeProfileId; }

    /** Resolves the DB filename for the active profile. */
    private static String dbFileName() {
        if (activeProfileId == null || activeProfileId.equals("default")) return DB_FILE_NAME;
        String safe = activeProfileId.replaceAll("[^a-zA-Z0-9_-]", "");
        if (safe.isEmpty()) return DB_FILE_NAME;
        return "tasktracker_" + safe + ".db";
    }

    private StorageManager() { }

    // ---------------------------------------------------------------------
    // Public API surface — Phase 1 drop-in replacements
    // ---------------------------------------------------------------------

    public static void saveTasks(List<TaskItem> tasks) {
        try {
            ensureOpen();
            TaskRepository.saveAll(db, tasks == null ? new ArrayList<>() : tasks);
            notifyChanged();
        } catch (SQLException e) {
            notifyError("Couldn't save your tasks — the last change may be lost. Check the data folder / disk.", e);
        }
    }

    public static List<TaskItem> loadTasks() {
        try {
            ensureOpen();
            return TaskRepository.loadAll(db);
        } catch (SQLException e) {
            System.err.println("[StorageManager] loadTasks failed: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    public static void saveStats(AppStats stats) {
        if (stats == null) return;
        try {
            ensureOpen();
            AppMetaRepository.save(db, stats);
            notifyChanged();
        } catch (SQLException e) {
            notifyError("Couldn't save your stats/settings — the last change may be lost. Check the data folder / disk.", e);
        }
    }

    public static AppStats loadStats() {
        try {
            ensureOpen();
            AppStats s = AppMetaRepository.load(db);
            return s != null ? s : new AppStats();
        } catch (SQLException e) {
            System.err.println("[StorageManager] loadStats failed: " + e.getMessage());
            return new AppStats();
        }
    }

    // ---------------------------------------------------------------------
    // Phase 2 — surgical writes (per-row, no full-rewrite cost)
    // ---------------------------------------------------------------------

    /** Upserts a single task — the per-row write the doc wants checkbox/edit/add to migrate to. */
    public static void upsertTask(TaskItem task) {
        try {
            ensureOpen();
            TaskRepository.upsert(db, task);
        } catch (SQLException e) {
            System.err.println("[StorageManager] upsertTask failed: " + e.getMessage());
        }
    }

    /** Deletes a single task by id. */
    public static void deleteTask(String taskId) {
        try {
            ensureOpen();
            TaskRepository.delete(db, taskId);
        } catch (SQLException e) {
            System.err.println("[StorageManager] deleteTask failed: " + e.getMessage());
        }
    }

    // ---------------------------------------------------------------------
    // Phase 3 — lazy slices (avoid loading the archive at startup)
    // ---------------------------------------------------------------------

    /** Active tasks only — what the main UI should be loading on launch. */
    public static List<TaskItem> loadActiveTasks() {
        try {
            ensureOpen();
            return TaskRepository.loadActive(db);
        } catch (SQLException e) {
            System.err.println("[StorageManager] loadActiveTasks failed: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    /** Archived tasks only. Lazily fetched when the Archive page opens. */
    public static List<TaskItem> loadArchivedTasks() {
        try {
            ensureOpen();
            return TaskRepository.loadArchived(db);
        } catch (SQLException e) {
            System.err.println("[StorageManager] loadArchivedTasks failed: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    /** All tasks (active + archived) belonging to a single section. */
    public static List<TaskItem> loadTasksBySection(String sectionId) {
        try {
            ensureOpen();
            return TaskRepository.loadBySection(db, sectionId);
        } catch (SQLException e) {
            System.err.println("[StorageManager] loadTasksBySection failed: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    // ---------------------------------------------------------------------
    // Misc / utility
    // ---------------------------------------------------------------------

    /**
     * Absolute path of the directory where the app keeps {@code tasktracker.db}, daily backups,
     * and any user-imported bundles. Exposed so settings UI can open the folder in the OS file
     * explorer.
     */
    public static File getDataDirectory() {
        File override = dataDirOverride;
        return override != null ? override : new File(APP_DIR);
    }

    /**
     * Overrides the data directory used for the live DB and snapshots. Intended for tests so they
     * can use a tmp dir; production code never calls this.
     */
    public static synchronized void setDataDirectoryForTesting(File dir) {
        close();
        dataDirOverride = dir;
    }

    /** Closes the live DB connection. Safe to call when nothing's been opened yet. */
    public static synchronized void close() {
        if (db != null) {
            db.close();
            db = null;
        }
    }

    // ---------------------------------------------------------------------
    // Internals
    // ---------------------------------------------------------------------

    /**
     * Lazily opens the DB, applies the schema, and runs the JSON migration importer + the daily
     * snapshot on first access. Subsequent calls are no-ops.
     */
    private static void ensureOpen() throws SQLException {
        if (db != null) return;
        synchronized (StorageManager.class) {
            if (db != null) return;

            File dir = getDataDirectory();
            if (!dir.exists() && !dir.mkdirs()) {
                throw new SQLException("Could not create data directory: " + dir);
            }

            File dbFile = new File(dir, dbFileName());
            String url = "jdbc:sqlite:" + dbFile.getAbsolutePath().replace('\\', '/');
            Db opening = new Db(url);
            opening.open();

            try {
                boolean freshInstall = SchemaManager.ensure(opening);
                if (freshInstall) {
                    // Pull anything left over from the JSON era into the new schema. Idempotent;
                    // no-ops once the source files have been renamed to *.imported.
                    new JsonImporter(dir).importIfNeeded(opening);
                }
            } catch (SQLException e) {
                opening.close();
                throw e;
            }

            db = opening;

            // Best-effort daily snapshot. Failure here must never block app startup, so the
            // SnapshotManager swallows its own errors.
            try { SnapshotManager.runDaily(db, dir); } catch (Throwable ignore) { }
        }
    }
}
