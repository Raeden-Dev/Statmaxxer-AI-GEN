package com.raeden.ors_to_do.dependencies.storage.sqlite;

import com.google.gson.reflect.TypeToken;
import com.raeden.ors_to_do.dependencies.models.AppStats;
import com.raeden.ors_to_do.dependencies.models.TaskItem;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.Reader;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * First-launch importer: reads the user's existing JSON / legacy .dat files and migrates them
 * into the SQLite database, then renames the originals so the importer never runs twice.
 *
 * <p>This is the {@code storage_redesign.md} migration step, kept idempotent and reversible:
 * the .json originals are kept as {@code *.imported} sidecars instead of being deleted, so a
 * user can always roll back by closing the app, restoring the {@code .json} extension, and
 * blowing away the {@code tasktracker.db} file.</p>
 */
public final class JsonImporter {

    private final File dataDir;

    public JsonImporter(File dataDir) {
        this.dataDir = dataDir;
    }

    /**
     * Imports tasks + stats if the supplied DB is empty. Idempotent: subsequent calls become
     * no-ops because the source files are renamed and the DB is no longer empty.
     *
     * @return {@code true} when at least one row was imported.
     */
    public boolean importIfNeeded(Db db) throws SQLException {
        boolean dbEmpty = TaskRepository.count(db) == 0 && AppMetaRepository.isEmpty(db);
        if (!dbEmpty) return false;

        // Tasks: try the JSON file first, then the legacy .dat. Same logic as the old
        // StorageManager — we want byte-for-byte compatible behaviour during migration.
        List<TaskItem> tasks = readTasks();
        AppStats stats = readStats();

        boolean importedAny = false;

        if (!tasks.isEmpty()) {
            // ensureCollections() mirrors what the old loadTasks() did for missing fields.
            for (TaskItem t : tasks) {
                if (t.getStatRewards() == null) t.setStatRewards(new HashMap<>());
                if (t.getStatCapRewards() == null) t.setStatCapRewards(new HashMap<>());
                if (t.getStatCosts() == null) t.setStatCosts(new HashMap<>());
                if (t.getStatPenalties() == null) t.setStatPenalties(new HashMap<>());
                if (t.getStatRequirements() == null) t.setStatRequirements(new HashMap<>());
            }
            TaskRepository.saveAll(db, tasks);
            importedAny = true;
        }

        if (stats != null) {
            if (stats.getFocusStatRewards() == null) stats.setFocusStatRewards(new HashMap<>());
            if (stats.getUrgeQuotes() == null) stats.setUrgeQuotes(new ArrayList<>());
            AppMetaRepository.save(db, stats);
            importedAny = true;
        }

        // Rename — don't delete — the source files so the user can roll back. We rename here
        // regardless of whether anything imported so a partial state (e.g. tasks.json existed but
        // was empty) still flips the importer to "done".
        renameToImported(new File(dataDir, "tasks.json"));
        renameToImported(new File(dataDir, "stats.json"));
        renameToImported(new File(dataDir, "tasks.dat"));
        renameToImported(new File(dataDir, "stats.dat"));

        return importedAny;
    }

    // ---------------------------------------------------------------------
    // Source readers — preserve the exact code paths the legacy StorageManager used.
    // ---------------------------------------------------------------------

    @SuppressWarnings("unchecked")
    private List<TaskItem> readTasks() {
        Type taskListType = new TypeToken<List<TaskItem>>(){}.getType();
        List<TaskItem> fromJson = readJson(new File(dataDir, "tasks.json"), taskListType);
        if (fromJson != null) return fromJson;

        Object fromDat = readLegacyDat(new File(dataDir, "tasks.dat"));
        if (fromDat instanceof List) {
            try {
                return (List<TaskItem>) fromDat;
            } catch (ClassCastException ignore) {
                // Legacy .dat is unsalvageable; treat as no tasks rather than crash on launch.
            }
        }
        return new ArrayList<>();
    }

    private AppStats readStats() {
        AppStats fromJson = readJson(new File(dataDir, "stats.json"), AppStats.class);
        if (fromJson != null) return fromJson;

        Object fromDat = readLegacyDat(new File(dataDir, "stats.dat"));
        if (fromDat instanceof AppStats) return (AppStats) fromDat;

        return null;
    }

    @SuppressWarnings("unchecked")
    private <T> T readJson(File f, Type type) {
        if (!f.exists()) return null;
        try (Reader r = new InputStreamReader(new FileInputStream(f), StandardCharsets.UTF_8)) {
            return (T) GsonProvider.compact().fromJson(r, type);
        } catch (Exception e) {
            // Honour the legacy fall-through-to-backups behaviour for damaged JSON.
            for (String bak : new String[] { f.getPath() + ".bak1", f.getPath() + ".bak2", f.getPath() + ".bak3" }) {
                File b = new File(bak);
                if (!b.exists()) continue;
                try (Reader br = new InputStreamReader(new FileInputStream(b), StandardCharsets.UTF_8)) {
                    System.out.println("[JsonImporter] Recovered from backup: " + bak);
                    return (T) GsonProvider.compact().fromJson(br, type);
                } catch (Exception ignore) {
                    // Try the next backup.
                }
            }
            return null;
        }
    }

    private Object readLegacyDat(File f) {
        if (!f.exists()) return null;
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(f))) {
            return ois.readObject();
        } catch (Exception e) {
            System.err.println("[JsonImporter] Failed to read legacy .dat: " + f);
            return null;
        }
    }

    private void renameToImported(File f) {
        if (!f.exists()) return;
        try {
            Path target = f.toPath().resolveSibling(f.getName() + ".imported");
            Files.move(f.toPath(), target, StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception e) {
            System.err.println("[JsonImporter] Failed to rename " + f + ": " + e.getMessage());
        }
    }
}
