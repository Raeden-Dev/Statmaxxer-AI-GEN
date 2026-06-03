package com.raeden.ors_to_do.i18n;

/**
 * All user-facing text (any sentence/label longer than a couple of words) lives here,
 * fetched through {@link LangManager}. Centralising the copy keeps messages consistent,
 * auditable and translation-ready instead of being scattered across the UI classes.
 *
 * <p>Placeholders use the {@code {0}}, {@code {1}}, ... convention and are filled in by
 * {@link LangManager#get(Lang, Object...)} (or the convenience {@link #get(Object...)}).</p>
 */
public enum Lang {

    // ---------------------------------------------------------------------
    // Shared requirement / dependency lines (Challenge & Perk cards)
    // ---------------------------------------------------------------------
    REQ_STAT_UNMET("❌ Requires {0} {1} (Current: {2})"),
    REQ_STAT_MET("✅ {0} {1}"),
    REQ_DEP_UNMET("❌ Requires: {0}"),
    REQ_DEP_PERK_UNMET("❌ Requires Perk: {0}"),
    REQ_DEP_COUNTER_UNMET("❌ Requires (counter at max): {0} ({1}/{2})"),
    DEP_HOOKED("✅ Hooked: {0}"),
    DEP_PERK_HOOKED("✅ Hooked Perk: {0} Unlocked"),
    REQ_LINE_BULLET("• Requires {0} {1}"),

    // ---------------------------------------------------------------------
    // Card status lines
    // ---------------------------------------------------------------------
    SETUP_PHASE_INACTIVE("⏳ Inactive (Setup Phase) - {0} mins left"),
    CONQUERED_ON("🏆 Conquered on: {0}"),
    FAILED_ON("💀 Failed on: {0}"),
    PERK_UNLOCKED_ON("📅 Unlocked: {0}"),
    PERK_LOST_ON("⚠️ Lost: {0}"),
    NO_DESCRIPTION("No description."),

    // ---------------------------------------------------------------------
    // Deadline / time labels
    // ---------------------------------------------------------------------
    EXPIRED("Expired!"),
    EXPIRES_IN_DAYS("Expires in {0}d {1}h"),
    EXPIRES_IN_HOURS("Expires in {0}h {1}m"),

    // ---------------------------------------------------------------------
    // Loot / reward chips
    // ---------------------------------------------------------------------
    LOOT_GLOBAL_PTS("+{0} Global Pts"),
    LOOT_STAT_XP("+{0} {1} XP"),
    LOOT_STAT_CAP("+{0} {1} Cap"),
    LOOT_TITLE("🎁 Rewards:"),
    REQUIREMENTS_TITLE("📋 Requirements:"),

    // ---------------------------------------------------------------------
    // Expand / collapse toggles
    // ---------------------------------------------------------------------
    TOGGLE_SHOW_REQS("▼ Show Requirements & Rewards"),
    TOGGLE_HIDE_REQS("▲ Hide Requirements & Rewards"),
    TOGGLE_SHOW_DETAILS("▼ Show Details"),
    TOGGLE_HIDE_DETAILS("▲ Hide Details"),

    // ---------------------------------------------------------------------
    // Challenge card buttons / actions
    // ---------------------------------------------------------------------
    BTN_CHALLENGE_DONE("Challenge Done"),
    BTN_CHALLENGE_FAIL("Fail Challenge"),
    LEVEL_LABEL("Level {0}"),

    // ---------------------------------------------------------------------
    // Edit-lock messaging
    // ---------------------------------------------------------------------
    EDIT_LOCKED_HEADER("Editing Locked"),
    EDIT_LOCKED_CHALLENGE_BODY("This challenge was created over {0} hour(s) ago and is locked from editing."),
    EDIT_LOCKED_TASK_BODY("This task was created over {0} hour(s) ago and is locked from editing."),
    EDIT_LOCKED_COMPLETED_BODY("This task has been completed and is locked from editing by the section settings."),
    EDIT_LOCKED_TOOLTIP("Editing Locked (Time expired)"),

    // ---------------------------------------------------------------------
    // Context-menu items
    // ---------------------------------------------------------------------
    MENU_EDIT_CHALLENGE("Edit Challenge"),
    MENU_EDIT_CHALLENGE_LOCKED("Edit Challenge (Locked)"),
    MENU_DELETE_CHALLENGE("Permanently Delete Challenge"),
    MENU_EDIT_PERK("Edit Perk"),
    MENU_DELETE_PERK("Permanently Delete Perk"),
    MENU_RESET_PROGRESS("Reset Repeating Progress"),

