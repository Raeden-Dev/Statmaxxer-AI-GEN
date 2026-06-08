package com.raeden.ors_to_do.modules.dependencies.ui.utils;

import com.raeden.ors_to_do.dependencies.models.*;
import com.raeden.ors_to_do.dependencies.storage.StorageManager;
import com.raeden.ors_to_do.modules.dependencies.ui.dialogs.TaskDialogs;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.raeden.ors_to_do.modules.dependencies.services.SystemTrayManager.pushNotification;

public class TaskActionHandler {

    // --- NEW: Dynamic Aura Threshold Evaluator ---
    public static void evaluateThresholdDebuffs(AppStats appStats) {
        if (appStats.getCustomStats() == null || appStats.getDebuffTemplates() == null) return;

        Set<String> requiredAuraDebuffIds = new HashSet<>();

        // 1. Identify all Debuffs that SHOULD be active based on thresholds
        for (CustomStat stat : appStats.getCustomStats()) {
            for (StatThreshold t : stat.getThresholds()) {
                boolean conditionMet = t.isUpperThreshold()
                        ? stat.getCurrentAmount() >= t.getThresholdValue()
                        : stat.getCurrentAmount() <= t.getThresholdValue();

                if (conditionMet) {
                    requiredAuraDebuffIds.add(t.getDebuffId());
                }
            }
        }

        boolean changed = false;

        // 2. Remove Auras that are no longer required (stat was fixed)
        if (appStats.getActiveDebuffs().removeIf(d -> d.isAura() && !requiredAuraDebuffIds.contains(d.getId()))) {
            changed = true;
            pushNotification("Stat Improved!", "A stat threshold aura has been lifted.");
        }

        // 3. Inflict Auras that are newly required
        for (String reqId : requiredAuraDebuffIds) {
            boolean active = appStats.getActiveDebuffs().stream().anyMatch(d -> d.getId().equals(reqId) && d.isAura());
            if (!active) {
                Debuff template = appStats.getDebuffTemplates().stream().filter(d -> d.getId().equals(reqId)).findFirst().orElse(null);
                if (template != null) {
                    Debuff aura = template.cloneAsActive();
                    aura.setAura(true);
                    aura.setRequiredTaskCompletions(0); // Can't be cleansed
                    aura.setDurationHours(0); // Can't expire
                    appStats.getActiveDebuffs().add(aura);
                    changed = true;
                    pushNotification("Stat Threshold Reached!", "Afflicted with permanent Aura: " + aura.getName());
                }
            }
        }

        if (changed) StorageManager.saveStats(appStats);
    }

    public static void applyInflictedDebuffs(TaskItem task, AppStats appStats) {
        if (task.getInflictedDebuffIds().isEmpty() || appStats.getDebuffTemplates() == null) return;

        boolean changed = false;
        for (String dId : task.getInflictedDebuffIds()) {
            Debuff template = appStats.getDebuffTemplates().stream().filter(d -> d.getId().equals(dId)).findFirst().orElse(null);
            if (template != null) {
                boolean applied = false;
                for (Debuff active : appStats.getActiveDebuffs()) {
                    if (active.getId().equals(template.getId()) && !active.isAura()) { // Don't stack into Auras
                        if (template.isAllowStacking()) {
                            if (active.getCurrentStacks() < template.getMaxStacks()) {
                                active.setCurrentStacks(active.getCurrentStacks() + 1);
                                applied = true;
                                changed = true;
                                pushNotification("Debuff Stacked!", "You gained another stack of: " + template.getName());
                                break;
                            }
                        }
                        applied = true;
                        break;
                    }
                }
                if (!applied) {
                    appStats.getActiveDebuffs().add(template.cloneAsActive());
                    changed = true;
                    pushNotification("Debuff Inflicted!", "You have been afflicted with: " + template.getName());
                }
            }
        }
        if (changed) StorageManager.saveStats(appStats);
    }

