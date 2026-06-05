# Section Bug-Hunting Guide

A section's behaviour is driven by ~30 toggles on `SectionConfig`. Many of them interact, and the
interactions are enforced in **three different places** that don't always agree:

1. **`SectionEditDialog.updateUIState`** — disables/auto-unchecks conflicting toggles *while you edit*.
2. **`DynamicModule.refreshList`** — decides how the page renders, using a fixed **priority order**.
3. **`DailyRolloverManager` / `TaskActionHandler`** — act on the flags at runtime (resets, archiving, XP).

Because the rules live in separate places, the most productive bugs come from **option combinations**
that one layer allows but another mishandles. This guide lists the combinations worth testing and what
to watch for. (Concrete, already-identified defects are written up separately in
[`potential_bugs.md`](potential_bugs.md).)

---

## 1. The option model at a glance

### Special "page mode" flags (intended to be mutually exclusive)
`Notes Page`, `Stat Page`, `Perk Page`, `Rewards Shop`, `Challenge Page`.

- The edit dialog enforces "pick at most one" only via the checkbox **click handlers**
  (`SectionEditDialog` lines 194–198). They are **not** reconciled when the dialog *opens* or when a
  **preset** is loaded — so a config that already has two of them set stays that way.
- The renderer resolves conflicts with a hard priority order
  (`DynamicModule.refreshList`, lines 141–143):

  ```
  Stat Page  >  Perk Page  >  Challenge Page  >  (Notes / Rewards / normal)
  ```

  So if two flags are ever both true, the higher one silently wins and the other is ignored.

### Reset / streak group
`Reset Interval (Hours)`, `Enable Streak System`, `Auto-Archive Completed`.

- `Streak` is force-disabled unless interval > 0.
- For any special page, interval is forced to 0 and auto-archive is force-disabled.

### Mutually-constraining feature toggles
- `Enable Sub-Tasks` ⟷ `Enable Link Cards` ⟷ `Track Focus Time` are pairwise disabled
  (`updateUIState` lines 176–177): link cards conflict with sub-tasks **and** focus time.
- `Enable Optional Tasks` requires **both** an interval **and** a point system
  (`Enable Point System` or `Enable Custom Stats`).

---

## 2. Combinations worth testing (and what to look for)

Legend: ✅ = should work, ⚠️ = likely to surface a bug.

### A. Special-mode conflicts
| # | Set these options | How to force it | Watch for |
|---|---|---|---|
| A1 | ⚠️ `Stat Page` **+** `Challenge Page` together | Save a **preset** while one is checked, then load it after checking the other; or edit saved JSON in `%APPDATA%\TaskTracker\stats.json` | Page renders as **Stat** only; the Challenge intent is silently lost. Input bar/zen/buttons may mismatch the visible content. |
| A2 | ⚠️ `Perk Page` **+** `Rewards Shop` | Same preset trick | Renders as Perk; "Buy" semantics from Rewards never apply. |
| A3 | ✅ Switch a populated section's mode `Normal → Challenge` | Toggle Challenge Page on an existing to-do section with tasks | Existing plain tasks are now drawn as `ChallengeCard`s; confirm none of them already had `isChallengeCard=false` rendering oddly. |
| A4 | ⚠️ `Challenge Page` + add a task | On a Challenge page, type a name and Add | Only the *last task in the whole database* is flagged as a challenge (`DynamicModule` lines 92–99). Add quickly / with other sections to see the wrong card flagged. |

### B. Reset interval + streak + archive
| # | Set these options | How to force it | Watch for |
|---|---|---|---|
| B1 | ⚠️ `Reset Interval > 0` + a **repeating** or **counter** task | Make a daily section, add a 🔁/counter task, advance the system clock a day, reopen | On rollover **every** task in the section is archived **and marked finished** (`DailyRolloverManager` lines 59–64) — including counters/repeaters, unlike `autoArchiveTasks` which skips counters. Progress vanishes. |
| B2 | ✅/⚠️ `Streak` + miss exactly 1 day vs 2 days | Change clock +1 day, reopen; then +2 days | 1 missed day keeps/increments streak if completion ≥ threshold; ≥2 days always resets. Verify the threshold boundary (`requiredFraction - 0.001`). |
| B3 | ⚠️ `Streak` on, then add tasks **after** the day starts | Complete 100% early, then add a new incomplete task before rollover | History fraction is computed at rollover over the *current* task set; late additions can swing the percentage and break a streak you "earned". |
| B4 | ⚠️ `Auto-Archive` + `Lock Task After Completion` | Both on; complete a task | Auto-archive removes the card immediately, so the "locked after completion" behaviour is never observable — confirm that's intended. |

