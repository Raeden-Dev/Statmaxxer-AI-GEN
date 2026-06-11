package com.raeden.ors_to_do.dependencies.models;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SectionConfig implements Serializable {
    private static final long serialVersionUID = 1L;

    private String id;
    private String name;
    private String sidebarColor = "#FFFFFF";

    private boolean isSeparator = false;

    private int resetIntervalHours = 0;
    private boolean hasStreak = false;
    private int currentStreak = 0;
    private boolean autoArchive = false;
    private boolean allowManualArchiving = true;
    private boolean enableSubTasks = true;
    private boolean showDate = true;
    private boolean showPrefix = false;
    private boolean showTags = false;
    private boolean enableScore = false;
    private boolean enableLinks = false;
    private boolean isRewardsPage = false;

    private boolean isStatPage = false;
    private boolean isPerkPage = false;
    private boolean isChallengePage = false;

    // --- Calendar Page mode (per-month grid, mark days a task was done, colour the cell). ---
    private boolean isCalendarPage = false;
    private boolean calendarShowSegments = true;   // colour bands across the day cell
    private boolean calendarShowDots = true;        // colour dots under the date number
    private boolean allowCalendarManipulation = false; // mark past/future days, not just "today"
    private boolean calendarGrantsXp = false;        // award Custom Stats XP on completion
    private List<CalendarTask> calendarTasks = new ArrayList<>();
    /** ISO date string ("yyyy-MM-dd") -> list of CalendarTask ids completed that day. */
    private Map<String, List<String>> calendarCompletions = new java.util.HashMap<>();

    // --- Journal / favorite / per-day icon (all keyed by ISO date string) ---
    private boolean calendarJournalEnabled = false;  // per-day notes available
    private boolean calendarJournalOnly = false;     // notes replace the task list
    private Map<String, String> calendarJournal = new java.util.HashMap<>();      // legacy: iso -> single note text
    /** iso date -> customizable journal/event cards for that day. */
    private Map<String, List<CalendarEntry>> calendarEntries = new java.util.HashMap<>();
    private List<String> calendarFavoriteDays = new ArrayList<>();                 // iso list
    private Map<String, String> calendarDayIcons = new java.util.HashMap<>();      // iso -> icon symbol
    private Map<String, String> calendarDayIconColors = new java.util.HashMap<>(); // iso -> icon color

    private boolean showPriority = true;
    private boolean trackTime = false;
    private boolean showTaskType = false;
    private boolean allowFavorite = true;
    private boolean showAnalytics = true;
    private boolean enableIcons = false;
    private boolean enableZenMode = true;
    private boolean enableStatsSystem = false;
    private boolean enableLinkCards = false;

    private boolean isNotesPage = false;

    private boolean enableOptionalTasks = false;
    private boolean enableTaskStyling = false;
    private boolean enableTimedTasks = false;

    private boolean allowRepeatingTasks = false;

    // --- NEW: Lock Task After Completion Flag ---
    private boolean lockCompletedTasks = false;

    // --- NEW: per-section "Prevent Editing (Hours)" (moved from AppStats so it can be turned
    // on/off per section instead of being a global hammer). 0 = disabled.
    private int preventEditingHours = 0;

    // --- NEW: per-section category support
    private boolean enableCategories = false;

    /**
     * Optional visual customization for category bars. Keyed by category name. Categories without
     * a matching entry render in the default dark theme. Kept as a list (not a map) for clean
     * Gson round-tripping with the existing serializer.
     */
    private List<CategoryStyle> categoryStyles = new ArrayList<>();

    private List<DailyTemplate> autoAddTemplates = new ArrayList<>();

    public SectionConfig(String id, String name) {
        this.id = id;
        this.name = name;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getSidebarColor() { return sidebarColor; }
    public void setSidebarColor(String sidebarColor) { this.sidebarColor = sidebarColor; }

    public boolean isSeparator() { return isSeparator; }
    public void setSeparator(boolean separator) { this.isSeparator = separator; }

    public int getResetIntervalHours() { return resetIntervalHours; }
    public void setResetIntervalHours(int resetIntervalHours) { this.resetIntervalHours = resetIntervalHours; }

    public boolean isHasStreak() { return hasStreak; }
    public void setHasStreak(boolean hasStreak) { this.hasStreak = hasStreak; }

    public int getCurrentStreak() { return currentStreak; }
    public void setCurrentStreak(int currentStreak) { this.currentStreak = currentStreak; }

    public boolean isAutoArchive() { return autoArchive; }
    public void setAutoArchive(boolean autoArchive) { this.autoArchive = autoArchive; }

    public boolean isAllowManualArchiving() { return allowManualArchiving; }
    public void setAllowManualArchiving(boolean allowManualArchiving) { this.allowManualArchiving = allowManualArchiving; }

    public boolean isEnableSubTasks() { return enableSubTasks; }
    public void setEnableSubTasks(boolean enableSubTasks) { this.enableSubTasks = enableSubTasks; }

    public boolean isShowDate() { return showDate; }
    public void setShowDate(boolean showDate) { this.showDate = showDate; }

    public boolean isShowPrefix() { return showPrefix; }
    public void setShowPrefix(boolean showPrefix) { this.showPrefix = showPrefix; }

    public boolean isShowTags() { return showTags; }
    public void setShowTags(boolean showTags) { this.showTags = showTags; }

    public boolean isEnableScore() { return enableScore; }
    public void setEnableScore(boolean enableScore) { this.enableScore = enableScore; }

    public boolean isEnableLinks() { return enableLinks; }
    public void setEnableLinks(boolean enableLinks) { this.enableLinks = enableLinks; }

    public boolean isRewardsPage() { return isRewardsPage; }
    public void setRewardsPage(boolean rewardsPage) { this.isRewardsPage = rewardsPage; }

    public boolean isStatPage() { return isStatPage; }
    public void setStatPage(boolean statPage) { this.isStatPage = statPage; }

    public boolean isPerkPage() { return isPerkPage; }
    public void setPerkPage(boolean perkPage) { this.isPerkPage = perkPage; }

    public boolean isChallengePage() { return isChallengePage; }
    public void setChallengePage(boolean challengePage) { this.isChallengePage = challengePage; }

    public boolean isCalendarPage() { return isCalendarPage; }
    public void setCalendarPage(boolean calendarPage) { this.isCalendarPage = calendarPage; }

    public boolean isCalendarShowSegments() { return calendarShowSegments; }
    public void setCalendarShowSegments(boolean v) { this.calendarShowSegments = v; }

    public boolean isCalendarShowDots() { return calendarShowDots; }
    public void setCalendarShowDots(boolean v) { this.calendarShowDots = v; }

    public boolean isAllowCalendarManipulation() { return allowCalendarManipulation; }
    public void setAllowCalendarManipulation(boolean v) { this.allowCalendarManipulation = v; }

    public boolean isCalendarGrantsXp() { return calendarGrantsXp; }
    public void setCalendarGrantsXp(boolean v) { this.calendarGrantsXp = v; }

    public List<CalendarTask> getCalendarTasks() {
        if (calendarTasks == null) calendarTasks = new ArrayList<>();
        return calendarTasks;
    }
    public void setCalendarTasks(List<CalendarTask> calendarTasks) { this.calendarTasks = calendarTasks; }

    public Map<String, List<String>> getCalendarCompletions() {
        if (calendarCompletions == null) calendarCompletions = new java.util.HashMap<>();
        return calendarCompletions;
    }
    public void setCalendarCompletions(Map<String, List<String>> m) { this.calendarCompletions = m; }

    /** @return the list of CalendarTask ids completed on {@code isoDate} (never null). */
    public List<String> getCompletedTaskIds(String isoDate) {
        List<String> ids = getCalendarCompletions().get(isoDate);
        return ids == null ? new ArrayList<>() : ids;
    }

    public boolean isTaskCompletedOn(String isoDate, String taskId) {
        List<String> ids = getCalendarCompletions().get(isoDate);
        return ids != null && ids.contains(taskId);
    }

    /** Toggles completion of {@code taskId} on {@code isoDate}. @return true if now completed. */
    public boolean toggleCompletion(String isoDate, String taskId) {
        List<String> ids = getCalendarCompletions().computeIfAbsent(isoDate, k -> new ArrayList<>());
        if (ids.contains(taskId)) {
            ids.remove(taskId);
            if (ids.isEmpty()) getCalendarCompletions().remove(isoDate);
            return false;
        }
        ids.add(taskId);
        return true;
    }

    public CalendarTask findCalendarTask(String taskId) {
        if (taskId == null) return null;
        for (CalendarTask t : getCalendarTasks()) if (taskId.equals(t.getId())) return t;
        return null;
    }

    // --- Journal ---
    public boolean isCalendarJournalEnabled() { return calendarJournalEnabled || calendarJournalOnly; }
    public void setCalendarJournalEnabled(boolean v) { this.calendarJournalEnabled = v; }

    public boolean isCalendarJournalOnly() { return calendarJournalOnly; }
    public void setCalendarJournalOnly(boolean v) { this.calendarJournalOnly = v; }

    /**
     * iso date -> list of journal/event cards. Lazily migrates the legacy single-note map
     * ({@code calendarJournal}) into journal entries on first access.
     */
    public Map<String, List<CalendarEntry>> getCalendarEntries() {
        if (calendarEntries == null) calendarEntries = new java.util.HashMap<>();
        if (calendarJournal != null && !calendarJournal.isEmpty()) {
            for (Map.Entry<String, String> e : calendarJournal.entrySet()) {
                if (e.getValue() == null || e.getValue().isBlank()) continue;
                calendarEntries.computeIfAbsent(e.getKey(), k -> new ArrayList<>())
                        .add(new CalendarEntry(false, e.getValue()));
            }
            calendarJournal.clear();
        }
        return calendarEntries;
    }

    public List<CalendarEntry> getDayEntries(String isoDate) {
        List<CalendarEntry> list = getCalendarEntries().get(isoDate);
        return list == null ? new ArrayList<>() : list;
    }
    public boolean hasDayEntries(String isoDate) {
        List<CalendarEntry> list = getCalendarEntries().get(isoDate);
        return list != null && !list.isEmpty();
    }
    public void addDayEntry(String isoDate, CalendarEntry entry) {
        getCalendarEntries().computeIfAbsent(isoDate, k -> new ArrayList<>()).add(entry);
    }
    public void removeDayEntry(String isoDate, String entryId) {
        List<CalendarEntry> list = getCalendarEntries().get(isoDate);
        if (list == null) return;
        list.removeIf(en -> en.getId().equals(entryId));
        if (list.isEmpty()) getCalendarEntries().remove(isoDate);
    }

    /** True if the day has any non-event journal entry. */
    public boolean hasDayNote(String isoDate) {
        for (CalendarEntry e : getDayEntries(isoDate)) if (!e.isEvent()) return true;
        return false;
    }

    // --- Single-note convenience for Task+Journal mode (at most one journal entry per day). ---
    public CalendarEntry getSingleJournalEntry(String isoDate) {
        for (CalendarEntry e : getDayEntries(isoDate)) if (!e.isEvent()) return e;
        return null;
    }
    public String getDayNote(String isoDate) {
        CalendarEntry e = getSingleJournalEntry(isoDate);
        return e == null ? "" : e.getText();
    }
    public void setDayNote(String isoDate, String text) {
        CalendarEntry existing = getSingleJournalEntry(isoDate);
        if (text == null || text.isBlank()) {
            if (existing != null) removeDayEntry(isoDate, existing.getId());
            return;
        }
        if (existing != null) existing.setText(text);
        else addDayEntry(isoDate, new CalendarEntry(false, text));
    }

    // --- Favorite days ---
    public List<String> getCalendarFavoriteDays() {
        if (calendarFavoriteDays == null) calendarFavoriteDays = new ArrayList<>();
        return calendarFavoriteDays;
    }
    public boolean isFavoriteDay(String isoDate) { return getCalendarFavoriteDays().contains(isoDate); }
    /** Toggles favorite state for a day. @return true if now favorited. */
    public boolean toggleFavoriteDay(String isoDate) {
        if (getCalendarFavoriteDays().remove(isoDate)) return false;
        getCalendarFavoriteDays().add(isoDate);
        return true;
    }

    // --- Per-day icons ---
    public Map<String, String> getCalendarDayIcons() {
        if (calendarDayIcons == null) calendarDayIcons = new java.util.HashMap<>();
        return calendarDayIcons;
    }
    public Map<String, String> getCalendarDayIconColors() {
        if (calendarDayIconColors == null) calendarDayIconColors = new java.util.HashMap<>();
        return calendarDayIconColors;
    }
    public String getDayIcon(String isoDate) { return getCalendarDayIcons().get(isoDate); }
    public boolean hasDayIcon(String isoDate) {
        String s = getCalendarDayIcons().get(isoDate);
        return s != null && !s.isBlank() && !s.equals("None");
    }
    public String getDayIconColor(String isoDate) {
        String c = getCalendarDayIconColors().get(isoDate);
        return c == null ? "#FFFFFF" : c;
    }
    public void setDayIcon(String isoDate, String symbol, String color) {
        if (symbol == null || symbol.isBlank() || symbol.equals("None")) {
            getCalendarDayIcons().remove(isoDate);
            getCalendarDayIconColors().remove(isoDate);
        } else {
            getCalendarDayIcons().put(isoDate, symbol);
            getCalendarDayIconColors().put(isoDate, color == null ? "#FFFFFF" : color);
        }
    }

    public boolean isShowPriority() { return showPriority; }
    public void setShowPriority(boolean showPriority) { this.showPriority = showPriority; }

    public boolean isTrackTime() { return trackTime; }
    public void setTrackTime(boolean trackTime) { this.trackTime = trackTime; }

    public boolean isShowTaskType() { return showTaskType; }
    public void setShowTaskType(boolean showTaskType) { this.showTaskType = showTaskType; }

    public boolean isAllowFavorite() { return allowFavorite; }
    public void setAllowFavorite(boolean allowFavorite) { this.allowFavorite = allowFavorite; }

    public boolean isShowAnalytics() { return showAnalytics; }
    public void setShowAnalytics(boolean showAnalytics) { this.showAnalytics = showAnalytics; }

    public boolean isEnableIcons() { return enableIcons; }
    public void setEnableIcons(boolean enableIcons) { this.enableIcons = enableIcons; }

    public boolean isEnableZenMode() { return enableZenMode; }
    public void setEnableZenMode(boolean enableZenMode) { this.enableZenMode = enableZenMode; }

    public boolean isEnableStatsSystem() { return enableStatsSystem; }
    public void setEnableStatsSystem(boolean enableStatsSystem) { this.enableStatsSystem = enableStatsSystem; }

    public boolean isEnableLinkCards() { return enableLinkCards; }
    public void setEnableLinkCards(boolean enableLinkCards) { this.enableLinkCards = enableLinkCards; }

    public boolean isNotesPage() { return isNotesPage; }
    public void setNotesPage(boolean notesPage) { this.isNotesPage = notesPage; }

    public boolean isEnableOptionalTasks() { return enableOptionalTasks; }
    public void setEnableOptionalTasks(boolean enableOptionalTasks) { this.enableOptionalTasks = enableOptionalTasks; }

    public boolean isEnableTaskStyling() { return enableTaskStyling; }
    public void setEnableTaskStyling(boolean enableTaskStyling) { this.enableTaskStyling = enableTaskStyling; }

    public boolean isEnableTimedTasks() { return enableTimedTasks; }
    public void setEnableTimedTasks(boolean enableTimedTasks) { this.enableTimedTasks = enableTimedTasks; }

    public boolean isAllowRepeatingTasks() { return allowRepeatingTasks; }
    public void setAllowRepeatingTasks(boolean allowRepeatingTasks) { this.allowRepeatingTasks = allowRepeatingTasks; }

    // --- NEW: Getter & Setter for Lock Completed Tasks ---
    public boolean isLockCompletedTasks() { return lockCompletedTasks; }
    public void setLockCompletedTasks(boolean lockCompletedTasks) { this.lockCompletedTasks = lockCompletedTasks; }

    public int getPreventEditingHours() { return preventEditingHours; }
    public void setPreventEditingHours(int preventEditingHours) { this.preventEditingHours = preventEditingHours; }

    public boolean isEnableCategories() { return enableCategories; }
    public void setEnableCategories(boolean enableCategories) { this.enableCategories = enableCategories; }

    public List<CategoryStyle> getCategoryStyles() {
        if (categoryStyles == null) categoryStyles = new ArrayList<>();
        return categoryStyles;
    }

    /** @return the {@link CategoryStyle} for {@code name}, or null if none has been customized. */
    public CategoryStyle findCategoryStyle(String name) {
        if (name == null) return null;
        for (CategoryStyle s : getCategoryStyles()) {
            if (name.equals(s.getName())) return s;
        }
        return null;
    }

    /**
     * Returns the existing style for {@code name}, or creates+stores a blank one if missing.
     * Used by the customization dialog so callers can mutate the returned object directly.
     */
    public CategoryStyle upsertCategoryStyle(String name) {
        CategoryStyle existing = findCategoryStyle(name);
        if (existing != null) return existing;
        CategoryStyle created = new CategoryStyle(name);
        getCategoryStyles().add(created);
        return created;
    }

    /** Drops the style for {@code name} if present (used by the "Reset to Default" path). */
    public void removeCategoryStyle(String name) {
        if (name == null) return;
        getCategoryStyles().removeIf(s -> name.equals(s.getName()));
    }

    /**
     * Renames a category in this section: rewrites the {@code categoryName} of every task in
     * {@code tasks} that belongs to this section and matches {@code oldName}, migrates the
     * persisted collapse state, and re-keys the {@link CategoryStyle} entry. If a style already
     * exists under {@code newName} (i.e. the user is merging categories), the old style is dropped
     * and the existing one wins.
     *
     * @return true if anything changed
     */
    public boolean renameCategory(String oldName, String newName, List<TaskItem> tasks, AppStats appStats) {
        if (oldName == null || newName == null) return false;
        String trimmedNew = newName.trim();
        if (trimmedNew.isEmpty() || trimmedNew.equals(oldName)) return false;

        boolean changed = false;

        if (tasks != null) {
            for (TaskItem t : tasks) {
                if (id.equals(t.getSectionId()) && oldName.equals(t.getCategoryName())) {
                    t.setCategoryName(trimmedNew);
                    changed = true;
                }
            }
        }

        if (appStats != null) {
            boolean wasCollapsed = appStats.isCategoryCollapsed(id, oldName);
            if (wasCollapsed || appStats.getCollapsedCategories().containsKey(id)) {
                appStats.setCategoryCollapsed(id, oldName, false);
                if (wasCollapsed) appStats.setCategoryCollapsed(id, trimmedNew, true);
            }
        }

        CategoryStyle oldStyle = findCategoryStyle(oldName);
        if (oldStyle != null) {
            CategoryStyle clash = findCategoryStyle(trimmedNew);
            if (clash != null) {
                // Merge: the destination's style wins; we just drop the old key.
                getCategoryStyles().remove(oldStyle);
            } else {
                oldStyle.setName(trimmedNew);
            }
            changed = true;
        }

        return changed;
    }

    public List<DailyTemplate> getAutoAddTemplates() {
        if (autoAddTemplates == null) {
            autoAddTemplates = new ArrayList<>();
        }
        return autoAddTemplates;
    }

    public void setAutoAddTemplates(List<DailyTemplate> autoAddTemplates) {
        this.autoAddTemplates = autoAddTemplates;
    }

    @Override
    public String toString() { return name; }
}