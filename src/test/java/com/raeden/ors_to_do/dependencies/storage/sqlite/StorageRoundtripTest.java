package com.raeden.ors_to_do.dependencies.storage.sqlite;

import com.raeden.ors_to_do.dependencies.models.AppStats;
import com.raeden.ors_to_do.dependencies.models.CustomStat;
import com.raeden.ors_to_do.dependencies.models.SectionConfig;
import com.raeden.ors_to_do.dependencies.models.TaskItem;
import com.raeden.ors_to_do.dependencies.storage.StorageManager;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.FileWriter;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Integration tests that drive {@link StorageManager} against a fresh tmp directory each run.
 * Covers the full Phase 1/2/3/4 surface plus the JSON-to-SQLite migration importer.
 */
public class StorageRoundtripTest {

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    @Before
    public void beforeEach() throws Exception {
        StorageManager.setDataDirectoryForTesting(tmp.getRoot());
    }

    @After
    public void afterEach() {
        StorageManager.close();
    }

    // ---------------------------------------------------------------------
    // Phase 1 — drop-in API
    // ---------------------------------------------------------------------

    @Test
    public void saveAndLoadTasks_roundTrip() {
        TaskItem a = new TaskItem("Buy milk", null, "sec-1");
        TaskItem b = new TaskItem("Write report", null, "sec-1");
        b.setFinished(true);
        b.setRewardPoints(25);

        StorageManager.saveTasks(Arrays.asList(a, b));

        List<TaskItem> loaded = StorageManager.loadTasks();
        assertEquals(2, loaded.size());
        assertTrue(loaded.stream().anyMatch(t -> "Buy milk".equals(t.getTextContent()) && !t.isFinished()));
        assertTrue(loaded.stream().anyMatch(t -> "Write report".equals(t.getTextContent()) && t.isFinished() && t.getRewardPoints() == 25));
    }

    @Test
    public void saveTasks_deletesRowsMissingFromTheNewList() {
        TaskItem a = new TaskItem("a", null, "sec");
        TaskItem b = new TaskItem("b", null, "sec");
        StorageManager.saveTasks(Arrays.asList(a, b));
        assertEquals(2, StorageManager.loadTasks().size());

        // Re-save with only `b` — `a` should be deleted, matching old whole-list-rewrite semantics.
        StorageManager.saveTasks(java.util.Collections.singletonList(b));
        List<TaskItem> after = StorageManager.loadTasks();
        assertEquals(1, after.size());
        assertEquals("b", after.get(0).getTextContent());
    }

    @Test
    public void saveAndLoadStats_roundTrip() {
        AppStats s = new AppStats();
        s.setGlobalScore(123);
        s.setGlobalStatsEnabled(true);
        s.getSections().add(new SectionConfig("sec-1", "Daily"));
        CustomStat strength = new CustomStat("Strength", "💪", "#000000", "#FF0000");
        strength.setCurrentAmount(42);
        s.getCustomStats().add(strength);
        s.addHistoryRecord(LocalDate.of(2026, 6, 1), 0.85);
        s.getDeletedTaskHistory().add("Some task | 2026-06-01");

        StorageManager.saveStats(s);

        AppStats loaded = StorageManager.loadStats();
        assertEquals(123, loaded.getGlobalScore());
        assertTrue(loaded.isGlobalStatsEnabled());
        assertEquals(1, loaded.getSections().size());
        assertEquals("Daily", loaded.getSections().get(0).getName());
        assertEquals(1, loaded.getCustomStats().size());
        assertEquals(42, loaded.getCustomStats().get(0).getCurrentAmount());
        assertEquals(Double.valueOf(0.85), loaded.getHistoryLog().get(LocalDate.of(2026, 6, 1)));
        assertEquals(1, loaded.getDeletedTaskHistory().size());
    }

    @Test
    public void emptyDatabase_returnsDefaultStats() {
        AppStats loaded = StorageManager.loadStats();
        assertNotNull(loaded);
        // The default constructor seeds three priorities, so this is a fingerprint that we got
        // a fresh AppStats rather than something half-deserialized.
        assertEquals(3, loaded.getCustomPriorities().size());
    }

    // ---------------------------------------------------------------------
    // Phase 2 — surgical writes
    // ---------------------------------------------------------------------

    @Test
    public void upsertTask_writesOneRowWithoutTouchingOthers() {
        TaskItem a = new TaskItem("a", null, "sec");
        TaskItem b = new TaskItem("b", null, "sec");
        StorageManager.saveTasks(Arrays.asList(a, b));

        b.setTextContent("b-updated");
        StorageManager.upsertTask(b);

        List<TaskItem> loaded = StorageManager.loadTasks();
        assertEquals(2, loaded.size());
        assertTrue(loaded.stream().anyMatch(t -> "b-updated".equals(t.getTextContent())));
        assertTrue(loaded.stream().anyMatch(t -> "a".equals(t.getTextContent())));
    }

