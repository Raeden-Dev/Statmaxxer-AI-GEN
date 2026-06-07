# Suggested Improvements

A running list of concrete, high-value changes for the ORS-To-Do code base. Items are
roughly ordered by impact-to-effort. Sections marked **✅ Done** were applied in the last pass;
the remainder is the open backlog.

---

## ✅ Done

### 1. `Lang` enum + `LangManager` with locale support
- All sentences in the card / challenge / perk / repeatable subsystem now route through `Lang`.
- `LangManager` now supports `registerLocale(...)` + `setLocale(...)`, with per-locale partial
  override tables and graceful fall-back to the enum default. Adding a translation is a single
  `Map<Lang, String>` registration; no caller changes needed.
- Migrated additional copy: `TaskDialogs.showCreditsDialog` / `showHelpDialog`, `DynamicModule`
  empty-states and the "Active Debuffs" header.

### 2. Shared utilities — `ColorUtil`, `ProgressionService`, `DependencyMenuBuilder`
- `ColorUtil` is the single source of `Color → "#RRGGBB"` (and the `"transparent"` variant the
  cards use). Removed the private `toHexString` duplicates from `ChallengeCard`/`ChallengeConfig
  Dialog`/`PerkCard`/`SectionEditDialog`.
- `ProgressionService` holds the pure rules: `isDependencyUnlocked`, `meetsRequirements`,
  `isInSetupPhase`, `recomputePerkState`. `TaskLinkUtil` is now a thin delegate.
- `DependencyMenuBuilder` builds the "Hook Tasks / Challenges" `MenuButton` shared by both Perk
  and Challenge editors (deletes ~50 duplicated lines per dialog).

### 3. Tests
- JUnit 4 wired up as a `test` dependency.
- `ProgressionServiceTest` covers the counter→perk regression, stat-threshold gating, perk
  unlock/lost transitions, and the setup-phase window (13 tests). Runs via `mvn test` (and was
  verified offline via `JUnitCore` on this machine).

### 4. Counter UI enforces the cap
- `RepeatableTaskCard` shows `x / max` when a target is set, disables the `+` button at the cap,
  and refuses to advance past max even if clicked.

### 5. Design class for long/design dialogs
- `Design.confirmedYes(...)` / `Design.warn(...)` / `Design.error(...)`. All card delete /
  fail / complete / edit-lock dialogs route through it. Copy comes from `Lang`; styling from
  `TaskDialogs.styleDialog`.

### 6. Split `ChallengeCard` below the 500-line god-class threshold
- Was 631 lines; now `ChallengeCard` (view, ~340) + `ChallengeConfigDialog` (editor, ~340).

### 7. Fail Challenge button + counter→perk bug fix
- Challenge cards expose a **Fail Challenge** action; failed challenges show a "💀 Failed on" stamp
  and a dimmed-red style.
- `ProgressionService.isDependencyUnlocked` requires counter cards to reach their target before
  they count as satisfied — fixes the reported bug.

### 8. Card categorization system
- New `SectionConfig.enableCategories` toggle (in Section Edit dialog).
- New `TaskItem.categoryName` field; per-card category input in Task/Perk/Challenge edit dialogs
  with chips for existing categories in the same section.
- `CategoryGroupRenderer` renders tasks under collapsible category headers; collapse state is
  persisted per-section via `AppStats.collapsedCategories`.

### 9. "Prevent Editing (Hours)" moved to per-section
- Removed from `GeneralSettingsPanel`; added to `SectionEditDialog`.
- Migration on first launch copies the legacy global value onto every section that had 0, then
  zeroes the global field so the new per-section knob is the source of truth.
- All four card readers (`ChallengeCard`, `PerkCard`-context, `TaskCard`, `RepeatableTaskCard`,
  `TaskContextMenu`) now consult `config.getPreventEditingHours()`.

