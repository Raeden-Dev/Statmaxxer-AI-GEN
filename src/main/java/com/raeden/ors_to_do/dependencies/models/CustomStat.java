package com.raeden.ors_to_do.dependencies.models;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class CustomStat implements Serializable {
    private static final long serialVersionUID = 1L;

    private String id;
    private String name;
    private String iconSymbol;
    private String backgroundColor;
    private String textColor;

    private int currentAmount = 0;
    private String description = "";

    /** Baseline value this stat is reset to via the "Base Stats" dialog. Defaults to 0. */
    private int baseValue = 0;

    // --- FIXED: Increased default max cap to 10 Million for money tracking ---
    private int maxCap = 10000000;

    private int atrophyDays = 0;
    private int lifetimeEarned = 0;
    private int lifetimeLost = 0;
    private int maxLevelReached = 0;

    // --- EXP leveling (opt-in per stat) -----------------------------------------------------
    // When enabled, rewards/costs/penalties feed an EXP pool instead of changing the point value
    // directly. Crossing {@code expPerLevel} upward grants a stat point (carrying the remainder);
    // dropping below 0 removes a point (carrying the deficit down). At max cap the bar stays full;
    // at 0 points EXP can't go negative.
    private boolean useExp = false;
    private int expPerLevel = 100;
    private int currentExp = 0;
    /** Per-card EXP-bar visibility override on the Stat page. null = follow the global default. */
    private Boolean expBarExpanded = null;

    private List<StatThreshold> thresholds = new ArrayList<>();

    public CustomStat() {
        this.id = UUID.randomUUID().toString();
    }

    public CustomStat(String name, String iconSymbol, String backgroundColor, String textColor) {
        this.id = UUID.randomUUID().toString();
        this.name = name;
        this.iconSymbol = iconSymbol;
        this.backgroundColor = backgroundColor;
        this.textColor = textColor;
    }

    public int getEffectiveMaxCap(java.util.List<Debuff> activeDebuffs) {
        int cap = maxCap;
        if (activeDebuffs != null) {
            for (Debuff d : activeDebuffs) {
                if (d.getStatCapReductions().containsKey(id)) {
                    int reduction = d.getStatCapReductions().get(id);
                    if (d.isAllowStacking() && d.getStatCapReductionStackIncreasers().containsKey(id)) {
                        reduction += d.getStatCapReductionStackIncreasers().get(id) * (d.getCurrentStacks() - 1);
                    }
                    cap -= reduction;
                }
            }
        }
        return Math.max(1, cap);
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getIconSymbol() { return iconSymbol; }
    public void setIconSymbol(String iconSymbol) { this.iconSymbol = iconSymbol; }
    public String getBackgroundColor() { return backgroundColor; }
    public void setBackgroundColor(String backgroundColor) { this.backgroundColor = backgroundColor; }
    public String getTextColor() { return textColor; }
    public void setTextColor(String textColor) { this.textColor = textColor; }
    public int getCurrentAmount() { return currentAmount; }
    public void setCurrentAmount(int currentAmount) { this.currentAmount = currentAmount; }
    public int getBaseValue() { return baseValue; }
    public void setBaseValue(int baseValue) { this.baseValue = baseValue; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public int getMaxCap() { return maxCap; }
    public void setMaxCap(int maxCap) { this.maxCap = maxCap; }
    public int getAtrophyDays() { return atrophyDays; }
    public void setAtrophyDays(int atrophyDays) { this.atrophyDays = atrophyDays; }
    public int getLifetimeEarned() { return lifetimeEarned; }
    public void setLifetimeEarned(int lifetimeEarned) { this.lifetimeEarned = lifetimeEarned; }
    public int getLifetimeLost() { return lifetimeLost; }
    public void setLifetimeLost(int lifetimeLost) { this.lifetimeLost = lifetimeLost; }
    public int getMaxLevelReached() { return maxLevelReached; }
    public void setMaxLevelReached(int maxLevelReached) { this.maxLevelReached = maxLevelReached; }

    public List<StatThreshold> getThresholds() {
        if (thresholds == null) thresholds = new ArrayList<>();
        return thresholds;
    }
    public void setThresholds(List<StatThreshold> thresholds) { this.thresholds = thresholds; }

    // --- EXP leveling accessors -------------------------------------------------------------
    public boolean isUseExp() { return useExp; }
    public void setUseExp(boolean useExp) { this.useExp = useExp; }
    public int getExpPerLevel() { return expPerLevel <= 0 ? 100 : expPerLevel; }
    public void setExpPerLevel(int expPerLevel) { this.expPerLevel = expPerLevel; }
    public int getCurrentExp() { return currentExp; }
    public void setCurrentExp(int currentExp) { this.currentExp = currentExp; }

    public Boolean getExpBarExpanded() { return expBarExpanded; }
    public void setExpBarExpanded(Boolean expBarExpanded) { this.expBarExpanded = expBarExpanded; }
    /** Effective EXP-bar visibility given the global default. */
    public boolean isExpBarVisible(boolean globalDefault) {
        return expBarExpanded != null ? expBarExpanded : globalDefault;
    }
    /** Flips this stat's bar relative to its current effective visibility. */
    public void toggleExpBar(boolean globalDefault) {
        expBarExpanded = !isExpBarVisible(globalDefault);
    }

    /**
     * Applies an EXP {@code delta}, leveling the point value ({@link #currentAmount}) up or down
     * with carry-over across multiple levels. Respects bounds: at {@code effectiveCap} the bar
     * stays full and surplus is discarded; at 0 points EXP is floored at 0.
     *
     * @param effectiveCap the debuff-adjusted point ceiling (0 = infinite)
     * @return the net change in stat points
     */
    public int addExp(int delta, int effectiveCap) {
        int per = getExpPerLevel();
        int startAmount = currentAmount;
        currentExp += delta;

        while (currentExp >= per) {
            if (effectiveCap > 0 && currentAmount >= effectiveCap) {
                currentExp = per; // capped: bar stays full
                break;
            }
            currentExp -= per;
            currentAmount++;
        }
        while (currentExp < 0) {
            if (currentAmount <= 0) {
                currentExp = 0; // floored: no negative EXP at zero points
                break;
            }
            currentAmount--;
            currentExp += per;
        }

        if (effectiveCap > 0 && currentAmount > effectiveCap) currentAmount = effectiveCap;
        if (currentAmount < 0) currentAmount = 0;
        if (currentAmount > maxLevelReached) maxLevelReached = currentAmount;
        return currentAmount - startAmount;
    }

    /**
     * Adds {@code amount} to this stat — through the EXP pool when EXP leveling is on, otherwise
     * directly to the point value (clamped to {@code effectiveCap}).
     *
     * @return the amount actually credited. For a non-EXP stat this is the real point increase
     *         (0 when already at the cap), so callers can track "lifetime earned" accurately
     *         instead of counting rewards that were discarded at the cap.
     */
    public int gain(int amount, int effectiveCap) {
        if (amount <= 0) return 0;
        if (useExp) {
            addExp(amount, effectiveCap);
            return amount;
        }
        int before = currentAmount;
        int n = currentAmount + amount;
        if (effectiveCap > 0 && n > effectiveCap) n = effectiveCap;
        currentAmount = n;
        if (currentAmount > maxLevelReached) maxLevelReached = currentAmount;
        return currentAmount - before;
    }

    /**
     * Subtracts {@code amount} — through EXP when enabled, otherwise directly (floored at 0).
     *
     * @return the amount actually removed (for a non-EXP stat, capped by the floor at 0).
     */
    public int drain(int amount, int effectiveCap) {
        if (amount <= 0) return 0;
        if (useExp) {
            addExp(-amount, effectiveCap);
            return amount;
        }
        int before = currentAmount;
        currentAmount = Math.max(0, currentAmount - amount);
        return before - currentAmount;
    }
}