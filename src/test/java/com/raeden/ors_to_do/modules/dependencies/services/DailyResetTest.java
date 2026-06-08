package com.raeden.ors_to_do.modules.dependencies.services;

import com.raeden.ors_to_do.dependencies.models.AppStats;
import com.raeden.ors_to_do.dependencies.models.CustomStat;
import com.raeden.ors_to_do.dependencies.models.DailyTemplate;
import com.raeden.ors_to_do.dependencies.models.SectionConfig;
import com.raeden.ors_to_do.dependencies.models.TaskItem;
import com.raeden.ors_to_do.dependencies.storage.StorageManager;
import com.raeden.ors_to_do.modules.dependencies.ui.utils.TaskActionHandler;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Verifies the daily-reset behavior the user specified: a resettable page that adds template cards
 * must always return to exactly the template count. Completed cards are archived (rewards already
 * applied at completion); incomplete cards have their miss penalty applied and are then deleted.
 */
public class DailyResetTest {

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    private AppStats appStats;
    private CustomStat strength;
    private SectionConfig daily;

    @Before
    public void setUp() {
        // processDailyRollover persists via StorageManager — point it at a throwaway DB.
        StorageManager.setDataDirectoryForTesting(tmp.getRoot());

        appStats = new AppStats();
        appStats.setGlobalStatsEnabled(true);
        appStats.setGlobalScore(100);

        strength = new CustomStat("Strength", "💪", "#000000", "#FF0000");
        strength.setCurrentAmount(50);
        appStats.getCustomStats().add(strength);

        daily = new SectionConfig("DAILY", "Daily");
        daily.setResetIntervalHours(24);
        daily.setEnableScore(true);
        daily.setEnableStatsSystem(true);
        daily.getAutoAddTemplates().add(new DailyTemplate("", "Daily Pushups", null, null));
        appStats.getSections().add(daily);

        // Pretend the app was last opened yesterday so the rollover fires.
        appStats.setLastOpenedDate(LocalDate.now().minusDays(1));
    }

    @After
    public void tearDown() {
        StorageManager.close();
    }

    private TaskItem incompleteWithPenalty(String text) {
        TaskItem t = new TaskItem(text, null, "DAILY");
        t.setPenaltyPoints(10);
        t.getStatPenalties().put(strength.getId(), 5);
        return t;
    }

    @Test
    public void reset_archivesCompleted_deletesIncompleteWithPenalty_andReturnsToTemplateCount() {
        TaskItem completed = new TaskItem("Did it", null, "DAILY");
        completed.setFinished(true);

        TaskItem incomplete = incompleteWithPenalty("Skipped it");

        TaskItem counterMaxed = new TaskItem("Counter done", null, "DAILY");
        counterMaxed.setCounterMode(true);
        counterMaxed.setMaxCount(3);
        counterMaxed.setCurrentCount(3);   // reached target == completed

        TaskItem counterBelow = incompleteWithPenalty("Counter unfinished");
        counterBelow.setCounterMode(true);
        counterBelow.setMaxCount(3);
        counterBelow.setCurrentCount(1);   // below target == incomplete

        List<TaskItem> db = new ArrayList<>();
        db.add(completed);
        db.add(incomplete);
        db.add(counterMaxed);
        db.add(counterBelow);

        DailyRolloverManager.processDailyRollover(appStats, db);

        // Completed + maxed counter are archived (kept, out of the page).
        assertTrue(db.contains(completed));
        assertTrue(completed.isArchived());
        assertTrue(db.contains(counterMaxed));
        assertTrue(counterMaxed.isArchived());
        assertTrue("maxed counter should read as finished in archive", counterMaxed.isFinished());

        // Incomplete tasks are deleted outright.
        assertFalse(db.contains(incomplete));
        assertFalse(db.contains(counterBelow));

        // Exactly one active card remains — the freshly added template.
        long activeInDaily = db.stream()
                .filter(t -> "DAILY".equals(t.getSectionId()) && !t.isArchived())
                .count();
        assertEquals(1, activeInDaily);
        assertTrue(db.stream().anyMatch(t -> "Daily Pushups".equals(t.getTextContent()) && !t.isArchived()));

        // Penalties from the two incomplete tasks applied: -5 Strength each, -10 points each.
        assertEquals(40, strength.getCurrentAmount());   // 50 - 5 - 5
        assertEquals(80, appStats.getGlobalScore());     // 100 - 10 - 10
        assertEquals(10, strength.getLifetimeLost());    // 5 + 5

        // The rollover advanced the "last opened" marker so it won't fire again today.
        assertEquals(LocalDate.now(), appStats.getLastOpenedDate());
    }

    @Test
    public void reset_emptyPage_justAddsTemplate() {
        List<TaskItem> db = new ArrayList<>();
        DailyRolloverManager.processDailyRollover(appStats, db);

        long activeInDaily = db.stream()
                .filter(t -> "DAILY".equals(t.getSectionId()) && !t.isArchived())
                .count();
        assertEquals(1, activeInDaily);
        // No tasks were missed, so stats are untouched.
        assertEquals(50, strength.getCurrentAmount());
        assertEquals(100, appStats.getGlobalScore());
    }

    // ---- applyMissPenalty unit behavior (no DB needed) ----------------------

    @Test
    public void applyMissPenalty_appliesOnceThenIsIdempotent() {
        TaskItem t = incompleteWithPenalty("x");

        boolean first = TaskActionHandler.applyMissPenalty(t, appStats, daily);
        assertTrue(first);
        assertTrue(t.isPenaltyApplied());
        assertEquals(45, strength.getCurrentAmount());
        assertEquals(90, appStats.getGlobalScore());

        // Second call is a no-op because penaltyApplied is already set.
        boolean second = TaskActionHandler.applyMissPenalty(t, appStats, daily);
        assertFalse(second);
        assertEquals(45, strength.getCurrentAmount());
        assertEquals(90, appStats.getGlobalScore());
    }

    @Test
    public void applyMissPenalty_skipsFinishedTasksAndPenaltyFreeTasks() {
        TaskItem finished = incompleteWithPenalty("done");
        finished.setFinished(true);
        assertFalse(TaskActionHandler.applyMissPenalty(finished, appStats, daily));

        TaskItem noPenalty = new TaskItem("free", null, "DAILY");
        assertFalse(TaskActionHandler.applyMissPenalty(noPenalty, appStats, daily));

        // Nothing moved.
        assertEquals(50, strength.getCurrentAmount());
        assertEquals(100, appStats.getGlobalScore());
    }
}
