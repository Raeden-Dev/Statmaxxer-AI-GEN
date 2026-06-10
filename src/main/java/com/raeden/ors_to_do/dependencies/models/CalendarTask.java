package com.raeden.ors_to_do.dependencies.models;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * A single trackable activity on a {@link SectionConfig} that is in Calendar Page mode (e.g.
 * "Gym", "8 Hour Work"). Each task has a colour (and optional icon) used to paint/mark the days it
 * was completed on.
 *
 * <p>When the owning section has reward granting enabled, completing the task on a day can award
 * XP to one or more Custom Stats, award global score points, inflict debuffs, and "hook" other
 * cards (completing normal cards, incrementing counter/repeating cards).</p>
 */
public class CalendarTask implements Serializable {
    private static final long serialVersionUID = 1L;

    private String id;
    private String name;
    private String colorHex = "#569CD6";

    private String iconSymbol = "None";
    private String iconColor = "#FFFFFF";

    // --- Legacy single-stat fields (pre-expansion). Migrated into statRewards on first access. ---
    private String statId;
    private int xpPerCompletion = 0;

    // --- Rewards applied on completion (mirroring normal task cards) ---
    private Map<String, Integer> statRewards = new HashMap<>();    // statId -> XP
    private Map<String, Integer> statCapRewards = new HashMap<>(); // statId -> Max Cap increase
    private int rewardPoints = 0;                                  // global score
    private List<String> inflictedDebuffIds = new ArrayList<>();
    private List<String> hookedTaskIds = new ArrayList<>();      // cards to complete / increment

    public CalendarTask(String name) {
        this.id = UUID.randomUUID().toString();
        this.name = name;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getColorHex() { return colorHex == null ? "#569CD6" : colorHex; }
    public void setColorHex(String colorHex) { this.colorHex = colorHex; }

    public String getIconSymbol() { return iconSymbol == null ? "None" : iconSymbol; }
    public void setIconSymbol(String iconSymbol) { this.iconSymbol = iconSymbol; }
    public boolean hasIcon() { return iconSymbol != null && !iconSymbol.equals("None") && !iconSymbol.isBlank(); }

    public String getIconColor() { return iconColor == null ? "#FFFFFF" : iconColor; }
    public void setIconColor(String iconColor) { this.iconColor = iconColor; }

    public Map<String, Integer> getStatRewards() {
        if (statRewards == null) statRewards = new HashMap<>();
        // One-time migration of the legacy single-stat configuration.
        if (statRewards.isEmpty() && statId != null && xpPerCompletion > 0) {
            statRewards.put(statId, xpPerCompletion);
            statId = null;
            xpPerCompletion = 0;
        }
        return statRewards;
    }
    public void setStatRewards(Map<String, Integer> statRewards) { this.statRewards = statRewards; }

    public Map<String, Integer> getStatCapRewards() {
        if (statCapRewards == null) statCapRewards = new HashMap<>();
        return statCapRewards;
    }
    public void setStatCapRewards(Map<String, Integer> statCapRewards) { this.statCapRewards = statCapRewards; }

    public int getRewardPoints() { return rewardPoints; }
    public void setRewardPoints(int rewardPoints) { this.rewardPoints = rewardPoints; }

    public List<String> getInflictedDebuffIds() {
        if (inflictedDebuffIds == null) inflictedDebuffIds = new ArrayList<>();
        return inflictedDebuffIds;
    }
    public void setInflictedDebuffIds(List<String> inflictedDebuffIds) { this.inflictedDebuffIds = inflictedDebuffIds; }

    public List<String> getHookedTaskIds() {
        if (hookedTaskIds == null) hookedTaskIds = new ArrayList<>();
        return hookedTaskIds;
    }
    public void setHookedTaskIds(List<String> hookedTaskIds) { this.hookedTaskIds = hookedTaskIds; }

    @Override
    public String toString() { return name; }
}