    // ---------------------------------------------------------------------
    // Confirmation dialogs
    // ---------------------------------------------------------------------
    CONFIRM_CHALLENGE_DONE_HEADER("Challenge Done"),
    CONFIRM_CHALLENGE_DONE_BODY("Are you sure you have completed this challenge?\n\n"
            + "This is permanent. You will gain the rewards, and this card will be locked forever."),
    CONFIRM_CHALLENGE_FAIL_HEADER("Fail Challenge"),
    CONFIRM_CHALLENGE_FAIL_BODY("Are you sure you want to mark this challenge as FAILED?\n\n"
            + "This is permanent. You will NOT gain any rewards, and this card will be locked forever."),
    CONFIRM_DELETE_CHALLENGE_HEADER("Delete Challenge"),
    CONFIRM_DELETE_CHALLENGE_BODY("Are you sure you want to permanently delete '{0}'?\n\n"
            + "This cannot be undone and rewards will not be revoked."),
    CONFIRM_DELETE_PERK_HEADER("Delete Perk"),
    CONFIRM_DELETE_PERK_BODY("Are you sure you want to permanently delete '{0}'?\n\n"
            + "This cannot be undone and stats will not be revoked."),

    // ---------------------------------------------------------------------
    // Challenge / Perk configuration dialog
    // ---------------------------------------------------------------------
    DLG_CONFIGURE_CHALLENGE_TITLE("Configure Challenge"),
    DLG_CONFIGURE_PERK_TITLE("Configure Perk: {0}"),
    FIELD_CHALLENGE_NAME_PROMPT("Challenge Name"),
    FIELD_CHALLENGE_DESC_PROMPT("Challenge Lore / Rules..."),
    FIELD_PERK_NAME_PROMPT("Perk Name"),
    FIELD_PERK_DESC_PROMPT("What does this perk do? (e.g. Grants access to special tasks, buffs a stat, etc.)"),
    LBL_CHALLENGE_NAME("Challenge Name:"),
    LBL_DESC_AND_RULES("Description & Rules:"),
    LBL_PERK_NAME("Perk Name:"),
    LBL_PERK_EFFECT("Perk Effect / Lore Description:"),
    LBL_CHALLENGE_TIMELINE("Challenge Timeline:"),
    LBL_DEADLINE_DATE("Deadline Date:"),
    LBL_EXACT_TIME("Exact Time:"),
    LBL_TIME_PROMPT("HH:mm (24h)"),
    LBL_APPEARANCE_STYLING("Appearance & Styling:"),
    LBL_PERK_APPEARANCE_STYLING("Perk Appearance & Styling:"),
    LBL_ICON_AND_COLOR("Icon & Color:"),
    LBL_BACKGROUND_COLOR("Background Color:"),
    LBL_OUTLINE_COLOR("Outline Color:"),
    LBL_OUTLINE_GLOW_COLOR("Outline & Glow Color:"),
    BTN_RANDOMIZE_STYLE("🎲 Randomize Style"),
    LBL_CHALLENGE_LOOT("Challenge Loot (Rewards upon completion):"),
    LBL_GLOBAL_POINTS("Global Points:"),
    LBL_COL_STAT("Stat"),
    LBL_COL_XP_REWARD("+ XP Reward"),
    LBL_COL_MAX_CAP("+ Max Cap"),
    LBL_UNLOCK_REQUIREMENTS("Unlock Requirements (Stats needed to attempt):"),
    LBL_HOOK_STAT_REQUIREMENTS("Hook Stat Requirements (RPG Thresholds):"),
    LBL_HOOK_TASKS("Hook Tasks / Challenges:"),
    BTN_ADD_HOOK("Add Hook"),
    BTN_SELECT_PARENTS("Select Parent Requirements..."),
    MENU_OTHER_TASKS("Other Tasks"),
    HOOKED_REQUIREMENTS_COUNT("Hooked Requirements ({0})"),
    NO_OTHER_TASKS("No other tasks available."),

    // ---------------------------------------------------------------------
    // Misc / generic
    // ---------------------------------------------------------------------
    NOTIFY_DEBUFF_CLEANSED_TITLE("Debuff Cleansed!"),
    NOTIFY_DEBUFF_CLEANSED_BODY("You have overcome: {0}"),
    ERR_OPEN_PATH("Failed to open path: \n{0}"),
    ERR_EXECUTION_HEADER("Execution Error");

    private final String template;

    Lang(String template) {
        this.template = template;
    }

    /** The raw message template, including any {@code {n}} placeholders. */
    public String template() {
        return template;
    }

    /** Convenience for {@link LangManager#get(Lang, Object...)}. */
    public String get(Object... args) {
        return LangManager.get(this, args);
    }
}
