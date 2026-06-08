package com.raeden.ors_to_do.dependencies.models;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

public class AppStats implements Serializable {
    private static final long serialVersionUID = 1L;

    private Boolean runInBackground = null;
    private boolean runOnStartup = false;
    private boolean alwaysOnTop = false;
    private boolean enableNotifications = true;
    private boolean enableTextToTask = true;
    private boolean showSidebarTaskCount = true;

    private int taskFontSize = 14;
    private String checkboxTheme = "Default";
    private int zenModeThreshold = 20;
    private int focusInactivityThreshold = 5;
    private int minDailyCompletionPercent = 100;

    private int deadlineNotificationFrequencyHours = 24;

    private boolean matchTitleColor = false;
    private Boolean matchDailyRectColor = null;
    private boolean matchPriorityOutline = true;

    // --- NEW: Compress Notes Option ---
    private boolean compressNotes = false;

    private String editMenuText = "Edit Task";
    private String archiveMenuText = "Archive Task";
    private String deleteMenuText = "Delete";

    private String navQuickText = "Quick To-Do";
    private String navDailyText = "Daily To-Do";
    private String navWorkText = "Work List";
    private String navFocusText = "Focus Hub";
    private String navAnalyticsText = "Analytics";
    private String navArchiveText = "Archived";
    private String navSettingsText = "Settings";

    private String navFocusColor = "#E06666";
    private String navAnalyticsColor = "#F2C94C";
    private String navArchiveColor = "#C586C0";
    private String navSettingsColor = "#858585";

    private boolean globalStatsEnabled = false;
    private int globalScore = 0;
    private int currentStreak = 0;
    private int highestStreak = 0;
    private int lifetimeDeletedTasks = 0;
    private int lifetimePointsSpent = 0;
    private int rewardsClaimed = 0;
    private LocalDateTime analyticsResetTimestamp;

    private List<CustomStat> customStats = new ArrayList<>();
    private Map<String, Integer> statXpMap = new HashMap<>();
    private Map<String, LocalDate> lastStatGainDates = new HashMap<>();
    private String highestStreakSection = "None";

    private List<SectionConfig> sections = new ArrayList<>();
    private List<SectionConfig> sectionPresets = new ArrayList<>();
    private List<String> requireConfirmationSections = new ArrayList<>();

    private List<CustomPriority> customPriorities = new ArrayList<>(List.of(
            new CustomPriority("LOW", "#4EC9B0"),
            new CustomPriority("MED", "#FF8C00"),
            new CustomPriority("HIGH", "#FF6666")
    ));
    private List<DailyTemplate> baseDailies = new ArrayList<>();

    private List<Debuff> activeDebuffs = new ArrayList<>();
    private List<Debuff> debuffTemplates = new ArrayList<>();

    private LocalDate lastOpenedDate = LocalDate.now();
    private Map<LocalDate, Double> historyLog = new LinkedHashMap<>();
    private Map<LocalDate, int[]> advancedHistoryLog = new LinkedHashMap<>();
    private List<String> deletedTaskHistory = new ArrayList<>();
    private String brainDumpText = "";
    private Map<OriginModule, String> pendingDrafts = new HashMap<>();

