package com.raeden.ors_to_do.dependencies.models;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.*;

public class TaskItem implements Serializable {
    private static final long serialVersionUID = 1L;

    private String id;
    private String textContent;
    private CustomPriority priority;

    private boolean isFinished;
    private boolean isArchived = false;
    private boolean isFavorite = false;
    private boolean isOptional = false;
    private boolean isPinned = false;

    private LocalDateTime dateCreated;
    private LocalDateTime dateCompleted;
    private LocalDateTime startDate;
    private LocalDateTime deadline;

    private String colorHex;
    private String customOutlineColor = null;
    private String customSideboxColor = null;
    private String prefix;
    private String prefixColor;
    private String iconSymbol;
    private String iconColor;
    private String taskType = "";

    private OriginModule originModule;
    private String sectionId;

    private boolean isCounterMode = false;
    private int currentCount = 0;
    private int maxCount = 0;
    private boolean isPermaLock = false;

    private boolean isRepeatingMode = false;
    private int repetitionCount = 0;

    private int timeSpentSeconds = 0;
    private int targetTimeMinutes = 0;

    private int rewardPoints = 0;
    private int penaltyPoints = 0;
    private int costPoints = 0;
    private boolean pointsClaimed = false;
    private boolean penaltyApplied = false;

    private boolean isLinkCard;
    private String linkActionPath;

    // --- Description card: shows a Copy button instead of a checkbox; clicking copies descriptionContent. ---
    private boolean isDescriptionCard = false;
    private String descriptionContent = "";

    // --- Notes card: an individual card that behaves like a note (pin instead of checkbox, never
    // completes), available on non-Notes sections that have "Allow Notes" turned on. ---
    private boolean isNoteCard = false;

    private boolean isChallengeCard = false;
    private int perkLevel = 0;
    private int weeksMaintained = 0;
    private String perkDescription = "";
    private LocalDateTime perkUnlockedDate;
    private LocalDateTime perkLostDate;

    private boolean isExpanded = false;
    private boolean statsExpanded = false;

    private boolean notified24h = false;
    private boolean notified12h = false;
    private boolean notified4h = false;
    private boolean notified2h = false;

    /**
     * Free-text category bucket. When the owning section has {@code enableCategories} on, the
     * renderer groups cards by this string and shows a collapsible header per group. Null/blank
     * counts as "Uncategorized".
     */
    private String categoryName;

    public String getCategoryName() { return categoryName; }
    public void setCategoryName(String categoryName) { this.categoryName = categoryName; }

    private List<SubTask> subTasks = new ArrayList<>();
    private List<String> links = new ArrayList<>();
    private List<TaskLink> taskLinks = new ArrayList<>();
    private List<String> dependsOnTaskIds = new ArrayList<>();

    // --- NEW: Debuffs inflicted upon completion ---
    private List<String> inflictedDebuffIds = new ArrayList<>();

    private Map<String, Integer> statRewards = new HashMap<>();
    private Map<String, Integer> statCapRewards = new HashMap<>();
    private Map<String, Integer> statCosts = new HashMap<>();
    private Map<String, Integer> statPenalties = new HashMap<>();
    private Map<String, Integer> statRequirements = new HashMap<>();

    public TaskItem(String textContent, CustomPriority priority, OriginModule legacyModule) {
        this.id = UUID.randomUUID().toString();
        this.textContent = textContent;
        this.priority = priority;
        this.originModule = legacyModule;
        this.sectionId = legacyModule.name();
        this.isFinished = false;
        this.dateCreated = LocalDateTime.now();
    }

    public TaskItem(String textContent, CustomPriority priority, String sectionId) {
        this.id = UUID.randomUUID().toString();
        this.textContent = textContent;
        this.priority = priority;
        this.sectionId = sectionId;
        this.isFinished = false;
        this.dateCreated = LocalDateTime.now();
    }

    public String getId() { return id; }

    public String getTextContent() { return textContent; }
    public void setTextContent(String textContent) { this.textContent = textContent; }

    public CustomPriority getPriority() { return priority; }
    public void setPriority(CustomPriority priority) { this.priority = priority; }

    public boolean isFinished() { return isFinished; }
    public void setFinished(boolean finished) {
        this.isFinished = finished;
        this.dateCompleted = finished ? LocalDateTime.now() : null;
    }

    public boolean isArchived() { return isArchived; }
    public void setArchived(boolean archived) { this.isArchived = archived; }

    public boolean isFavorite() { return isFavorite; }
    public void setFavorite(boolean favorite) { this.isFavorite = favorite; }

    public boolean isOptional() { return isOptional; }
    public void setOptional(boolean optional) { this.isOptional = optional; }

    public boolean isPinned() { return isPinned; }
    public void setPinned(boolean pinned) { this.isPinned = pinned; }

