# Statmaxxer — Changelog

All notable changes to the project are recorded here, newest first. Dates use ISO format
(YYYY-MM-DD). Version numbers match the `APP_VERSION` constant in `TaskTrackerApp`.

---

## v1.488 — 2026-07-02

### Added
- **Custom Scenario entries (Calendar Journal-Only).** Alongside **Add Journal Entry** and **Add
  Event**, days now offer **＋ Add Custom Scenario** (also in a day's right-click menu). A scenario is
  a journal card with a **user-defined tag name** and **tag colour** instead of the fixed `log` /
  `event` tag — so you can label days with your own categories (e.g. *Workout*, *Travel*, *Sick
  Day*). The editor includes Randomize Style, which also rolls a tag colour.
- **Filter journal entries by tag.** When a day holds entries with more than one tag, the day-detail
  panel shows a **Filter** dropdown to narrow the list to a single tag (log / event / any scenario).
- **Undo for deletions.** Deleting a journal/event/scenario entry, a calendar task, or a style preset
  now shows an **Undo** toast that restores it (at its original position, including a calendar task's
  day marks) — matching the existing undo for regular tasks.
- **Export / import style presets.** The **Style Preset** row has a new **⇄** menu to export all
  presets to a JSON file or import them from one (imported presets get fresh ids), so looks can be
  shared between profiles or machines.
- **Reset Style.** The **Customize Day** dialog has a **↺ Reset Style** button that clears the day's
  icon and (in Journal-Only mode) its background/outline back to default. A matching **Reset Day
  Style** item also appears in a day's right-click menu when the day has any custom styling.
- **Edit / delete style presets.** The **Style Preset** row now has **✎** (rename the selected preset
  and update it to the current style) and **🗑** (delete the selected preset) buttons next to **Save**.

### Changed
- **Style Preset row moved to the top** of the Customize Day and journal/event/scenario editors
  (above *Icon & Color*).
- **Uniform dialog controls.** The Background Color, Outline Color, Icon & Color, preset, and
  Randomize/Reset controls in the Customize Day and entry editors now all stretch to the same width
  (to the right edge), instead of the colour pickers being narrower than the buttons.
- **Storage write failures are now surfaced.** A locked database or full disk previously failed a
  save silently (stderr only); a throttled toast now warns that the last change may not have saved.
- **Focus-timer task search** no longer force-opens its dropdown on every keystroke — it only opens
  while actively narrowing with a query.

### Fixed
- **Focus timer double-granted stat rewards.** Completing a timed task via the Pomodoro timer applied
  its stat rewards twice (and applied them even if a confirmation prompt was declined). It now applies
  them once through the normal completion path.
- **Global score could go negative.** Un-checking or decrementing a rewarded task subtracted its
  points without a floor, so if the points had been spent the score went below zero. Reversals now
  floor at 0 and log the actual delta.
- **Weekly history only recorded one section.** With more than one streak section, each overwrote the
  same day's history entry; the daily-completion chart now records the aggregate across all streak
  sections. The detailed history log is also trimmed to the retained window (it grew unbounded).
- **Lifetime "earned" over-counted capped rewards.** `gain()`/`drain()` now report the amount
  actually applied, so a reward discarded at the stat cap no longer inflates lifetime totals.
- **Per-Column task tiles no longer shift size.** In the Task List *Per Column* view, tile height was
  bound to tile width, so resizing the window resized every tile. Tiles now use a fixed, uniform
  height and only flex in width with the available space; a hover tooltip shows the full text if a
  long name/summary is clipped.

### Reverted
- **Journal/event card text is a plain label again.** The selectable text box added in v1.487 (which
  showed a grey rectangle on each card and allowed in-card text selection/copy) has been removed,
  along with the card's right-click **Copy Text** option. Clicking a card opens its editor as before.

---

## v1.48 — 2026-06-23

### Added
- **Base Stats dialog.** A **⚖ Base Stats** button in the Stat Configuration header (just before the
  section's "Hide" toggle) opens a dialog to set a baseline value per stat. Each row has a **↩ Reset
  to Base** button that snaps that stat's current value back to its baseline. Base values persist on
  each stat.
- **Export Task Information.** Right-clicking a task-bearing section in the sidebar adds **"Export
  Task Information"**, which opens the OS save dialog (default name
  `[section_name]_task_information.txt`) and writes an organized text report of every non-archived
  task in the section — including status, category/prefix/type, description-card text, sub-task
  links, and sub-tasks. Available on normal/notes/rewards sections (not Stat/Perk/Challenge/Calendar
  or separators).
- **Auto-Style on Category.** A new **"Auto-Style on Category"** toggle in *Edit Section* (shown when
  Categories are enabled). When on, moving a **completely unstyled** card into a category copies that
  category's icon, colour, and outline onto the card. One-way and non-destructive — a card with any
  styling of its own is left untouched.
- **Allow Notes (per-card notes outside Notes pages).** A new **"Allow Notes"** section toggle. When
  on, any card in that section can be converted into a **notes card** from its edit dialog
  (**"Is Notes Card?"**) — the card then behaves like a note (a 📌 pin replaces the checkbox and it
  never counts as completed). Mutually exclusive with Link, Description, and Repeating cards.
- **Google Drive sync (cloud save & sync).** A new **Cloud Sync (Google Drive)** card in Settings.
  Connect a Google account to mirror all data files to a **`statmaxxer-data/`** folder in that
  account's Drive, removing USB/manual transfer entirely. Syncing is **asynchronous and
  single-writer** (a single-thread queue guarantees no two writes overlap); local saves upload
  automatically, with a manual **Sync Now** as well. Uses the restricted `drive.file` scope so the
  app only sees files it created.
  - **Setup note:** Google requires each app to use its own OAuth client, which cannot be bundled.
    Until you add a "Desktop app" OAuth `client_secret.json` to the data folder (renamed to
    `google_client_secret.json`), Cloud Sync shows a clear **"Not configured"** state and the rest of
    the app is unaffected. Step-by-step instructions ship in `google_drive_setup.txt`.
  - The card shows the **connected account email** and the **last sync time**, and the app runs a
    sync **on startup**.
  - Sync is **two-way and safe**: a per-file marker tracks what changed on each side, so a fresh
    device **pulls** your data down instead of overwriting Drive, and a device that still holds the
    real data **pushes** it back up (resolving the ambiguous/first-sync case by keeping the larger
    database). **Before any database is overwritten, a timestamped backup is written to
    `cloud_sync_backups/`.** Save-triggered syncs are push-only (they never replace the database
    you're editing mid-session); pulls happen on connect, startup, and "Sync Now", and reload the UI
    when newer data arrives.
- **Per-stat atrophy reset + countdown.** Each row in Stat Configuration gains a **⏳** button that
  resets just that stat's atrophy timer, plus an inline countdown showing how long until the stat
  starts decaying (or "Atrophy due" / "stat at 0" status). The countdown is hidden for stats with no
  atrophy configured.
- **Settings quick-jump nav.** A floating, icon-based menu pinned to the right of the Settings page
  jumps straight to each section (Help, Profiles, General, Sections, Templates, Stats, Priorities,
  Data, Cloud Sync, Danger Zone).
- **Sidebar pop-out windows.** Hovering a sidebar button reveals a **⧉** icon that opens that section
  in its own detached window (up to 5 at once); clicking it again closes that window.
- **Link cards: "Go to Link" button + sub-tasks.** Link cards now show a proper **↗ Go to Link**
  button (styled like the description card's Copy button) instead of the thin indicator, and can
  coexist with sub-tasks and sub-task links in the same section.

### Changed / Fixed
- **Dialog alignment.** Customization dialogs now line up consistently: the Category style editor's
  colour pickers and icon dropdown fill the input column (instead of sitting narrow and left-skewed),
  the card-edit "Randomize Style" button spans the full width, and labels next to multi-line fields
  (Content, Description, Notes) top-align with their input.
- **"Work Type" field no longer mislabelled "Category".** In the card editor the work-type field
  (from the section's "Enable Work Types" toggle) was labelled "Category:", clashing with the
  separate Categories feature; it now reads "Work Type:".
- **Number fields reject non-numeric input.** Points, costs, penalties, focus minutes and counter
  fields now accept digits only, instead of silently zeroing a typo on save.
- **Duplicate card is now a complete copy.** "Duplicate" also carries over the card's category,
  description-card text, notes-card flag and required focus time (it already copied styling and stats).
- **Consistent dialog scrollbars and colour-picker widths.** The card, stat, section and category
  editors now share one scrollbar style helper, and colour pickers that trail a field use a uniform
  width so every row's right edge lines up.
- **Score can no longer go negative from daily penalties.** Missed-task penalties now clamp the
  global score at zero, and the stat ledger records the actual (clamped) change.
- **Cloud Sync now decides by actual content, not timestamps.** Change detection uses a **content
  checksum (MD5)** — the local file's hash vs Drive's `md5Checksum` vs the hash agreed at the last
  sync — instead of file modified-times (the app rewrites its DB on open/rollover, so timestamps
  always looked "changed"). A sync now: does **nothing** when the two copies are byte-identical;
  **pushes** when only the local copy has new content; **pulls** (and reloads) when only the remote
  has new content; on a true conflict the remote wins and the local copy is backed up first. This
  stops redundant uploads/downloads and reliably propagates every card addition between devices.
  On-save pushes likewise upload only when local content actually changed and the remote isn't ahead.
- **Auto-Style on Category** now also recolours the card's small side rectangle, and **forcibly
  overwrites** any existing card styling with the category's icon/colours (previously it only styled
  cards that had no styling of their own).
- **"Sync Style" on a category.** A category header's right-click menu gains **Sync Style**, which
  applies that category's icon/colours to every card in the category at once.
- **Drag a card onto a category header** to move it into that category (dropping on the
  "Uncategorized" header clears the card's category). Honours **Auto-Style on Category** just like the
  "Move to Category" menu.
- **Undo for task deletion.** Deleting a card now shows an **Undo** toast at the bottom of the window
  that restores the card (and the dependencies that pointed at it) to its original position.
- **Searchable dropdowns.** The **Dependencies** and **Inflict Debuffs** pickers gain a type-to-filter
  search box, so long lists are quick to navigate.
- **Cloud Sync: Restore from Backup.** A new button lists the local pre-overwrite backups (with date
  and size) and rolls one back over the live database — works offline, and backs up the current data
  first so a restore is itself reversible. A small spinner now shows while a sync/connect is running.
- **Bulk "Move all cards to…".** A category header's right-click menu can move every card in that
  category to another category at once.
- **Keyboard shortcuts in dialogs.** Enter confirms and Esc cancels across the card, category, stat,
  section, and calendar editors.
- **Calendar journal: drag to reorder entries.** In a day's entry list (Journal-Only mode), drag a
  log/event card onto another to rearrange them; the order persists.
- **Calendar journal: convert log ↔ event.** A journal entry's right-click menu can convert a log into
  an event (or back) in place, keeping its text and styling.
- **Calendar style presets.** Both the **Customize Day** dialog and the journal **entry/event** editor
  gain a "Style Preset" row — save the current icon/colours under a name and re-apply it later from a
  dropdown. Day-style and entry-style presets are stored separately and persist with your data.
- **Copy text from read-only displays.** Displayed (non-editable) text can now be copied: journal
  entries and description cards get a **Copy Text**/**Copy Description Text** menu item, and sub-tasks,
  sub-task links and perk/challenge descriptions get a right-click **Copy**. (Editable text fields and
  text areas already supported normal copy/paste.)
- **Right-clicking a category header** no longer collapses/expands it — only a left-click toggles the
  group; right-click just opens the customise menu.

## v1.472 — 2026-06-14

### Added
- **Description Card (new card type).** Enable **"Enable Description Cards"** in *Edit Section* on a
  compatible page, then toggle **"Is Description Card?"** in a card's edit dialog and give it a body
  of text. The card replaces its checkbox with a **📋 Copy** button that copies that text to the
  clipboard (with a brief "✓ Copied" confirmation). Mutually exclusive with Link and Repeating
  cards. The section toggle is blocked on incompatible pages (Rewards, Stat/Perk/Challenge,
  Calendar); allowed on normal and notes pages.
- **Move to Category context-menu submenu.** When a page has **Enable Categories** on, every card's
  right-click menu gains a **"Move to Category"** submenu listing all categories available on that
  page (used by tasks or defined as styles), with the card's current category pre-selected, plus a
  **"Set as Uncategorized"** option. Works on normal task cards as well as **perk and challenge
  cards** (which group by category too).
- **Calendar Task List view modes.** A **Per Row / Per Column** selector in the Task List header.
  *Per Column* lays the task cards out in a grid (up to 5 per row) as **square tiles** — the task
  name wraps instead of being cut off and the ⚙ gear is pinned to the tile's top-right; *Per Row*
  keeps the single-column list. The choice is remembered per calendar page.
- **Drag-to-reorder calendar task cards.** Drag a task card onto another to reorder the Task List;
  works in both the Per Row and Per Column layouts.

### Changed
- **Stat-page History is now a live ledger.** The 📖 History dialog shows a chronological log of every
  actual Custom Stat / Global Score point/EXP/cap change *and its source* (task completion, miss
  penalty, reward purchase, calendar mark, focus session, daily atrophy, and reversals), newest
  first with colour-coded badges — instead of re-deriving "history" from task cards. The ledger is
  recorded going forward and capped to the 300 most recent entries; it is empty until new stat
  changes occur after upgrading.
- **Calendar journal opens on double-click.** In Task+Journal mode (with no task brush selected), a
  day's journal now opens on a **double-click** instead of a single click; a single click shows a
  hint. Journal-Only mode still selects a day on a single click.

## v1.465 — 2026-06-10

### Added
- **Calendar Page (new special page type).** A month grid you can navigate (previous/next/Today).
  Marked days are coloured with **segment bands and/or dots** (toggle which to show in *Edit
  Section*). Includes a **Show All / per-task display filter**, an **"Allow Calendar
  Manipulation"** toggle (off = you may only mark *today*; on = you may mark any past/future day),
  and an **HTML analytics export** (per-task totals + monthly breakdown).
  - **Dashboard layout** matching other pages: a title strip carries the month switcher, Today,
    and Export; the *Show All* filter sits in the same spot as the Order dropdown on other pages.
  - **Task List** of cards below the calendar: an add-task card plus one card per task. Click a
    card to make it the marking "brush", use the **⚙ gear** to configure it, and **right-click to
    delete** it.
  - **Journal, favorites & day icons:** right-click any day for a menu — journal, **Favorite Day**
    (gold ★ + border, plus a **★ Favorites** filter entry), **Set Day Icon**, and **Clear Day
    Marks**. Days with a note show a 📝 indicator. Enable per-section via **Enable Journal**.
  - **Journal-Only mode with multiple entries & events:** turn on **Journal Only** to replace the
    Task List with a per-day panel — click a day and the bottom area shows that day's **journal
    entries and events**, each a **fully customizable card** (text, background, outline, icon, with
    a 🎲 randomize) edited via its ⚙ gear or right-click. Add as many journal entries and **events**
    (shown with a small "event" tag) per day as you like. In Task+Journal mode a day keeps a single
    note. All entries are included in the HTML export.

### Added (stats)
- **Per-stat EXP leveling** — each stat can opt into an EXP economy (toggle + *EXP per Level* +
  *Current EXP* in its settings). Rewards/costs/penalties now feed an EXP pool instead of moving the
  value directly, so points can't be trivially farmed. Filling the bar grants a stat point (carrying
  the remainder toward the next level); a big reward can grant several at once. Penalties/costs drain
  EXP and, on crossing 0, remove points (carrying the deficit down). At max cap the bar stays full;
  at 0 points EXP is floored. Atrophy drains a level's worth of EXP for EXP stats. Stats left with
  EXP off behave exactly as before.
  - **EXP bar on the Stat page** with level + current/required EXP. A global **Show Stat EXP Bars**
    setting sets the default; each stat card has a chevron to override it.

### Added (app)
- **First-launch Setup Wizard** — on a brand-new install, a guided setup covers profile/display
  name, starter sections, appearance (theme + font), and personal/behavior preferences. Runs once.
- **Loading screen** — a brief branded loading view shows on app launch and when switching profiles
  so large data sets don't appear to freeze.

### Changed (calendar polish)
- **Journal/event cards** use a new row layout: entry-type tag (*event*/*log*) → icon → a colored
  rectangle spanning the card's height (like note sideboxes) → text.
- **Add Journal Entry / Add Event** buttons restyled to bright-outline-on-dark, matching the rest
  of the app; the entry edit dialog's controls are now uniformly sized and dark-themed.
- The day right-click menu starts with **Customize Day…** (day icon + color always; in Journal-Only
  mode the day cell's background/outline are stylable like a card, with randomize). The separate
  *Set/Change/Clear Day Icon* items were folded into it.
- In **Journal-Only** mode the top-right filter no longer lists calendar tasks (only *Show All* and
  *★ Favorites*), since the task system is hidden there.
- **Edit Section compatibility engine:** toggles that can do nothing for the selected page type are
  now disabled — Calendar/Stat pages grey out all task-feature toggles; Perk/Challenge pages grey
  out dead switches (tags, score, stats, dates, favorites, icons, styling, archiving, locking);
  Notes pages grey out completion-driven features; *Enable Sub-Task Links* requires *Enable
  Sub-Tasks*.
- New-feature UI text (calendar, journal, wizard, profiles, settings chrome) now routes through the
  central `Lang`/`LangManager` i18n layer.

### Fixed
- **Profile switching crash** — switching profiles no longer throws a
  `RejectedExecutionException` from the global hotkey hook (the JNativeHook dispatch pool is kept
  alive; only its data references are swapped).
  - **Rich calendar tasks:** each task has a **colour and icon** (the icon shows on marked days),
    a **🎲 Randomize** button, and — when the section's *Grant rewards on completion* toggle is on —
    can award **XP to multiple Custom Stats**, **stat Max-Cap increases**, **global score points**,
    **inflict debuffs**, and **hook other cards** (completing normal cards and adding +1 to
    counter/repeating cards on completion). Un-marking a day reverses the stat XP, Max-Cap and
    score it granted.
- **Profile system.** Manage multiple fully-separate "worlds" (each with its own tasks, sections,
  stats, priorities and calendars) from a new **Profiles** section in Settings. Create, rename,
  switch and delete profiles. Each profile is backed by its own database file; the active profile
  is remembered and restored on the next launch. The built-in *Default* profile keeps existing
  data unchanged.
- **More fonts.** A new **Font Style** dropdown (above *Task Font Size* in *Appearance & Behavior*)
  applies a font family across the whole app. Bundled options include **Retro (VT323)**,
  **Pixel Art (Press Start 2P)** and **Matrix (Share Tech Mono)**, plus Monospace and Serif.
- **Collapsible settings sections.** *Dynamic Sections*, *Stat Configuration* and *Priorities* now
  each have an **eye toggle** in their top-right corner to hide/show the section so the Settings
  page stays compact. Collapse state is remembered.
- **"(Hide)" option in Auto-Generating Tasks.** The section selector now starts collapsed and
  offers a *(Hide)* entry, so opening a page with many templates no longer cramps the Settings
  page with a long list.

### Changed
- **Link cards are now fully customizable.** Background and outline colours set on a link card are
  respected (they previously always rendered blue). Link cards can be themed even when the
  section's *Enable Task Styling* toggle is off, matching the behaviour of other cards. Blue
  remains the default when no custom colour is chosen.
- **General Configuration** now appears **above** *Manage Dynamic Sections* in Settings.
- In the **Edit Section** dialog, the **Special Page Modes** box now sits directly below
  *Name / Theme Color / etc.* and above the feature checkboxes (it was previously at the very
  bottom).

### Notes
- This file (`CHANGELOG.md`) was added under `src/main/resources` and will track changes per
  release going forward.

---

## v1.461 — 2026-06-09
- Maintenance and stability release.

## v1.46 — 2026-06-07
- Feature and fix rollup.

## v1.455 — earlier
- Incremental improvements.

## v1.45 — earlier
- Baseline release prior to the v1.46x line.