    public boolean isRunInBackground() { return runInBackground == null ? true : runInBackground; }
    public void setRunInBackground(boolean runInBackground) { this.runInBackground = runInBackground; }
    public boolean isRunOnStartup() { return runOnStartup; }
    public void setRunOnStartup(boolean runOnStartup) { this.runOnStartup = runOnStartup; }
    public boolean isAlwaysOnTop() { return alwaysOnTop; }
    public void setAlwaysOnTop(boolean alwaysOnTop) { this.alwaysOnTop = alwaysOnTop; }
    public boolean isEnableNotifications() { return enableNotifications; }
    public void setEnableNotifications(boolean enableNotifications) { this.enableNotifications = enableNotifications; }
    public boolean isEnableTextToTask() { return enableTextToTask; }
    public void setEnableTextToTask(boolean enableTextToTask) { this.enableTextToTask = enableTextToTask; }
    public boolean isShowSidebarTaskCount() { return showSidebarTaskCount; }
    public void setShowSidebarTaskCount(boolean showSidebarTaskCount) { this.showSidebarTaskCount = showSidebarTaskCount; }
    public int getTaskFontSize() { return taskFontSize == 0 ? 14 : taskFontSize; }
    public void setTaskFontSize(int taskFontSize) { this.taskFontSize = taskFontSize; }
    public String getCheckboxTheme() { return checkboxTheme == null ? "Default" : checkboxTheme; }
    public void setCheckboxTheme(String checkboxTheme) { this.checkboxTheme = checkboxTheme; }
    public int getZenModeThreshold() { return zenModeThreshold <= 0 ? 20 : zenModeThreshold; }
    public void setZenModeThreshold(int zenModeThreshold) { this.zenModeThreshold = zenModeThreshold; }
    public int getFocusInactivityThreshold() { return focusInactivityThreshold; }
    public void setFocusInactivityThreshold(int focusInactivityThreshold) { this.focusInactivityThreshold = focusInactivityThreshold; }
    public int getMinDailyCompletionPercent() { return minDailyCompletionPercent < 10 ? 100 : minDailyCompletionPercent; }
    public void setMinDailyCompletionPercent(int minDailyCompletionPercent) { this.minDailyCompletionPercent = minDailyCompletionPercent; }
    public int getDeadlineNotificationFrequencyHours() { return deadlineNotificationFrequencyHours <= 0 ? 1 : deadlineNotificationFrequencyHours; }
    public void setDeadlineNotificationFrequencyHours(int hrs) { this.deadlineNotificationFrequencyHours = hrs; }
    public boolean isMatchTitleColor() { return matchTitleColor; }
    public void setMatchTitleColor(boolean matchTitleColor) { this.matchTitleColor = matchTitleColor; }
    public boolean isMatchDailyRectColor() { return matchDailyRectColor != null && matchDailyRectColor; }
    public void setMatchDailyRectColor(boolean matchDailyRectColor) { this.matchDailyRectColor = matchDailyRectColor; }
    public boolean isMatchPriorityOutline() { return matchPriorityOutline; }
    public void setMatchPriorityOutline(boolean matchPriorityOutline) { this.matchPriorityOutline = matchPriorityOutline; }

    public boolean isCompressNotes() { return compressNotes; }
    public void setCompressNotes(boolean compressNotes) { this.compressNotes = compressNotes; }