### 10. Tray second-launch surfaces the existing window
- New `WindowRestorer.surface(Stage)` applies the Windows-foreground-lock workaround
  (alwaysOnTop flip + iconify nudge) so a second-launch via taskbar/icon while the app is in tray
  now reliably brings the existing window to the front instead of being swallowed.
- Used by `SingleInstanceManager` (FOCUS message) and `SystemTrayManager` (double-click + "Open"
  menu item).

---

## 🟡 Backlog

### 8. Finish the `Lang` migration outside the card subsystem
Settings panels (`GeneralSettingsPanel`, `StatsManagerPanel`, `DangerZonePanel`, `Priority
ManagerPanel`, `TemplateEditDialog`, …), services (`BackupManager`, `NotificationManager`,
`DailyRolloverManager`), and the help-card body strings still hold inline literals.

**Action.** Sweep one panel at a time; add `Lang` keys per group; verify with a quick translation
table.

### 9. Move progression logic *fully* out of the view layer
`PerkCard` still calls `ProgressionService.recomputePerkState(...)` **and** `StorageManager.save
Tasks(...)` during construction. The pure rule is now testable, but the side-effect remains
order-sensitive (re-rendering the same perk twice in one tick can re-stamp dates). See
[`potential_bugs.md`](potential_bugs.md) #5.

**Action.** Centralize "recompute & save" in the daily-rollover/app-tick path; let the card only
**read** the model.

### 10. Bug fixes from `potential_bugs.md` — ✅ DONE for the high-priority cluster
- #1 priority `.get(1)` crash — fixed (clamped index)
- #2 challenges auto-levelled as perks on Monday — fixed (rollover skips `isChallengeCard`)
- #3 failed challenges satisfying hooked deps — fixed (`ProgressionService` requires `perkUnlockedDate`)
- #4 daily rollover finishing & archiving counter / repeating tasks — fixed (rollover sweep only archives *finished* non-counter tasks)
- #6 wrong task flagged as challenge — fixed (input panel flags directly)
- #7 expired challenge state not persisted — fixed (auto-fail on expiry)
- #8 deadline edits re-arm notifications — fixed (Objects.equals guard)
- #9 stat atrophy single-day catch-up — fixed (now loops)
- #10 garbage time string silently saved — fixed (Design.warn surfaces it)
- #12 stale dep ids — fixed (`DependencyMenuBuilder.stripStale`)

Remaining: #5 (perk state mutation in view) and #11 (blank-priority chip cosmetic).

### 11. Keep splitting classes that are trending toward "god class" status
`ChallengeCard` was the only file >500 lines. Watch the next-largest:
- `PomodoroTimer` (459) — timer + UI + notifications.
- `AppStats` (393+) — data model has become a grab-bag of display + RPG + paths.
- `FilterSortHeader` (362), `GeneralSettingsPanel` (362), `TemplateEditDialog` (361).

**Action.** Soft 300-line guideline for UI classes; split when a class owns both layout and
behavior. For `AppStats`, group fields into cohesive sub-objects (`DisplaySettings`,
`RpgSettings`, `StreakSettings`).

### 12. Broaden test coverage
- `ProgressionServiceTest` is a template; add equivalents for `DailyRolloverManager` (covers #2,
  #4, #9 from the bug doc), `TaskActionHandler.processRPGStats`, and `ColorUtil`.
- A small fake `StorageManager` (interface + in-memory impl) would let the rollover/action tests
  run without touching `%APPDATA%`.

### 13. (Originally on this list, **superseded**) ~~Migrate `StorageManager` off Java serialization to JSON~~
Already JSON via Gson — only the migration path from `.dat` legacy files still uses Java
serialization. Models still implement `Serializable` only for that fallback; safe to delete the
`Serializable` markers and `loadLegacyDat(...)` after a deprecation window.

---

## Reference

- [`section_bug_hunting.md`](section_bug_hunting.md) — section option combinations worth testing.
- [`potential_bugs.md`](potential_bugs.md) — twelve concrete defects with repro steps.
