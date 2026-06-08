package com.raeden.ors_to_do.dependencies.storage.sqlite;

import java.io.File;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Comparator;

/**
 * Daily .db snapshots — Phase 4 of {@code storage_redesign.md}.
 *
 * <p>SQLite's {@code VACUUM INTO} creates a fully-consistent, openable copy of the live database
 * in a single statement (no half-written pages even mid-write thanks to the implicit transaction),
 * which is exactly the "rotating .db snapshots replace the JSON .bak rotation" pattern the doc
 * specifies.</p>
 *
 * <p>One snapshot per UTC day is kept (so repeated launches on the same day are cheap), and old
 * snapshots beyond the retention window are pruned automatically.</p>
 */
public final class SnapshotManager {

    /** Number of most-recent snapshots to keep. */
    public static final int RETENTION = 7;

    private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final String FILE_PREFIX = "tasktracker_";
    private static final String FILE_EXT = ".db";

    private SnapshotManager() { }

    /**
     * Creates today's snapshot if one doesn't exist yet, then prunes any older snapshots beyond
     * the retention window. No-op on any error (the worst case is "no snapshot today").
     *
     * @return the snapshot file that was just written, or {@code null} if today's already existed
     *         or the operation failed.
     */
    public static File runDaily(Db db, File dataDir) {
        try {
            File backupDir = new File(dataDir, "backups");
            if (!backupDir.exists() && !backupDir.mkdirs()) return null;

            File todayFile = new File(backupDir, FILE_PREFIX + LocalDate.now().format(STAMP) + FILE_EXT);
            if (todayFile.exists()) return null;

            // VACUUM INTO writes a consistent point-in-time snapshot of the entire DB. Note: the
            // path is sql-string-escaped using the SQL standard single-quote-doubling trick.
            String escaped = todayFile.getAbsolutePath().replace("'", "''");
            synchronized (db.lock()) {
                try (Statement st = db.connection().createStatement()) {
                    st.execute("VACUUM INTO '" + escaped + "'");
                }
            }
            prune(backupDir);
            return todayFile;
        } catch (SQLException e) {
            System.err.println("[SnapshotManager] Backup failed: " + e.getMessage());
            return null;
        }
    }

    /** Keeps the most recent {@link #RETENTION} snapshots and deletes the rest. Visible for tests. */
    static void prune(File backupDir) {
        File[] all = backupDir.listFiles((d, name) -> name.startsWith(FILE_PREFIX) && name.endsWith(FILE_EXT));
        if (all == null || all.length <= RETENTION) return;

        // Filenames embed the date, so a name sort gives newest-last.
        Arrays.sort(all, Comparator.comparing(File::getName));
        for (int i = 0; i < all.length - RETENTION; i++) {
            if (!all[i].delete()) {
                System.err.println("[SnapshotManager] Could not delete old snapshot: " + all[i]);
            }
        }
    }
}