    public String getEditMenuText() { return editMenuText; }
    public void setEditMenuText(String editMenuText) { this.editMenuText = editMenuText; }
    public String getArchiveMenuText() { return archiveMenuText; }
    public void setArchiveMenuText(String archiveMenuText) { this.archiveMenuText = archiveMenuText; }
    public String getDeleteMenuText() { return deleteMenuText; }
    public void setDeleteMenuText(String deleteMenuText) { this.deleteMenuText = deleteMenuText; }
    public String getNavQuickText() { return navQuickText; }
    public void setNavQuickText(String navQuickText) { this.navQuickText = navQuickText; }
    public String getNavDailyText() { return navDailyText; }
    public void setNavDailyText(String navDailyText) { this.navDailyText = navDailyText; }
    public String getNavWorkText() { return navWorkText; }
    public void setNavWorkText(String navWorkText) { this.navWorkText = navWorkText; }
    public String getNavFocusText() { return navFocusText; }
    public void setNavFocusText(String navFocusText) { this.navFocusText = navFocusText; }
    public String getNavAnalyticsText() { return navAnalyticsText != null ? navAnalyticsText : "Analytics"; }
    public void setNavAnalyticsText(String navAnalyticsText) { this.navAnalyticsText = navAnalyticsText; }
    public String getNavArchiveText() { return navArchiveText; }
    public void setNavArchiveText(String navArchiveText) { this.navArchiveText = navArchiveText; }
    public String getNavSettingsText() { return navSettingsText; }
    public void setNavSettingsText(String navSettingsText) { this.navSettingsText = navSettingsText; }
    public String getNavFocusColor() { return navFocusColor != null ? navFocusColor : "#E06666"; }
    public void setNavFocusColor(String navFocusColor) { this.navFocusColor = navFocusColor; }
    public String getNavAnalyticsColor() { return navAnalyticsColor != null ? navAnalyticsColor : "#F2C94C"; }
    public void setNavAnalyticsColor(String navAnalyticsColor) { this.navAnalyticsColor = navAnalyticsColor; }
    public String getNavArchiveColor() { return navArchiveColor != null ? navArchiveColor : "#C586C0"; }
    public void setNavArchiveColor(String navArchiveColor) { this.navArchiveColor = navArchiveColor; }
    public String getNavSettingsColor() { return navSettingsColor != null ? navSettingsColor : "#858585"; }
    public void setNavSettingsColor(String navSettingsColor) { this.navSettingsColor = navSettingsColor; }
    public boolean isGlobalStatsEnabled() { return globalStatsEnabled; }
    public void setGlobalStatsEnabled(boolean globalStatsEnabled) { this.globalStatsEnabled = globalStatsEnabled; }
    public int getGlobalScore() { return globalScore; }
    public void setGlobalScore(int globalScore) { this.globalScore = globalScore; }
    public int getCurrentStreak() { return currentStreak; }
    public void setCurrentStreak(int currentStreak) {
        this.currentStreak = currentStreak;
        if (this.currentStreak > this.highestStreak) this.highestStreak = this.currentStreak;
    }
    public int getHighestStreak() { return highestStreak; }
    public void setHighestStreak(int highestStreak) { this.highestStreak = highestStreak; }
    public String getHighestStreakSection() { return highestStreakSection != null ? highestStreakSection : "None"; }
    public void setHighestStreakSection(String highestStreakSection) { this.highestStreakSection = highestStreakSection; }
    public int getLifetimeDeletedTasks() { return lifetimeDeletedTasks; }
    public void setLifetimeDeletedTasks(int lifetimeDeletedTasks) { this.lifetimeDeletedTasks = lifetimeDeletedTasks; }
    public int getLifetimePointsSpent() { return lifetimePointsSpent; }
    public void setLifetimePointsSpent(int lifetimePointsSpent) { this.lifetimePointsSpent = lifetimePointsSpent; }
    public int getRewardsClaimed() { return rewardsClaimed; }
    public void setRewardsClaimed(int rewardsClaimed) { this.rewardsClaimed = rewardsClaimed; }
    public LocalDateTime getAnalyticsResetTimestamp() { return analyticsResetTimestamp; }
    public void setAnalyticsResetTimestamp(LocalDateTime analyticsResetTimestamp) { this.analyticsResetTimestamp = analyticsResetTimestamp; }

    public List<CustomStat> getCustomStats() {
        if (customStats == null) customStats = new ArrayList<>();
        return customStats;
    }
    public void setCustomStats(List<CustomStat> customStats) { this.customStats = customStats; }

    public List<Debuff> getActiveDebuffs() {
        if (activeDebuffs == null) activeDebuffs = new ArrayList<>();
        return activeDebuffs;
    }
    public void setActiveDebuffs(List<Debuff> activeDebuffs) { this.activeDebuffs = activeDebuffs; }

    public List<Debuff> getDebuffTemplates() {
        if (debuffTemplates == null) debuffTemplates = new ArrayList<>();
        return debuffTemplates;
    }
    public void setDebuffTemplates(List<Debuff> debuffTemplates) { this.debuffTemplates = debuffTemplates; }

    public Map<String, Integer> getStatXpMap() {
        if (statXpMap == null) statXpMap = new HashMap<>();
        return statXpMap;
    }
    public void setStatXpMap(Map<String, Integer> statXpMap) { this.statXpMap = statXpMap; }

    public Map<String, LocalDate> getLastStatGainDates() {
        if (lastStatGainDates == null) lastStatGainDates = new HashMap<>();
        return lastStatGainDates;
    }

    public List<SectionConfig> getSections() {
        if (sections == null) sections = new ArrayList<>();
        return sections;
    }

    /** Convenience lookup. Returns null when no section matches. */
    public SectionConfig findSection(String sectionId) {
        if (sectionId == null) return null;
        for (SectionConfig sc : getSections()) {
            if (sectionId.equals(sc.getId())) return sc;
        }
        return null;
    }

