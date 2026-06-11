# Statmaxxer — Changelog

All notable changes to the project are recorded here, newest first. Dates use ISO format
(YYYY-MM-DD). Version numbers match the `APP_VERSION` constant in `TaskTrackerApp`.

---

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

### Added (app)
- **First-launch Setup Wizard** — on a brand-new install, a guided setup covers profile/display
  name, starter sections, appearance (theme + font), and personal/behavior preferences. Runs once.
- **Loading screen** — a brief branded loading view shows on app launch and when switching profiles
  so large data sets don't appear to freeze.

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