### C. Feature-toggle conflicts
| # | Set these options | How to force it | Watch for |
|---|---|---|---|
| C1 | ⚠️ `Sub-Tasks` + `Link Cards` + `Track Focus Time` all on | Load a preset built before the mutual-exclusion rule, or edit JSON | The edit dialog will fight you, but a stored config can hold all three. Open such a section and check cards render without exceptions. |
| C2 | ⚠️ `Optional Tasks` with interval but **no** points | Enable optional + interval + points, save preset; later turn points off via JSON | `updateUIState` would uncheck optional on open, but a never-reopened config keeps `enableOptionalTasks=true` with no points — optional tasks may grant/strip nothing. |
| C3 | ✅ `Show Priority` off on a normal page | Turn off priority toggles | Input bar should omit the priority dropdown — but see **D1** for the related crash. |

### D. Global settings × section options
| # | Set these options | How to force it | Watch for |
|---|---|---|---|
| D1 | ⚠️ Exactly **one** custom priority + any section with `Show Priority` | Settings → Priority Manager, delete priorities down to 1; open a normal section | **Crash**: `DynamicInputPanel` reads `getCustomPriorities().get(1)` (index 1) guarded only by `isEmpty()`. The page fails to build. (See `potential_bugs.md` #1.) |
| D2 | ⚠️ `Prevent Editing (hours) > 0` + `Challenge Page` with a far deadline | Settings → set prevent-editing to 1h; create a challenge; wait 1h | The challenge becomes uneditable (time lock) even though it's not complete and the deadline is far away — you can only Complete/Fail it. |
| D3 | ⚠️ `Enable Custom Stats` section + a **Challenge** with stat requirements + a **Perk** hooked to that challenge | Create both; meet the stat requirement; let a **Monday** rollover pass | The challenge's `perkLevel` is bumped by `processRPGRollover` (it treats *any* task with `statRequirements` as a perk), which makes the hooked perk think the challenge is "unlocked" before it's completed. (See `potential_bugs.md` #2.) |

### E. RPG / counter / dependency interactions
| # | Set these options | How to force it | Watch for |
|---|---|---|---|
| E1 | ✅ (regression) Counter card hooked to a Perk | Create a counter task (Target Count > 0), hook it as a Perk dependency | Perk must stay **locked** until the counter hits its max (fixed via `ProgressionService.isDependencyUnlocked`; covered by tests). |
| E2 | ⚠️ Perk hooked **only** to other tasks (no stat requirements) | Create a perk whose only requirement is a hooked, already-finished task | It unlocks at Level 1 but **never** levels to 5, because `processRPGRollover` only advances perks that have `statRequirements`. |
| E3 | ⚠️ `Allow Repeating Tasks` + `Enable Custom Stats` + stat rewards, **no** target | Repeating task with XP rewards and no Target Count | Each `+` grants XP forever (intended "farming"), but verify it can't be hooked to gate a perk (a no-target counter is never "at max"). |

---

## 3. Suggested fastest repro setup

1. Create a throwaway profile/back up `%APPDATA%\TaskTracker\` first (`stats.json`, `tasks.json`).
2. Keep `%APPDATA%\TaskTracker\stats.json` open in an editor — most "impossible via UI" combinations
   (two page modes, conflicting feature toggles) are trivial to force by editing the JSON booleans
   directly, then reopening the app.
3. To exercise rollover/streak/atrophy bugs, change the **Windows system date** forward and relaunch
   (the app keys off `LocalDate.now()` vs `lastOpenedDate`). Use Mondays for perk-level tests.
4. After each test, watch `stderr`/console for stack traces — several of these manifest as a section
   that simply fails to render rather than a popup error.