    public List<SectionConfig> getSectionPresets() {
        if (sectionPresets == null) sectionPresets = new ArrayList<>();
        return sectionPresets;
    }
    public void setSectionPresets(List<SectionConfig> sectionPresets) { this.sectionPresets = sectionPresets; }

    public List<String> getRequireConfirmationSections() {
        if (requireConfirmationSections == null) requireConfirmationSections = new ArrayList<>();
        return requireConfirmationSections;
    }
    public void setRequireConfirmationSections(List<String> requireConfirmationSections) {
        this.requireConfirmationSections = requireConfirmationSections;
    }

    public List<CustomPriority> getCustomPriorities() {
        if (customPriorities == null) customPriorities = new ArrayList<>();
        return customPriorities;
    }

    public List<DailyTemplate> getBaseDailies() {
        if (baseDailies == null) baseDailies = new ArrayList<>();
        return baseDailies;
    }

    public LocalDate getLastOpenedDate() { return lastOpenedDate; }
    public void setLastOpenedDate(LocalDate lastOpenedDate) { this.lastOpenedDate = lastOpenedDate; }
    public Map<LocalDate, Double> getHistoryLog() { return historyLog; }
    public Map<LocalDate, int[]> getAdvancedHistoryLog() { return advancedHistoryLog; }

    public List<String> getDeletedTaskHistory() {
        if (deletedTaskHistory == null) deletedTaskHistory = new ArrayList<>();
        return deletedTaskHistory;
    }
    public void setDeletedTaskHistory(List<String> deletedTaskHistory) { this.deletedTaskHistory = deletedTaskHistory; }

    public String getBrainDumpText() { return brainDumpText; }
    public void setBrainDumpText(String brainDumpText) { this.brainDumpText = brainDumpText; }

    public Map<OriginModule, String> getPendingDrafts() { return pendingDrafts; }

    private boolean expandStatMiniCards = false;
    public boolean isExpandStatMiniCards() { return expandStatMiniCards; }
    public void setExpandStatMiniCards(boolean expandStatMiniCards) { this.expandStatMiniCards = expandStatMiniCards; }

    private int preventEditingHours = 0;
    public int getPreventEditingHours() { return preventEditingHours; }
    public void setPreventEditingHours(int preventEditingHours) { this.preventEditingHours = preventEditingHours; }

    /**
     * Persisted per-section collapse state for category groups. Keys are section ids; each value
     * is the set of category names the user has chosen to hide. Stored on AppStats so the UI
     * preserves the user's expand/collapse choices across restarts.
     */
    /**
     * Master switch for sidebar separator retractability. When {@code true} (default), separators
     * are clickable: hovering them shows a chevron, clicking toggles whether the section buttons
     * below the separator are visible. When {@code false}, separators behave like simple static
     * dividers (no hover, no click); turning the toggle off also collapses every separator so the
     * sidebar drops into a compact label-only mode the user can return from by re-enabling.
     */
    private Boolean sidebarSeparatorsCollapsible = null;
    public boolean isSidebarSeparatorsCollapsible() {
        return sidebarSeparatorsCollapsible == null || sidebarSeparatorsCollapsible;
    }
    public void setSidebarSeparatorsCollapsible(boolean v) { this.sidebarSeparatorsCollapsible = v; }

    /**
     * Per-separator collapse state in the sidebar. Keys are separator section ids; value is
     * {@code true} when the user has collapsed the separator (every section button below it,
     * up to the next separator, is hidden). Persisted so the sidebar reopens in the same shape.
     */
    private Map<String, Boolean> collapsedSeparators = new HashMap<>();
    public Map<String, Boolean> getCollapsedSeparators() {
        if (collapsedSeparators == null) collapsedSeparators = new HashMap<>();
        return collapsedSeparators;
    }
    public boolean isSeparatorCollapsed(String separatorId) {
        if (separatorId == null) return false;
        Boolean v = getCollapsedSeparators().get(separatorId);
        return v != null && v;
    }
    public void setSeparatorCollapsed(String separatorId, boolean collapsed) {
        if (separatorId == null) return;
        if (collapsed) getCollapsedSeparators().put(separatorId, true);
        else getCollapsedSeparators().remove(separatorId);
    }

