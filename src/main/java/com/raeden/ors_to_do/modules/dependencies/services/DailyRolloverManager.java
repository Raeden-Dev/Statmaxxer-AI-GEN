package com.raeden.ors_to_do.modules.dependencies.services;

import com.raeden.ors_to_do.dependencies.models.*;
import com.raeden.ors_to_do.dependencies.storage.StorageManager;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class DailyRolloverManager {

    public static void processDailyRollover(AppStats appStats, List<TaskItem> taskDatabase) {
        LocalDate today = LocalDate.now();
        LocalDate lastOpened = appStats.getLastOpenedDate();

        if (lastOpened == null) lastOpened = today;

        if (today.isAfter(lastOpened)) {
            long daysMissed = ChronoUnit.DAYS.between(lastOpened, today);

            for (SectionConfig section : appStats.getSections()) {
                if (section.getResetIntervalHours() > 0) {

                    int totalDaily = 0;
                    int completedDaily = 0;

                    for (TaskItem task : taskDatabase) {
                        if (section.getId().equals(task.getSectionId()) && !task.isArchived() && !task.isOptional()) {
                            totalDaily++;
                            if (task.isFinished()) completedDaily++;
                        }
                    }

                    if (section.isHasStreak() && totalDaily > 0) {
                        double percentComplete = (double) completedDaily / totalDaily;
                        appStats.addHistoryRecord(lastOpened, percentComplete);
                        appStats.getAdvancedHistoryLog().put(lastOpened, new int[]{totalDaily, completedDaily});

                        double requiredFraction = appStats.getMinDailyCompletionPercent() / 100.0;

                        if (daysMissed > 1) {
                            section.setCurrentStreak(0);
                        } else if (percentComplete >= (requiredFraction - 0.001)) {
                            section.setCurrentStreak(section.getCurrentStreak() + 1);
                            if (section.getCurrentStreak() > appStats.getHighestStreak()) {
                                appStats.setHighestStreak(section.getCurrentStreak());
                                appStats.setHighestStreakSection(section.getName());
                            }
                        } else {
                            section.setCurrentStreak(0);
                        }
                    }

                    // --- Reset-interval archive sweep ---
                    // Previously this loop archived (and silently finished) **every** unarchived
                    // task in the section, which:
                    //   - wiped progress on counter/repeating cards (which are meant to persist),
                    //   - silently flipped incomplete tasks to "finished" so they polluted streak
                    //     stats and challenge-completion detection.
                    // Now: only **completed**, non-counter, non-repeating tasks are archived. Tasks
                    // that weren't finished by the deadline simply stay on the page (the streak
                    // calculation above already counted them as a miss).
                    for (TaskItem task : taskDatabase) {
                        if (!section.getId().equals(task.getSectionId())) continue;
                        if (task.isArchived()) continue;
                        if (!task.isFinished()) continue;             // keep incomplete tasks visible
                        if (task.isCounterMode()) continue;           // counters persist across resets
                        if (task.isRepeatingMode()) continue;         // repeaters persist across resets
                        task.setArchived(true);
                    }

                    CustomPriority fallbackPrio = appStats.getCustomPriorities().isEmpty() ? null : appStats.getCustomPriorities().get(0);

                    for (DailyTemplate template : section.getAutoAddTemplates()) {
                        if (template.getActiveDays() != null && !template.getActiveDays().isEmpty() && !template.getActiveDays().contains(today.getDayOfWeek())) {
                            continue;
                        }

                        TaskItem newTask = new TaskItem(template.getText(), fallbackPrio, section.getId());

                        // --- FIXED: Formats prefix with brackets and safely injects properties ---
                        if (section.isEnableIcons() && template.getIconSymbol() != null && !template.getIconSymbol().equals("None")) {
                            newTask.setIconSymbol(template.getIconSymbol());
                            newTask.setIconColor(template.getIconColor());
                        }
                        if (section.isShowPrefix() && template.getPrefix() != null && !template.getPrefix().isEmpty()) {
                            String pText = template.getPrefix().trim();
                            if (!pText.isEmpty()) {
                                if (!pText.startsWith("[")) pText = "[" + pText;
                                if (!pText.endsWith("]")) pText = pText + "]";
                                newTask.setPrefix(pText.toUpperCase());
                                newTask.setPrefixColor(template.getPrefixColor());
                            }
                        }
                        if (section.isShowPriority() && template.getPriorityName() != null && !template.isOptional()) {
                            appStats.getCustomPriorities().stream().filter(p -> p.getName().equals(template.getPriorityName())).findFirst().ifPresent(newTask::setPriority);
                        }
                        if (section.isShowTaskType() && template.getTaskType() != null) newTask.setTaskType(template.getTaskType());
                        if (section.isEnableScore()) {
                            newTask.setRewardPoints(template.getRewardPoints());
                            newTask.setPenaltyPoints(template.getPenaltyPoints());
                        }
                        if (section.isEnableSubTasks() && template.getSubTaskLines() != null) {
                            for (String st : template.getSubTaskLines()) {
                                if (!st.trim().isEmpty()) newTask.getSubTasks().add(new SubTask(st.trim()));
                            }
                        }

                        if (template.getBgColor() != null) newTask.setColorHex(template.getBgColor());
                        if (template.getCustomOutlineColor() != null) newTask.setCustomOutlineColor(template.getCustomOutlineColor());
                        if (template.getCustomSideboxColor() != null) newTask.setCustomSideboxColor(template.getCustomSideboxColor());

                        newTask.setRepeatingMode(template.isRepeatingMode());
                        newTask.setRepetitionCount(template.getRepetitionCount());
                        newTask.setOptional(template.isOptional());

                        if (section.isEnableStatsSystem()) {
                            if (template.getStatRewards() != null) newTask.setStatRewards(new HashMap<>(template.getStatRewards()));
                            if (template.getStatCapRewards() != null) newTask.setStatCapRewards(new HashMap<>(template.getStatCapRewards()));
                            if (template.getStatCosts() != null) newTask.setStatCosts(new HashMap<>(template.getStatCosts()));
                            if (template.getStatPenalties() != null) newTask.setStatPenalties(new HashMap<>(template.getStatPenalties()));
                            if (template.getStatRequirements() != null) newTask.setStatRequirements(new HashMap<>(template.getStatRequirements()));
                            if (template.getInflictedDebuffIds() != null) newTask.setInflictedDebuffIds(new ArrayList<>(template.getInflictedDebuffIds()));
                        }

                        taskDatabase.add(newTask);
                    }
                }
            }

            processRPGRollover(appStats, taskDatabase);

            appStats.setLastOpenedDate(today);
            StorageManager.saveStats(appStats);
            StorageManager.saveTasks(taskDatabase);
        }
    }

    public static void autoArchiveTasks(AppStats appStats, List<TaskItem> taskDatabase) {
        for (TaskItem task : taskDatabase) {
            Optional<SectionConfig> matchedConfig = appStats.getSections().stream()
                    .filter(c -> c.getId().equals(task.getSectionId()))
                    .findFirst();

            if (matchedConfig.isPresent() && matchedConfig.get().isAutoArchive()) {
                if (task.isFinished() && !task.isArchived() && !task.isCounterMode()) {
                    task.setArchived(true);
                    if (task.getDateCompleted() == null) task.setFinished(true);
                }
            }
        }
    }

    public static void processRPGRollover(AppStats appStats, List<TaskItem> globalDatabase) {
        if (!appStats.isGlobalStatsEnabled()) return;

        LocalDate today = LocalDate.now();
        boolean statsChanged = false;
        boolean perksChanged = false;

        for (CustomStat stat : appStats.getCustomStats()) {
            if (stat.getAtrophyDays() > 0 && stat.getCurrentAmount() > 0) {
                LocalDate lastGain = appStats.getLastStatGainDates().getOrDefault(stat.getId(), today);
                long daysSinceGain = ChronoUnit.DAYS.between(lastGain, today);

                // Catch up on missed atrophy windows in one pass — previously this only ever ticked
                // a stat down by 1 even after a 10-day absence.
                boolean fired = false;
                while (daysSinceGain >= stat.getAtrophyDays() && stat.getCurrentAmount() > 0) {
                    stat.setCurrentAmount(Math.max(0, stat.getCurrentAmount() - 1));
                    stat.setLifetimeLost(stat.getLifetimeLost() + 1);
                    statsChanged = true;
                    fired = true;
                    lastGain = lastGain.plusDays(stat.getAtrophyDays());
                    daysSinceGain = ChronoUnit.DAYS.between(lastGain, today);
                }
                if (fired) {
                    appStats.getLastStatGainDates().put(stat.getId(), lastGain);
                    SystemTrayManager.pushNotification(
                            "Stat Atrophy: " + stat.getName(),
                            "Your " + stat.getName() + " stat has decayed due to inactivity! Complete a task to recover it."
                    );
                }
            }
        }

        boolean isLevelUpDay = (today.getDayOfWeek() == DayOfWeek.MONDAY);

        for (TaskItem task : globalDatabase) {
            // Skip challenges: they have stat requirements too (as unlock gates), but they are NOT
            // perks. Auto-levelling them on Monday rollover would let a hooked perk think the
            // challenge was "completed" before the user actually clicks Challenge Done.
            if (task.isChallengeCard()) continue;

            if (task.getStatRequirements() != null && !task.getStatRequirements().isEmpty()) {

                boolean meetsAllStats = true;

                for (Map.Entry<String, Integer> req : task.getStatRequirements().entrySet()) {
                    CustomStat s = appStats.getCustomStats().stream()
                            .filter(x -> x.getId().equals(req.getKey()))
                            .findFirst().orElse(null);

                    if (s == null || s.getCurrentAmount() < req.getValue()) {
                        meetsAllStats = false;
                        break;
                    }
                }

                if (meetsAllStats) {
                    if (isLevelUpDay && task.getPerkLevel() < 5) {
                        task.setWeeksMaintained(task.getWeeksMaintained() + 1);
                        task.setPerkLevel(task.getPerkLevel() + 1);
                        perksChanged = true;

                        SystemTrayManager.pushNotification(
                                "Perk Leveled Up! ✨",
                                "Your perk '" + task.getTextContent() + "' has reached Level " + task.getPerkLevel() + "!"
                        );
                    }
                } else {
                    if (task.getPerkLevel() > 0) {
                        task.setPerkLevel(0);
                        task.setWeeksMaintained(0);
                        perksChanged = true;

                        SystemTrayManager.pushNotification(
                                "Perk Lost! 🔒",
                                "Stats decayed below requirements. You have lost the perk: " + task.getTextContent()
                        );
                    }
                }
            }
        }

        if (statsChanged) StorageManager.saveStats(appStats);
        if (perksChanged) StorageManager.saveTasks(globalDatabase);
    }
}