    public LocalDateTime getDateCreated() { return dateCreated; }

    public LocalDateTime getDateCompleted() { return dateCompleted; }

    public LocalDateTime getStartDate() { return startDate; }
    public void setStartDate(LocalDateTime startDate) { this.startDate = startDate; }

    public LocalDateTime getDeadline() { return deadline; }
    public void setDeadline(LocalDateTime deadline) {
        // Only re-arm the notification flags if the deadline actually moved. Without this guard,
        // any edit-and-save that round-trips through the deadline field would re-fire every
        // notification threshold the next time the notification scan runs.
        if (java.util.Objects.equals(this.deadline, deadline)) {
            this.deadline = deadline;
            return;
        }
        this.deadline = deadline;
        this.notified24h = false;
        this.notified12h = false;
        this.notified4h = false;
        this.notified2h = false;
    }

    public String getColorHex() { return colorHex; }
    public void setColorHex(String colorHex) { this.colorHex = colorHex; }

    public String getCustomOutlineColor() { return customOutlineColor; }
    public void setCustomOutlineColor(String color) { this.customOutlineColor = color; }

    public String getCustomSideboxColor() { return customSideboxColor; }
    public void setCustomSideboxColor(String color) { this.customSideboxColor = color; }

    public String getPrefix() { return prefix; }
    public void setPrefix(String prefix) { this.prefix = prefix; }

    public String getPrefixColor() { return prefixColor; }
    public void setPrefixColor(String prefixColor) { this.prefixColor = prefixColor; }

    public String getIconSymbol() { return iconSymbol; }
    public void setIconSymbol(String iconSymbol) { this.iconSymbol = iconSymbol; }

    public String getIconColor() { return iconColor; }
    public void setIconColor(String iconColor) { this.iconColor = iconColor; }

    public String getTaskType() { return taskType; }
    public void setTaskType(String taskType) { this.taskType = taskType; }

    public String getSectionId() { return sectionId; }
    public void setSectionId(String sectionId) { this.sectionId = sectionId; }

    public OriginModule getLegacyOriginModule() { return originModule; }
    public OriginModule getOriginModule() {
        if (originModule != null) return originModule;
        try { return OriginModule.valueOf(sectionId); } catch (Exception e) { return null; }
    }

    public boolean isCounterMode() { return isCounterMode; }
    public void setCounterMode(boolean counterMode) { this.isCounterMode = counterMode; }

    public int getCurrentCount() { return currentCount; }
    public void setCurrentCount(int currentCount) { this.currentCount = currentCount; }

    public int getMaxCount() { return maxCount; }
    public void setMaxCount(int maxCount) { this.maxCount = maxCount; }

    public boolean isPermaLock() { return isPermaLock; }
    public void setPermaLock(boolean permaLock) { this.isPermaLock = permaLock; }

    public boolean isRepeatingMode() { return isRepeatingMode; }
    public void setRepeatingMode(boolean repeatingMode) { this.isRepeatingMode = repeatingMode; }

    public int getRepetitionCount() { return repetitionCount; }
    public void setRepetitionCount(int repetitionCount) { this.repetitionCount = repetitionCount; }

    public int getTimeSpentSeconds() { return timeSpentSeconds; }
    public void setTimeSpentSeconds(int timeSpentSeconds) { this.timeSpentSeconds = timeSpentSeconds; }
    public void addTimeSpent(int seconds) { this.timeSpentSeconds += seconds; }

    public int getTargetTimeMinutes() { return targetTimeMinutes; }
    public void setTargetTimeMinutes(int targetTimeMinutes) { this.targetTimeMinutes = targetTimeMinutes; }

    public int getRewardPoints() { return rewardPoints; }
    public void setRewardPoints(int rewardPoints) { this.rewardPoints = rewardPoints; }

    public int getPenaltyPoints() { return penaltyPoints; }
    public void setPenaltyPoints(int penaltyPoints) { this.penaltyPoints = penaltyPoints; }

    public int getCostPoints() { return costPoints; }
    public void setCostPoints(int costPoints) { this.costPoints = costPoints; }

    public boolean isPointsClaimed() { return pointsClaimed; }
    public void setPointsClaimed(boolean pointsClaimed) { this.pointsClaimed = pointsClaimed; }

    public boolean isPenaltyApplied() { return penaltyApplied; }
    public void setPenaltyApplied(boolean penaltyApplied) { this.penaltyApplied = penaltyApplied; }

    public boolean isLinkCard() { return isLinkCard; }
    public void setLinkCard(boolean linkCard) { this.isLinkCard = linkCard; }

    public String getLinkActionPath() { return linkActionPath; }
    public void setLinkActionPath(String linkActionPath) { this.linkActionPath = linkActionPath; }