    private Map<String, java.util.Set<String>> collapsedCategories = new HashMap<>();
    public Map<String, java.util.Set<String>> getCollapsedCategories() {
        if (collapsedCategories == null) collapsedCategories = new HashMap<>();
        return collapsedCategories;
    }
    public boolean isCategoryCollapsed(String sectionId, String categoryName) {
        if (sectionId == null || categoryName == null) return false;
        java.util.Set<String> set = getCollapsedCategories().get(sectionId);
        return set != null && set.contains(categoryName);
    }
    public void setCategoryCollapsed(String sectionId, String categoryName, boolean collapsed) {
        if (sectionId == null || categoryName == null) return;
        java.util.Set<String> set = getCollapsedCategories().computeIfAbsent(sectionId, k -> new java.util.HashSet<>());
        if (collapsed) set.add(categoryName); else set.remove(categoryName);
    }

    private boolean enableUrgeButton = true;
    private int urgeSessionDurationSeconds = 120;
    private List<String> urgeQuotes = new ArrayList<>();

    public boolean isEnableUrgeButton() { return enableUrgeButton; }
    public void setEnableUrgeButton(boolean enableUrgeButton) { this.enableUrgeButton = enableUrgeButton; }

    public int getUrgeSessionDurationSeconds() { return urgeSessionDurationSeconds; }
    public void setUrgeSessionDurationSeconds(int urgeSessionDurationSeconds) { this.urgeSessionDurationSeconds = urgeSessionDurationSeconds; }

    private String userBirthDateStr = "2005-01-01";
    private int userTargetAge = 80;
    private int lifetimeDeletedNotes = 0;
    private int lifetimeFullFocusSessions = 0;

    public java.time.LocalDate getUserBirthDate() {
        try { return java.time.LocalDate.parse(userBirthDateStr); }
        catch (Exception e) { return null; }
    }
    public void setUserBirthDate(java.time.LocalDate date) { this.userBirthDateStr = date != null ? date.toString() : null; }

    public int getUserTargetAge() { return userTargetAge; }
    public void setUserTargetAge(int userTargetAge) { this.userTargetAge = userTargetAge; }

    public int getLifetimeDeletedNotes() { return lifetimeDeletedNotes; }
    public void setLifetimeDeletedNotes(int lifetimeDeletedNotes) { this.lifetimeDeletedNotes = lifetimeDeletedNotes; }

    public int getLifetimeFullFocusSessions() { return lifetimeFullFocusSessions; }
    public void setLifetimeFullFocusSessions(int lifetimeFullFocusSessions) { this.lifetimeFullFocusSessions = lifetimeFullFocusSessions; }

    public List<String> getUrgeQuotes() {
        if (urgeQuotes == null) urgeQuotes = new ArrayList<>();
        if (urgeQuotes.isEmpty()) {
            urgeQuotes.add("This urge is just a feeling, and feelings pass.");
            urgeQuotes.add("Ride the wave. You don't have to act on it.");
            urgeQuotes.add("Breathe in strength, breathe out the craving.");
            urgeQuotes.add("You are in control of your actions.");
        }
        return urgeQuotes;
    }
    public void setUrgeQuotes(List<String> urgeQuotes) { this.urgeQuotes = urgeQuotes; }

    private int focusRewardPoints = 0;
    private java.util.Map<String, Integer> focusStatRewards = new java.util.HashMap<>();

    public int getFocusRewardPoints() { return focusRewardPoints; }
    public void setFocusRewardPoints(int focusRewardPoints) { this.focusRewardPoints = focusRewardPoints; }

    public java.util.Map<String, Integer> getFocusStatRewards() {
        if (focusStatRewards == null) focusStatRewards = new java.util.HashMap<>();
        return focusStatRewards;
    }
    public void setFocusStatRewards(java.util.Map<String, Integer> focusStatRewards) { this.focusStatRewards = focusStatRewards; }