    public static void handleRewardPurchase(TaskItem task, SectionConfig config, AppStats appStats, List<TaskItem> globalDatabase, Runnable onUpdate) {
        if (appStats.getGlobalScore() < task.getCostPoints()) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "Not enough points! You need " + task.getCostPoints() + " but only have " + appStats.getGlobalScore() + ".");
            alert.setHeaderText("Cannot Buy Reward");
            TaskDialogs.styleDialog(alert);
            alert.show();
            return;
        }

        StringBuilder modifierStr = new StringBuilder();
        if (config != null && config.isEnableStatsSystem()) {
            for (CustomStat stat : appStats.getCustomStats()) {
                int capAmt = getStatValue(task.getStatCapRewards(), stat);
                if (capAmt > 0) modifierStr.append("▲ ").append(capAmt).append(" Max ").append(stat.getName()).append(" Cap\n");

                int rewardAmt = getStatValue(task.getStatRewards(), stat);
                if (rewardAmt > 0) modifierStr.append("+").append(rewardAmt).append(" ").append(stat.getName()).append(" XP\n");

                int costAmt = getStatValue(task.getStatCosts(), stat);
                if (costAmt > 0) modifierStr.append("-").append(costAmt).append(" ").append(stat.getName()).append(" XP (Cost)\n");
            }
        }

        String statInfo = modifierStr.toString().trim();
        String prompt = "Buy '" + task.getTextContent() + "' for " + task.getCostPoints() + " points?" +
                (statInfo.isEmpty() ? "" : "\n\nYou will also gain:\n" + statInfo);

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, prompt, ButtonType.YES, ButtonType.NO);
        alert.setHeaderText("Confirm Purchase");
        TaskDialogs.styleDialog(alert);

        alert.showAndWait().ifPresent(res -> {
            if (res == ButtonType.YES) {
                appStats.setGlobalScore(appStats.getGlobalScore() - task.getCostPoints());
                appStats.setLifetimePointsSpent(appStats.getLifetimePointsSpent() + task.getCostPoints());
                appStats.setRewardsClaimed(appStats.getRewardsClaimed() + 1);

                if (task.isCounterMode()) {
                    task.setCurrentCount(task.getCurrentCount() + 1);
                    if (task.getMaxCount() > 0 && task.getCurrentCount() >= task.getMaxCount()) {
                        task.setFinished(true);
                    }
                } else {
                    task.setFinished(true);
                }

                if (config != null && config.isEnableStatsSystem()) {
                    processRPGStats(task, appStats, true);
                }

                StorageManager.saveStats(appStats);
                StorageManager.saveTasks(globalDatabase);
                onUpdate.run();
                pushNotification("Reward Claimed!", "You bought: " + task.getTextContent());
            }
        });
    }

    public static void handleTaskCompletion(TaskItem task, SectionConfig config, AppStats appStats, List<TaskItem> globalDatabase, Runnable onUpdate, CheckBox optCheckBox) {
        boolean hasRewards = task.getRewardPoints() > 0 || (config.isEnableStatsSystem() && task.getStatRewards() != null && !task.getStatRewards().isEmpty());
        boolean hasCosts = config.isEnableStatsSystem() && task.getStatCosts() != null && !task.getStatCosts().isEmpty();
        boolean hasCapRewards = config.isEnableStatsSystem() && task.getStatCapRewards() != null && !task.getStatCapRewards().isEmpty();
        boolean hasModifiers = hasRewards || hasCosts || hasCapRewards;

        List<String> reqSections = appStats.getRequireConfirmationSections();
        boolean needsConfirmation = (hasModifiers && task.isPermaLock()) || reqSections.contains("ALL") || reqSections.contains(config.getId());

        if (needsConfirmation && !task.isPointsClaimed()) {
            String promptText;
            String titleText;

            if (hasModifiers) {
                StringBuilder modifierStr = new StringBuilder();
                if (task.getRewardPoints() > 0) modifierStr.append("+").append(task.getRewardPoints()).append(" Global Points\n");

                if (config.isEnableStatsSystem()) {
                    for (CustomStat stat : appStats.getCustomStats()) {
                        int capAmt = getStatValue(task.getStatCapRewards(), stat);
                        if (capAmt > 0) modifierStr.append("▲ ").append(capAmt).append(" Max ").append(stat.getName()).append(" Cap\n");

                        int rewardAmt = getStatValue(task.getStatRewards(), stat);
                        if (rewardAmt > 0) modifierStr.append("+").append(rewardAmt).append(" ").append(stat.getName()).append(" XP\n");

                        int costAmt = getStatValue(task.getStatCosts(), stat);
                        if (costAmt > 0) modifierStr.append("-").append(costAmt).append(" ").append(stat.getName()).append(" XP (Cost)\n");
                    }
                }
                titleText = "Complete Task & Process Stats";
                promptText = "Process the following changes?\n\n" + modifierStr.toString().trim() +
                        (task.isPermaLock() ? "\n\nThis will permanently lock the task." : "");
            } else {
                titleText = "Confirm Task Completion";
                promptText = "Are you sure you want to mark '" + task.getTextContent() + "' as completed?";
            }

            Alert alert = new Alert(Alert.AlertType.CONFIRMATION, promptText, ButtonType.YES, ButtonType.NO);
            alert.setHeaderText(titleText);
            TaskDialogs.styleDialog(alert);

            alert.showAndWait().ifPresent(res -> {
                if (res == ButtonType.YES) {
                    finalizeCompletion(task, appStats, config, hasModifiers);
                    StorageManager.saveStats(appStats);
                    StorageManager.saveTasks(globalDatabase);
                    onUpdate.run();
                } else {
                    if (optCheckBox != null) optCheckBox.setSelected(false);
                    if (task.isCounterMode()) task.setCurrentCount(task.getCurrentCount() - 1);
                    onUpdate.run();
                }
            });
        } else {
            finalizeCompletion(task, appStats, config, hasModifiers);
            StorageManager.saveTasks(globalDatabase);
            onUpdate.run();
        }
    }

    private static void finalizeCompletion(TaskItem task, AppStats appStats, SectionConfig config, boolean hasModifiers) {
        task.setFinished(true);

        if (task.isCounterMode() && task.getMaxCount() > 0) task.setCurrentCount(task.getMaxCount());
        for (SubTask sub : task.getSubTasks()) sub.setFinished(true);

        if (appStats.getActiveDebuffs() != null && !appStats.getActiveDebuffs().isEmpty() && !task.isPointsClaimed()) {
            boolean cleansed = false;
            java.util.Iterator<Debuff> it = appStats.getActiveDebuffs().iterator();
            while (it.hasNext()) {
                Debuff d = it.next();
                if (!d.isAura() && d.getRequiredTaskCompletions() > 0) { // Can't cleanse Auras with tasks
                    d.setCurrentTaskCompletions(d.getCurrentTaskCompletions() + 1);
                    if (d.getCurrentTaskCompletions() >= d.getRequiredTaskCompletions()) {
                        it.remove();
                        cleansed = true;
                        pushNotification("Debuff Cleansed!", "You have overcome: " + d.getName());
                    }
                }
            }
            if (cleansed) StorageManager.saveStats(appStats);
        }

        if (hasModifiers && !task.isPointsClaimed()) {
            task.setPointsClaimed(true);

            applyInflictedDebuffs(task, appStats);

            appStats.setGlobalScore(appStats.getGlobalScore() + task.getRewardPoints());
            if (config.isEnableStatsSystem()) {
                processRPGStats(task, appStats, true);
            }
        }
    }

    /**
     * Applies the "missed task" penalty exactly once: subtracts the task's {@code penaltyPoints}
     * from the global score and reduces each stat named in the task's {@code statPenalties} map.
     *
     * <p>Previously the {@code statPenalties} map was shown in the UI but never actually applied to
     * any stat — the only "miss" penalty in the app reversed the would-be rewards. This is the
     * single, shared penalty path used by both the deadline-miss handler and the daily-reset
     * sweep, so "missing a task" costs the same no matter how the miss is detected.</p>
     *
     * <p>The method mutates {@code appStats} in memory only — it does <b>not</b> persist or
     * re-evaluate threshold auras; the caller decides when to save and whether to call
     * {@link #evaluateThresholdDebuffs(AppStats)}.</p>
     *
     * @return {@code true} if a penalty was actually applied (caller should persist), {@code false}
     *         when the task is finished, already penalised, or carries no penalty at all.
     */
    public static boolean applyMissPenalty(TaskItem task, AppStats appStats, SectionConfig config) {
        if (task == null || task.isFinished() || task.isPenaltyApplied()) return false;

        boolean statsEnabled = config != null && config.isEnableStatsSystem();
        boolean hasStatPenalties = statsEnabled && task.getStatPenalties() != null && !task.getStatPenalties().isEmpty();
        boolean hasAnyPenalty = task.getPenaltyPoints() > 0 || hasStatPenalties;
        if (!hasAnyPenalty) return false;

        task.setPenaltyApplied(true);
        appStats.setGlobalScore(appStats.getGlobalScore() - task.getPenaltyPoints());

        if (hasStatPenalties && appStats.isGlobalStatsEnabled()) {
            for (CustomStat stat : appStats.getCustomStats()) {
                int pen = getStatValue(task.getStatPenalties(), stat);
                if (pen > 0) {
                    stat.setCurrentAmount(Math.max(0, stat.getCurrentAmount() - pen));
                    stat.setLifetimeLost(stat.getLifetimeLost() + pen);
                }
            }
        }
        return true;
    }

    public static void processRPGStats(TaskItem task, AppStats appStats, boolean isCompletion) {
        if (!appStats.isGlobalStatsEnabled()) return;

        if (appStats.getActiveDebuffs() != null) {
            appStats.getActiveDebuffs().removeIf(d -> d.getExpiryDate() != null && java.time.LocalDateTime.now().isAfter(d.getExpiryDate()));
        }

        for (CustomStat stat : appStats.getCustomStats()) {
            int capAmt = getStatValue(task.getStatCapRewards(), stat);
            int rewardAmt = getStatValue(task.getStatRewards(), stat);
            int costAmt = getStatValue(task.getStatCosts(), stat);

            int effectiveCap = stat.getMaxCap();
            if (appStats.getActiveDebuffs() != null) {
                for (Debuff d : appStats.getActiveDebuffs()) {
                    if (d.getStatGainMultipliers().containsKey(stat.getId())) {
                        double multi = d.getStatGainMultipliers().get(stat.getId());
                        if (d.isAllowStacking() && d.getStatGainMultiplierStackReductions().containsKey(stat.getId())) {
                            multi -= d.getStatGainMultiplierStackReductions().get(stat.getId()) * (d.getCurrentStacks() - 1);
                        }
                        multi = Math.max(0.0, multi);
                        rewardAmt = (int) Math.round(rewardAmt * multi);
                    }
                    if (d.getStatCapReductions().containsKey(stat.getId())) {
                        int reduction = d.getStatCapReductions().get(stat.getId());
                        if (d.isAllowStacking() && d.getStatCapReductionStackIncreasers().containsKey(stat.getId())) {
                            reduction += d.getStatCapReductionStackIncreasers().get(stat.getId()) * (d.getCurrentStacks() - 1);
                        }
                        effectiveCap -= reduction;
                    }
                }
            }
            effectiveCap = Math.max(1, effectiveCap);

            if (isCompletion) {
                if (capAmt > 0 && stat.getMaxCap() > 0) {
                    stat.setMaxCap(stat.getMaxCap() + capAmt);
                    effectiveCap += capAmt;
                }

                if (rewardAmt > 0) {
                    int newAmount = stat.getCurrentAmount() + rewardAmt;
                    if (stat.getMaxCap() > 0 && newAmount > effectiveCap) {
                        newAmount = effectiveCap;
                    }
                    stat.setCurrentAmount(newAmount);
                    stat.setLifetimeEarned(stat.getLifetimeEarned() + rewardAmt);
                    if (stat.getCurrentAmount() > stat.getMaxLevelReached()) {
                        stat.setMaxLevelReached(stat.getCurrentAmount());
                    }
                    appStats.getLastStatGainDates().put(stat.getId(), java.time.LocalDate.now());
                }

                if (costAmt > 0) {
                    stat.setCurrentAmount(Math.max(0, stat.getCurrentAmount() - costAmt));
                    stat.setLifetimeLost(stat.getLifetimeLost() + costAmt);
                }

            } else {
                if (rewardAmt > 0) {
                    stat.setCurrentAmount(Math.max(0, stat.getCurrentAmount() - rewardAmt));
                    stat.setLifetimeLost(stat.getLifetimeLost() + rewardAmt);
                }

                if (costAmt > 0) {
                    int newAmount = stat.getCurrentAmount() + costAmt;
                    if (stat.getMaxCap() > 0 && newAmount > effectiveCap) {
                        newAmount = effectiveCap;
                    }
                    stat.setCurrentAmount(newAmount);
                }

                if (capAmt > 0 && stat.getMaxCap() > 0) {
                    stat.setMaxCap(Math.max(1, stat.getMaxCap() - capAmt));
                    effectiveCap = Math.max(1, effectiveCap - capAmt);

                    if (stat.getCurrentAmount() > effectiveCap) {
                        stat.setCurrentAmount(effectiveCap);
                    }
                }
            }
        }

        // --- NEW: Trigger Aura Check after XP changes ---
        evaluateThresholdDebuffs(appStats);
    }

    private static int getStatValue(Map<String, Integer> map, CustomStat stat) {
        if (map == null || map.isEmpty()) return 0;
        if (map.containsKey(stat.getId())) return map.get(stat.getId());
        if (map.containsKey(stat.getName())) return map.get(stat.getName());
        return 0;
    }
}