    public boolean isDescriptionCard() { return isDescriptionCard; }
    public void setDescriptionCard(boolean descriptionCard) { this.isDescriptionCard = descriptionCard; }

    public boolean isNoteCard() { return isNoteCard; }
    public void setNoteCard(boolean noteCard) { this.isNoteCard = noteCard; }

    public String getDescriptionContent() { return descriptionContent == null ? "" : descriptionContent; }
    public void setDescriptionContent(String descriptionContent) { this.descriptionContent = descriptionContent; }

    public boolean isChallengeCard() { return isChallengeCard; }
    public void setChallengeCard(boolean challengeCard) { this.isChallengeCard = challengeCard; }

    public int getPerkLevel() { return perkLevel; }
    public void setPerkLevel(int perkLevel) { this.perkLevel = perkLevel; }

    public int getWeeksMaintained() { return weeksMaintained; }
    public void setWeeksMaintained(int weeksMaintained) { this.weeksMaintained = weeksMaintained; }

    public String getPerkDescription() { return perkDescription; }
    public void setPerkDescription(String perkDescription) { this.perkDescription = perkDescription; }

    public LocalDateTime getPerkUnlockedDate() { return perkUnlockedDate; }
    public void setPerkUnlockedDate(LocalDateTime perkUnlockedDate) { this.perkUnlockedDate = perkUnlockedDate; }

    public LocalDateTime getPerkLostDate() { return perkLostDate; }
    public void setPerkLostDate(LocalDateTime perkLostDate) { this.perkLostDate = perkLostDate; }

    public boolean isExpanded() { return isExpanded; }
    public void setExpanded(boolean expanded) { this.isExpanded = expanded; }

    public boolean isStatsExpanded() { return statsExpanded; }
    public void setStatsExpanded(boolean statsExpanded) { this.statsExpanded = statsExpanded; }

    public boolean isNotified24h() { return notified24h; }
    public void setNotified24h(boolean v) { this.notified24h = v; }

    public boolean isNotified12h() { return notified12h; }
    public void setNotified12h(boolean v) { this.notified12h = v; }

    public boolean isNotified4h() { return notified4h; }
    public void setNotified4h(boolean v) { this.notified4h = v; }

    public boolean isNotified2h() { return notified2h; }
    public void setNotified2h(boolean v) { this.notified2h = v; }

    public List<SubTask> getSubTasks() {
        if (subTasks == null) subTasks = new ArrayList<>();
        return subTasks;
    }

    public List<TaskLink> getTaskLinks() {
        if (taskLinks == null) taskLinks = new ArrayList<>();
        if (links != null && !links.isEmpty()) {
            for (String oldLink : links) {
                taskLinks.add(new TaskLink(oldLink, oldLink));
            }
            links.clear();
        }
        return taskLinks;
    }

    public List<String> getDependsOnTaskIds() {
        if (dependsOnTaskIds == null) dependsOnTaskIds = new ArrayList<>();
        return dependsOnTaskIds;
    }
    public void setDependsOnTaskIds(List<String> dependsOnTaskIds) { this.dependsOnTaskIds = dependsOnTaskIds; }

    public List<String> getInflictedDebuffIds() {
        if (inflictedDebuffIds == null) inflictedDebuffIds = new ArrayList<>();
        return inflictedDebuffIds;
    }
    public void setInflictedDebuffIds(List<String> inflictedDebuffIds) { this.inflictedDebuffIds = inflictedDebuffIds; }

    public Map<String, Integer> getStatRewards() {
        if (statRewards == null) statRewards = new HashMap<>();
        return statRewards;
    }
    public void setStatRewards(Map<String, Integer> statRewards) { this.statRewards = statRewards; }

    public Map<String, Integer> getStatCapRewards() {
        if (statCapRewards == null) statCapRewards = new HashMap<>();
        return statCapRewards;
    }
    public void setStatCapRewards(Map<String, Integer> statCapRewards) { this.statCapRewards = statCapRewards; }

    public Map<String, Integer> getStatCosts() {
        if (statCosts == null) statCosts = new HashMap<>();
        return statCosts;
    }
    public void setStatCosts(Map<String, Integer> statCosts) { this.statCosts = statCosts; }

    public Map<String, Integer> getStatPenalties() {
        if (statPenalties == null) statPenalties = new HashMap<>();
        return statPenalties;
    }
    public void setStatPenalties(Map<String, Integer> statPenalties) { this.statPenalties = statPenalties; }

    public Map<String, Integer> getStatRequirements() {
        if (statRequirements == null) statRequirements = new HashMap<>();
        return statRequirements;
    }
    public void setStatRequirements(Map<String, Integer> statRequirements) { this.statRequirements = statRequirements; }
}