    public void addHistoryRecord(LocalDate date, double completionPercentage) {
        historyLog.put(date, completionPercentage);
        if (historyLog.size() > 7) {
            LocalDate oldestDate = historyLog.keySet().iterator().next();
            historyLog.remove(oldestDate);
        }
    }

    public void addStatXp(String statId, int amount) {
        if (statXpMap == null) statXpMap = new HashMap<>();
        statXpMap.put(statId, statXpMap.getOrDefault(statId, 0) + amount);
    }

    public void saveDraft(OriginModule module, String text) {
        if (text != null && !text.trim().isEmpty()) {
            pendingDrafts.put(module, text.trim());
        } else {
            pendingDrafts.remove(module);
        }
    }

    public void copyFrom(AppStats other) {
        this.runInBackground = other.runInBackground;
        this.runOnStartup = other.runOnStartup;
        this.alwaysOnTop = other.alwaysOnTop;
        this.enableNotifications = other.enableNotifications;
        this.enableTextToTask = other.enableTextToTask;
        this.showSidebarTaskCount = other.showSidebarTaskCount;
        this.taskFontSize = other.taskFontSize;
        this.checkboxTheme = other.checkboxTheme;
        this.zenModeThreshold = other.zenModeThreshold;
        this.focusInactivityThreshold = other.focusInactivityThreshold;
        this.minDailyCompletionPercent = other.minDailyCompletionPercent;
        this.matchTitleColor = other.matchTitleColor;
        this.matchDailyRectColor = other.matchDailyRectColor;
        this.matchPriorityOutline = other.matchPriorityOutline;
        this.compressNotes = other.compressNotes; // Synced!

        this.editMenuText = other.editMenuText;
        this.archiveMenuText = other.archiveMenuText;
        this.deleteMenuText = other.deleteMenuText;
        this.deadlineNotificationFrequencyHours = other.deadlineNotificationFrequencyHours;

        this.navQuickText = other.navQuickText;
        this.navDailyText = other.navDailyText;
        this.navWorkText = other.navWorkText;
        this.navFocusText = other.navFocusText;
        this.navAnalyticsText = other.navAnalyticsText;
        this.navArchiveText = other.navArchiveText;
        this.navSettingsText = other.navSettingsText;
        this.navFocusColor = other.navFocusColor;
        this.navAnalyticsColor = other.navAnalyticsColor;
        this.navArchiveColor = other.navArchiveColor;
        this.navSettingsColor = other.navSettingsColor;

        this.globalStatsEnabled = other.globalStatsEnabled;
        this.globalScore = other.globalScore;
        this.currentStreak = other.currentStreak;
        this.highestStreak = other.highestStreak;
        this.lifetimeDeletedTasks = other.lifetimeDeletedTasks;
        this.lifetimePointsSpent = other.lifetimePointsSpent;
        this.rewardsClaimed = other.rewardsClaimed;
        this.analyticsResetTimestamp = other.analyticsResetTimestamp;

        this.customStats = new ArrayList<>(other.customStats);
        this.statXpMap = new HashMap<>(other.statXpMap);
        this.lastStatGainDates = new HashMap<>(other.lastStatGainDates);

        this.activeDebuffs = new ArrayList<>(other.getActiveDebuffs());
        this.debuffTemplates = new ArrayList<>(other.getDebuffTemplates());

        this.sections = new ArrayList<>(other.sections);
        this.sectionPresets = new ArrayList<>(other.sectionPresets);
        this.requireConfirmationSections = new ArrayList<>(other.requireConfirmationSections);
        this.customPriorities = new ArrayList<>(other.customPriorities);
        this.baseDailies = new ArrayList<>(other.baseDailies);

        this.lastOpenedDate = other.lastOpenedDate;
        this.historyLog = new LinkedHashMap<>(other.historyLog);
        this.advancedHistoryLog = new LinkedHashMap<>(other.advancedHistoryLog);
        this.deletedTaskHistory = new ArrayList<>(other.deletedTaskHistory);
        this.brainDumpText = other.brainDumpText;
        this.pendingDrafts = new HashMap<>(other.pendingDrafts);
        this.highestStreak = other.highestStreak;
    }
}