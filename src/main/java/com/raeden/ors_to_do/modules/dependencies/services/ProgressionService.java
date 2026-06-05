package com.raeden.ors_to_do.modules.dependencies.services;

import com.raeden.ors_to_do.dependencies.models.AppStats;
import com.raeden.ors_to_do.dependencies.models.CustomStat;
import com.raeden.ors_to_do.dependencies.models.TaskItem;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Pure, UI-free progression rules for perks and challenges.
 *
 * <p>This logic used to live inside the {@code PerkCard}/{@code ChallengeCard} constructors, which
 * also mutated the model and saved to disk as a side effect of *rendering* — fragile and impossible
 * to unit test. It now lives here as side-effect-light, deterministic methods so it can be covered
 * by tests (see {@code ProgressionServiceTest}) and reused by both cards and the rollover manager.</p>
 */
public final class ProgressionService {

    /** How long after creation a perk/challenge stays inactive before it can unlock. */
    public static final int SETUP_PHASE_MINUTES = 15;

    private ProgressionService() { }

    /**
     * Whether a hooked dependency task counts as "satisfied" for unlocking a perk/challenge that
     * depends on it.
     *
     * <p>Counter cards are the tricky case: they carry no stat requirements and no dependencies, so
     * the generic "no gates -> satisfied" rule would treat a freshly-created counter card as already
     * complete. A counter card is satisfied only once its counter reaches the configured maximum.</p>
     */
    public static boolean isDependencyUnlocked(TaskItem depTask) {
        if (depTask == null) return false;

        if (depTask.isCounterMode()) {
            return depTask.isFinished()
                    || (depTask.getMaxCount() > 0 && depTask.getCurrentCount() >= depTask.getMaxCount());
        }

        boolean hasNoGates = depTask.getStatRequirements().isEmpty()
                && (depTask.getDependsOnTaskIds() == null || depTask.getDependsOnTaskIds().isEmpty());

        return depTask.getPerkLevel() > 0 || depTask.isFinished() || hasNoGates;
    }

    /**
     * Whether every stat threshold and hooked dependency of {@code task} is currently met.
     * Does not consider the setup phase, deadlines or completion — purely the gating requirements.
     */
    public static boolean meetsRequirements(TaskItem task, AppStats appStats, List<TaskItem> globalDatabase) {
        if (task == null) return false;

        for (Map.Entry<String, Integer> req : task.getStatRequirements().entrySet()) {
            CustomStat stat = findStat(appStats, req.getKey());
            if (stat == null || stat.getCurrentAmount() < req.getValue()) {
                return false;
            }
        }

        if (task.getDependsOnTaskIds() != null) {
            for (String depId : task.getDependsOnTaskIds()) {
                TaskItem dep = findTask(globalDatabase, depId);
                if (dep != null && !isDependencyUnlocked(dep)) {
                    return false;
                }
            }
        }
        return true;
    }

    /** True if the perk/challenge is still inside its post-creation setup phase. */
    public static boolean isInSetupPhase(TaskItem task, LocalDateTime now) {
        if (task == null || task.getDateCreated() == null) return false;
        return now.isBefore(task.getDateCreated().plusMinutes(SETUP_PHASE_MINUTES));
    }

    /**
     * Applies the perk unlock/lost state transition to {@code perk} based on whether it currently
     * meets its requirements. Mutates the perk's unlocked/lost dates and level, but does NOT save —
     * the caller decides when to persist.
     *
     * @return {@code true} if any field changed (i.e. the caller should persist).
     */
    public static boolean recomputePerkState(TaskItem perk, boolean meetsRequirements, boolean isSetupPhase, LocalDateTime now) {
        if (perk == null) return false;

        if (meetsRequirements && !isSetupPhase && perk.getPerkUnlockedDate() == null) {
            perk.setPerkUnlockedDate(now);
            perk.setPerkLostDate(null);
            if (perk.getPerkLevel() == 0) perk.setPerkLevel(1);
            return true;
        }
        if (!meetsRequirements && perk.getPerkUnlockedDate() != null) {
            perk.setPerkLostDate(now);
            perk.setPerkUnlockedDate(null);
            perk.setPerkLevel(0);
            return true;
        }
        return false;
    }

    private static CustomStat findStat(AppStats appStats, String id) {
        if (appStats == null) return null;
        return appStats.getCustomStats().stream().filter(s -> s.getId().equals(id)).findFirst().orElse(null);
    }

    private static TaskItem findTask(List<TaskItem> tasks, String id) {
        if (tasks == null) return null;
        return tasks.stream().filter(t -> t.getId().equals(id)).findFirst().orElse(null);
    }
}
