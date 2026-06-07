package com.raeden.ors_to_do.modules.dependencies.services;

import com.raeden.ors_to_do.dependencies.models.AppStats;
import com.raeden.ors_to_do.dependencies.models.CustomStat;
import com.raeden.ors_to_do.dependencies.models.TaskItem;
import org.junit.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Unit tests for {@link ProgressionService} — the perk/challenge gating rules that previously
 * lived (untested) inside the card constructors. The counter-card regression these guard against
 * was the reported bug: a counter card hooked to a perk unlocked the perk before the counter
 * reached its maximum.
 */
public class ProgressionServiceTest {

    private static TaskItem task(String text) {
        return new TaskItem(text, null, "section-1");
    }

    private static TaskItem counter(int current, int max) {
        TaskItem t = task("counter");
        t.setCounterMode(true);
        t.setMaxCount(max);
        t.setCurrentCount(current);
        return t;
    }

    // ---- isDependencyUnlocked -------------------------------------------------

    @Test
    public void counterBelowMax_isNotUnlocked() {
        assertFalse(ProgressionService.isDependencyUnlocked(counter(2, 5)));
    }

    @Test
    public void counterAtMax_isUnlocked() {
        assertTrue(ProgressionService.isDependencyUnlocked(counter(5, 5)));
    }

    @Test
    public void counterOverMax_isUnlocked() {
        assertTrue(ProgressionService.isDependencyUnlocked(counter(7, 5)));
    }

    @Test
    public void finishedTask_isUnlocked() {
        TaskItem t = task("done");
        t.setFinished(true);
        assertTrue(ProgressionService.isDependencyUnlocked(t));
    }

    @Test
    public void plainTaskWithoutGates_isUnlocked() {
        assertTrue(ProgressionService.isDependencyUnlocked(task("plain")));
    }

    @Test
    public void perkWithLevel_isUnlocked() {
        TaskItem t = task("perk");
        t.setPerkLevel(2);
        assertTrue(ProgressionService.isDependencyUnlocked(t));
    }

    @Test
    public void nullDependency_isNotUnlocked() {
        assertFalse(ProgressionService.isDependencyUnlocked(null));
    }

    @Test
    public void failedChallenge_doesNotSatisfyHook() {
        // A challenge marked Fail (finished but no perkUnlockedDate) used to satisfy hooked deps.
        TaskItem failed = task("failed-challenge");
        failed.setChallengeCard(true);
        failed.setFinished(true);
        failed.setPerkLostDate(LocalDateTime.now());
        // perkUnlockedDate remains null

        assertFalse(ProgressionService.isDependencyUnlocked(failed));
    }

    @Test
    public void conqueredChallenge_satisfiesHook() {
        TaskItem won = task("won-challenge");
        won.setChallengeCard(true);
        won.setFinished(true);
        won.setPerkUnlockedDate(LocalDateTime.now());

        assertTrue(ProgressionService.isDependencyUnlocked(won));
    }

    @Test
    public void unfinishedChallenge_doesNotSatisfyHook() {
        TaskItem inFlight = task("in-flight");
        inFlight.setChallengeCard(true);
        // not finished

        assertFalse(ProgressionService.isDependencyUnlocked(inFlight));
    }

    // ---- meetsRequirements ----------------------------------------------------

    @Test
    public void statThresholdUnmet_failsRequirements() {
        AppStats stats = new AppStats();
        CustomStat strength = new CustomStat();
        strength.setName("Strength");
        strength.setCurrentAmount(50);
        stats.getCustomStats().add(strength);

        TaskItem perk = task("perk");
        perk.getStatRequirements().put(strength.getId(), 100);

        assertFalse(ProgressionService.meetsRequirements(perk, stats, new ArrayList<>()));

        strength.setCurrentAmount(150);
        assertTrue(ProgressionService.meetsRequirements(perk, stats, new ArrayList<>()));
    }

    @Test
    public void counterDependencyBelowMax_failsRequirements() {
        // The reported bug: a perk hooked to a counter card must NOT unlock until the counter is maxed.
        AppStats stats = new AppStats();
        TaskItem counterDep = counter(1, 3);

        TaskItem perk = task("perk");
        perk.getDependsOnTaskIds().add(counterDep.getId());

        List<TaskItem> db = new ArrayList<>();
        db.add(counterDep);
        db.add(perk);

        assertFalse(ProgressionService.meetsRequirements(perk, stats, db));

        counterDep.setCurrentCount(3);
        assertTrue(ProgressionService.meetsRequirements(perk, stats, db));
    }

    // ---- recomputePerkState ---------------------------------------------------

    @Test
    public void recompute_unlockTransition_setsDateAndLevel() {
        TaskItem perk = task("perk");
        LocalDateTime now = LocalDateTime.now();

        boolean changed = ProgressionService.recomputePerkState(perk, true, false, now);

        assertTrue(changed);
        assertEquals(now, perk.getPerkUnlockedDate());
        assertNull(perk.getPerkLostDate());
        assertEquals(1, perk.getPerkLevel());
    }

    @Test
    public void recompute_lostTransition_clearsUnlockAndLevel() {
        TaskItem perk = task("perk");
        LocalDateTime earlier = LocalDateTime.now().minusDays(1);
        perk.setPerkUnlockedDate(earlier);
        perk.setPerkLevel(3);

        LocalDateTime now = LocalDateTime.now();
        boolean changed = ProgressionService.recomputePerkState(perk, false, false, now);

        assertTrue(changed);
        assertNull(perk.getPerkUnlockedDate());
        assertEquals(now, perk.getPerkLostDate());
        assertEquals(0, perk.getPerkLevel());
    }

    @Test
    public void recompute_duringSetupPhase_makesNoChange() {
        TaskItem perk = task("perk");
        boolean changed = ProgressionService.recomputePerkState(perk, true, true, LocalDateTime.now());

        assertFalse(changed);
        assertNull(perk.getPerkUnlockedDate());
        assertEquals(0, perk.getPerkLevel());
    }

    // ---- isInSetupPhase -------------------------------------------------------

    @Test
    public void freshTask_isInSetupPhase_butOldTaskIsNot() {
        TaskItem fresh = task("fresh");
        assertTrue(ProgressionService.isInSetupPhase(fresh, LocalDateTime.now()));

        LocalDateTime wellPastSetup = fresh.getDateCreated().plusMinutes(ProgressionService.SETUP_PHASE_MINUTES + 1);
        assertFalse(ProgressionService.isInSetupPhase(fresh, wellPastSetup));
    }
}
