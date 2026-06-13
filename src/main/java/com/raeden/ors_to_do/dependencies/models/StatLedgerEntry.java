package com.raeden.ors_to_do.dependencies.models;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * One immutable record in the stat ledger: a single point/EXP change to a Custom Stat (or to the
 * Global Score) at a moment in time, together with where it came from.
 *
 * <p>Unlike the old {@code StatHistoryDialog}, which re-derived "history" by scanning task cards,
 * the ledger is an append-only log written at the moment a stat actually changes (task completion,
 * miss penalty, reward purchase, calendar mark, focus session, …). The stat name is snapshotted on
 * write so the history still reads correctly after a stat is renamed or deleted.</p>
 */
public class StatLedgerEntry implements Serializable {
    private static final long serialVersionUID = 1L;

    /** Sentinel statName used for changes to the Global Score (not a Custom Stat). */
    public static final String GLOBAL_SCORE = "Global Score";

    private LocalDateTime date;
    private String statName;   // display name snapshot; GLOBAL_SCORE for global points
    private int amount;        // signed: positive = gain, negative = loss
    private String unit;       // "XP", "pts", or "Max Cap"
    private String source;     // human-readable origin, e.g. the task text or "Calendar: Gym"

    public StatLedgerEntry() { }

    public StatLedgerEntry(LocalDateTime date, String statName, int amount, String unit, String source) {
        this.date = date;
        this.statName = statName;
        this.amount = amount;
        this.unit = unit;
        this.source = source;
    }

    public LocalDateTime getDate() { return date; }
    public void setDate(LocalDateTime date) { this.date = date; }

    public String getStatName() { return statName == null ? "" : statName; }
    public void setStatName(String statName) { this.statName = statName; }

    public int getAmount() { return amount; }
    public void setAmount(int amount) { this.amount = amount; }

    public String getUnit() { return unit == null ? "" : unit; }
    public void setUnit(String unit) { this.unit = unit; }

    public String getSource() { return source == null ? "" : source; }
    public void setSource(String source) { this.source = source; }

    public boolean isGlobalScore() { return GLOBAL_SCORE.equals(statName); }
    public boolean isGain() { return amount >= 0; }
}