    @Test
    public void deleteTask_dropsOneRow() {
        TaskItem a = new TaskItem("a", null, "sec");
        TaskItem b = new TaskItem("b", null, "sec");
        StorageManager.saveTasks(Arrays.asList(a, b));

        StorageManager.deleteTask(a.getId());

        List<TaskItem> loaded = StorageManager.loadTasks();
        assertEquals(1, loaded.size());
        assertEquals("b", loaded.get(0).getTextContent());
    }

    // ---------------------------------------------------------------------
    // Phase 3 — lazy slices
    // ---------------------------------------------------------------------

    @Test
    public void loadActiveTasks_excludesArchived() {
        TaskItem active = new TaskItem("active", null, "sec");
        TaskItem archived = new TaskItem("archived", null, "sec");
        archived.setArchived(true);
        StorageManager.saveTasks(Arrays.asList(active, archived));

        List<TaskItem> a = StorageManager.loadActiveTasks();
        assertEquals(1, a.size());
        assertEquals("active", a.get(0).getTextContent());

        List<TaskItem> arch = StorageManager.loadArchivedTasks();
        assertEquals(1, arch.size());
        assertEquals("archived", arch.get(0).getTextContent());
    }

    @Test
    public void loadTasksBySection_filtersOnSectionColumn() {
        TaskItem inSecA = new TaskItem("a", null, "sec-A");
        TaskItem inSecB = new TaskItem("b", null, "sec-B");
        StorageManager.saveTasks(Arrays.asList(inSecA, inSecB));

        List<TaskItem> a = StorageManager.loadTasksBySection("sec-A");
        assertEquals(1, a.size());
        assertEquals("a", a.get(0).getTextContent());
    }

    // ---------------------------------------------------------------------
    // Phase 4 — daily snapshot
    // ---------------------------------------------------------------------

    @Test
    public void firstLaunch_createsTodaysSnapshot() {
        // Force ensureOpen() to run by issuing any read.
        StorageManager.loadTasks();
        File backupDir = new File(tmp.getRoot(), "backups");
        File[] snapshots = backupDir.exists() ? backupDir.listFiles((d, n) -> n.endsWith(".db")) : new File[0];
        assertNotNull(snapshots);
        assertTrue("Expected at least one .db snapshot under backups/", snapshots.length >= 1);
    }

    @Test
    public void snapshotManager_prune_keepsLastN() throws Exception {
        File backupDir = tmp.newFolder("backups");
        for (int i = 1; i <= SnapshotManager.RETENTION + 3; i++) {
            File f = new File(backupDir, String.format("tasktracker_2026%02d01.db", i));
            try (FileWriter w = new FileWriter(f)) { w.write("dummy"); }
        }
        SnapshotManager.prune(backupDir);
        File[] left = backupDir.listFiles((d, n) -> n.endsWith(".db"));
        assertNotNull(left);
        assertEquals(SnapshotManager.RETENTION, left.length);
    }

    // ---------------------------------------------------------------------
    // Migration importer
    // ---------------------------------------------------------------------

    @Test
    public void importer_migratesLegacyTasksJson() throws Exception {
        // Lay down a tasks.json sidecar before the DB ever opens.
        File tasksJson = new File(tmp.getRoot(), "tasks.json");
        try (FileWriter w = new FileWriter(tasksJson)) {
            w.write("[{\"id\":\"task-1\",\"textContent\":\"Imported\",\"sectionId\":\"old-sec\"," +
                    "\"finished\":false,\"archived\":false}]");
        }

        // First read triggers schema creation + importer.
        List<TaskItem> loaded = StorageManager.loadTasks();
        assertEquals(1, loaded.size());
        assertEquals("Imported", loaded.get(0).getTextContent());
        assertEquals("old-sec", loaded.get(0).getSectionId());

        // The source file should have been renamed to *.imported (idempotent guard).
        assertFalse("tasks.json should have been moved to .imported", tasksJson.exists());
        assertTrue(new File(tmp.getRoot(), "tasks.json.imported").exists());
    }

    @Test
    public void importer_isNoOpWhenDbAlreadyHasData() throws Exception {
        // Seed the DB directly via the normal API.
        TaskItem seeded = new TaskItem("seeded", null, "sec");
        StorageManager.saveTasks(new ArrayList<>(java.util.Collections.singletonList(seeded)));
        StorageManager.close();

        // Drop a tasks.json into the same dir. Because the DB is non-empty, the importer must
        // leave it untouched.
        File tasksJson = new File(tmp.getRoot(), "tasks.json");
        try (FileWriter w = new FileWriter(tasksJson)) {
            w.write("[{\"id\":\"intruder\",\"textContent\":\"DO NOT IMPORT\",\"sectionId\":\"x\"}]");
        }

        List<TaskItem> loaded = StorageManager.loadTasks();
        assertEquals(1, loaded.size());
        assertEquals("seeded", loaded.get(0).getTextContent());
        assertTrue("tasks.json should NOT be renamed when DB had data", tasksJson.exists());
    }
}
