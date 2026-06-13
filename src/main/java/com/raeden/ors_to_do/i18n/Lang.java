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
    COUNTER_TARGET_REACHED("Target reached ({0}/{0})"),
    CATEGORY_UNCATEGORIZED("Uncategorized"),
    CATEGORY_LABEL("Category:"),
    CATEGORY_PROMPT("Optional category name"),
    CATEGORY_ENABLE_TOGGLE("Enable Categories"),
    CATEGORY_ENABLE_DESC("Group cards under collapsible category headers. Set a card's category in its edit dialog."),
    CATEGORY_HEADER_COUNT("{0} ({1})"),
    CATEGORY_CUSTOMIZE_MENU("Customize Category..."),
    CATEGORY_STYLE_DIALOG_TITLE("Customize Category: {0}"),
    CATEGORY_STYLE_BACKGROUND("Background Color:"),
    CATEGORY_STYLE_BORDER("Border Color:"),
    CATEGORY_STYLE_TEXT("Text Color:"),
    CATEGORY_STYLE_ICON("Icon & Color:"),
    CATEGORY_STYLE_RESET("Reset to Default"),
    CATEGORY_EDIT_TOOLTIP("Customize this category's appearance"),
    CATEGORY_NAME_LABEL("Category Name:"),
    CATEGORY_NAME_UNCATEGORIZED_TOOLTIP("This is the built-in fallback bucket — its name can't be changed."),
    OPEN_DATA_FOLDER_BTN("📁 Open Data Folder"),
    OPEN_DATA_FOLDER_TOOLTIP("Open the folder where tasks.json, stats.json and backups are stored."),
    OPEN_DATA_FOLDER_ERROR_HEADER("Cannot Open Data Folder"),
    OPEN_DATA_FOLDER_ERROR_BODY("Could not open the folder:\n{0}"),

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
    // Empty-state messages (DynamicModule)
    // ---------------------------------------------------------------------
    EMPTY_NOTES("Add a note to your board!"),
    EMPTY_REWARDS("Add a reward to your shop!"),
    EMPTY_TASKS("Add a task to get started!"),
    EMPTY_PERKS("Type a perk name in the bar below and click 'Add' to create your first Skill Tree Perk!"),
    EMPTY_CHALLENGES("Type a challenge name below to create a new conquerable Challenge!"),
    DEBUFFS_TITLE("Active Debuffs"),
    NO_DEBUFFS("You are completely healthy."),
    NO_CUSTOM_STATS("No custom stats available. Go to Settings to create them."),

    // ---------------------------------------------------------------------
    // About / Help / Credits (TaskDialogs)
    // ---------------------------------------------------------------------
    ABOUT_CREDITS_TITLE("About & Credits ({0})"),
    APP_NAME("Task Tracker"),
    ABOUT_CREDITS_BODY("Developed for anyone who wants to keep track of everything in their life.\n\n"
            + "Credits @Sadman Sakib - One Raid Studio"),
    HELP_TITLE("Application Help Guide"),
    HELP_HEADER("How to use the application: ({0})"),

    // ---------------------------------------------------------------------
    // Calendar Page
    // ---------------------------------------------------------------------
    CAL_FILTER_SHOW_ALL("Show All"),
    CAL_FILTER_FAVORITES("★ Favorites"),
    CAL_TASK_LIST_TITLE("Task List"),
    CAL_JOURNAL_TITLE("Journal"),
    CAL_JOURNAL_DAY_TITLE("Journal — {0}"),
    CAL_BTN_ADD_TASK("＋ Add Task"),
    CAL_BTN_ADD_JOURNAL("＋ Add Journal Entry"),
    CAL_BTN_ADD_EVENT("＋ Add Event"),
    CAL_HINT_SELECT_TASK("Select a task card below first, then click a day to mark it."),
    CAL_HINT_TODAY_ONLY("This calendar only allows marking today. Enable \"Allow Calendar Manipulation\" in Edit Section to change other days."),
    CAL_HINT_MARKING_WITH("Marking with \"{0}\" — click a day above."),
    CAL_HINT_DOUBLE_CLICK_JOURNAL("Double-click a day to open its journal."),
    CAL_JOURNAL_CLICK_DAY_HINT("Click a day above to view, add, or edit its journal entries and events."),
    CAL_NO_ENTRIES_FOR_DAY("No entries for this day yet. Add a journal entry or an event."),
    CAL_MENU_VIEW_ADD_ENTRIES("View / Add Entries"),
    CAL_MENU_ADD_JOURNAL("Add Journal Entry…"),
    CAL_MENU_ADD_EVENT("Add Event…"),
    CAL_MENU_EDIT_JOURNAL("Edit Journal Entry"),
    CAL_MENU_CUSTOMIZE_DAY("Customize Day…"),
    CAL_MENU_FAVORITE_DAY("Favorite Day"),
    CAL_MENU_UNFAVORITE_DAY("Unfavorite Day"),
    CAL_MENU_CLEAR_MARKS("Clear Day Marks"),
    CAL_MENU_EDIT_TASK("Edit Task"),
    CAL_MENU_REMOVE_TASK("Permanently Remove Task"),
    CAL_TOOLTIP_CONFIGURE_TASK("Configure / edit this task"),
    CAL_TOOLTIP_CUSTOMIZE_EDIT("Customize / edit"),
    CAL_DLG_ADD_TASK("Add Calendar Task"),
    CAL_DLG_EDIT_TASK("Edit Calendar Task"),
    CAL_DLG_ADD_JOURNAL("Add Journal Entry"),
    CAL_DLG_EDIT_JOURNAL("Edit Journal Entry"),
    CAL_DLG_ADD_EVENT("Add Event"),
    CAL_DLG_EDIT_EVENT("Edit Event"),
    CAL_DLG_CUSTOMIZE_DAY("Customize Day — {0}"),
    CAL_PROMPT_TASK_NAME("e.g. Gym, 8 Hour Work"),
    CAL_PROMPT_SCORE("+ Global score points"),
    CAL_REWARDS_HEADER("Rewards on completion"),
    CAL_REWARDS_DISABLED_NOTE("Note: enable \"Grant rewards on completion\" in Edit Section for these to apply."),
    CAL_STAT_REWARDS_HEADER("Stat Rewards"),
    CAL_LBL_SCORE_REWARD("Score Reward:"),
    CAL_LBL_INFLICT_DEBUFFS("Inflict Debuffs:"),
    CAL_LBL_HOOK_CARDS("Hook Cards:"),
    CAL_HOOK_DESC("Completing this calendar task will complete hooked cards and +1 to hooked counter/repeating cards."),
    CAL_DEBUFF_SELECT("Select Debuffs to Inflict"),
    CAL_DEBUFF_COUNT("Debuffs to Inflict ({0})"),
    CAL_ENTRY_TAG_EVENT("event"),
    CAL_ENTRY_TAG_LOG("log"),
    CAL_MENU_EDIT_ENTRY("Edit Entry"),
    CAL_MENU_DELETE_ENTRY("Delete Entry"),
    CAL_EXPORT_DONE_BODY("Calendar analytics exported to Desktop:\n{0}"),
    EXPORT_SUCCESS_HEADER("Export Successful"),

    // ---------------------------------------------------------------------
    // Edit Section — calendar options
    // ---------------------------------------------------------------------
    SEC_CAL_OPTIONS_HEADER("Calendar Options:"),
    SEC_CAL_SEGMENTS("Show Segments (color bands on the day)"),
    SEC_CAL_DOTS("Show Dots (color dots under the date)"),
    SEC_CAL_MANIPULATION("Allow Calendar Manipulation (mark past/future days)"),
    SEC_CAL_GRANT("Grant rewards on completion (XP / score / debuffs / hooks)"),
    SEC_CAL_JOURNAL("Enable Journal (write per-day notes)"),
    SEC_CAL_JOURNAL_ONLY("Journal Only (notes replace the Task List)"),
    SEC_CAL_DESC("Define the calendar's tasks on the page itself. Mark a day done to color it; toggle which indicators show above. Right-click a day for journal / favorite / customize options."),

    // ---------------------------------------------------------------------
    // Setup Wizard
    // ---------------------------------------------------------------------
    WIZ_TITLE("Welcome to Statmaxxer"),
    WIZ_HEADER("Let's set things up"),
    WIZ_SUB("You can change any of this later in Settings."),
    WIZ_SEC_PROFILE("1 · Your Profile"),
    WIZ_SEC_PROFILE_DESC("Names your active profile and how the app greets you."),
    WIZ_LBL_DISPLAY_NAME("Display name:"),
    WIZ_PROMPT_NAME("Your name / profile name"),
    WIZ_SEC_SECTIONS("2 · Sections"),
    WIZ_SEC_SECTIONS_DESC("Pick a starting point for your pages."),
    WIZ_KEEP_STARTER("Keep starter sections (Quick, Daily, Work)"),
    WIZ_START_EMPTY("Start empty — I'll create my own"),
    WIZ_SEC_APPEARANCE("3 · Appearance"),
    WIZ_SEC_APPEARANCE_DESC("Checkbox theme and the app-wide font (includes Retro, Pixel Art, Matrix)."),
    WIZ_LBL_CHECKBOX_THEME("Checkbox theme:"),
    WIZ_LBL_FONT_STYLE("Font style:"),
    WIZ_SEC_PERSONAL("4 · Personal & Behavior"),
    WIZ_SEC_PERSONAL_DESC("Used by the age analytics and app behavior."),
    WIZ_LBL_TARGET_AGE("Target age:"),
    WIZ_CHK_STARTUP("Launch on Windows startup"),
    WIZ_CHK_NOTIFICATIONS("Enable desktop notifications"),
    WIZ_BTN_FINISH("Finish Setup"),

    // ---------------------------------------------------------------------
    // Profiles
    // ---------------------------------------------------------------------
    PROFILE_DESC("Each profile is a separate world with its own tasks, sections, and stats. The active profile is remembered the next time you launch."),
    PROFILE_ACTIVE_LABEL("Active profile: {0}"),
    PROFILE_SWITCH_HEADER("Switch Profile"),
    PROFILE_SWITCH_BODY("Switch to profile \"{0}\"? Your current profile is saved automatically."),
    PROFILE_NEW_HEADER("New Profile"),
    PROFILE_NEW_PROMPT("Name for the new profile:"),
    PROFILE_CREATED_SWITCH_HEADER("Switch to New Profile?"),
    PROFILE_CREATED_SWITCH_BODY("Profile \"{0}\" created. Switch to it now?"),
    PROFILE_RENAME_HEADER("Rename Profile"),
    PROFILE_RENAME_PROMPT("New name for \"{0}\":"),
    PROFILE_CANT_DELETE_DEFAULT("The Default profile cannot be deleted."),
    PROFILE_CANT_DELETE_ACTIVE("You cannot delete the profile you are currently using. Switch to another profile first."),
    PROFILE_DELETE_HEADER("Delete Profile"),
    PROFILE_DELETE_BODY("Permanently delete profile \"{0}\" and all of its data? This cannot be undone."),
    PROFILE_DELETE_FAILED("This profile could not be deleted."),

    // ---------------------------------------------------------------------
    // Settings page chrome / loading
    // ---------------------------------------------------------------------
    SETTINGS_SHOW_SECTION("👁 Show {0}"),
    SETTINGS_HIDE_SECTION("🙈 Hide"),
    SETTINGS_HIDE_TOOLTIP("Hide this section"),
    SETTINGS_SHOW_TOOLTIP("Show this section"),
    TEMPLATE_NONE_FOR_SECTION("No templates for this section."),
    LOADING_TASKS("Loading your tasks…"),
    LOADING_SWITCHING_PROFILE("Switching profile…"),

    // ---------------------------------------------------------------------
    // Stats — EXP leveling
    // ---------------------------------------------------------------------
    STAT_USE_EXP("Use EXP Leveling"),
    STAT_EXP_PER_LEVEL("EXP per Level:"),
    STAT_CURRENT_LEVEL("Current Level / \nStarting Amount:"),
    STAT_EXP_TOOLTIP("Rewards/penalties feed an EXP pool instead of changing the stat value directly.\nFilling the bar grants a point; dropping below 0 removes one (carrying the remainder)."),
    STAT_EXP_BAR_TOOLTIP("Show / hide the EXP bar"),
    STAT_EXP_BAR_LABEL("Lv {0}  ·  {1} / {2} EXP"),
    SET_SHOW_EXP_BARS_TITLE("Show Stat EXP Bars"),
    SET_SHOW_EXP_BARS_DESC("Default visibility of EXP progress bars on the Stat page for stats using EXP leveling. Each stat card can override this."),

    // ---------------------------------------------------------------------
    // Misc / generic
    // ---------------------------------------------------------------------
    NOTIFY_DEBUFF_CLEANSED_TITLE("Debuff Cleansed!"),
    NOTIFY_DEBUFF_CLEANSED_BODY("You have overcome: {0}"),
    ERR_OPEN_PATH("Failed to open path: \n{0}"),
    ERR_EXECUTION_HEADER("Execution Error"),
    ERR_BAD_TIME_HEADER("Invalid Time"),
    ERR_BAD_TIME_BODY("Time '{0}' is not a valid HH:mm value (00:00-23:59). The deadline was saved as 00:00 — please re-open and fix.");

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
