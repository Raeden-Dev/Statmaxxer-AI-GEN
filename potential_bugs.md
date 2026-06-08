# Potential Bugs Catalogue

Each entry is a specific defect or fragile spot I found while reading the code base. Items marked
**✅ FIXED** were resolved in the latest pass; the rest is what remains. The companion guide
[`section_bug_hunting.md`](section_bug_hunting.md) describes which **option combinations** to
exercise per-section.

---

## ✅ FIXED

### 1. Crash when only one custom priority exists 🟥
Fixed in `DynamicInputPanel:63` — index now clamped to `Math.min(1, size - 1)`. Adding tasks with
a single priority defined no longer throws `IndexOutOfBoundsException`.

### 2. Challenges with stat-requirements get auto-levelled as perks 🟥
Fixed in `DailyRolloverManager.processRPGRollover` — `task.isChallengeCard()` is now skipped, so
challenges never receive a phantom `perkLevel` on Monday rollover.

### 3. Failed challenges still satisfy hooked dependencies 🟧
Fixed in `ProgressionService.isDependencyUnlocked` — challenge dependencies now require
`isFinished() && perkUnlockedDate != null`. A "Fail Challenge" outcome leaves `perkUnlockedDate`
null, so dependents stay locked. Covered by `ProgressionServiceTest`.

### 4. Daily rollover archives & finishes counter / repeating tasks 🟧
**Superseded by an explicit redesign (see below).** The original silent "archive + mark finished
everything" bug is gone; the reset sweep is now a deliberate, documented behavior:
- **Completed** cards (including counters at their target) → archived (rewards were already applied
  at completion time, so they're not re-applied).
- **Incomplete** cards (including counters below target and repeaters) → the task's miss penalty is
  applied (`TaskActionHandler.applyMissPenalty`: subtract Penalty Points + apply the Stat Penalties
  map), then the card is **deleted**.
- Result: a resettable page always returns to exactly the count its templates produce, instead of
  stacking leftover cards. The `statPenalties` map — previously displayed but never applied to any
  stat — is now wired into both the reset sweep and the deadline-miss handler.

### 7. Expired challenge state is computed but never persisted 🟨
Fixed in `ChallengeCard` — when a deadline passes without completion, the card now persists the
challenge as Failed (sets `isFinished=true`, `perkLostDate=now`, `permaLock=true`) so subsequent
checks see the right state.

### 8. `notified*h` flags reset on every deadline edit 🟨
Fixed in `TaskItem.setDeadline` — flags only reset when the deadline value actually changes
(`Objects.equals` guard).

### 9. Stat atrophy advances by exactly 1 day per atrophy event 🟨
Fixed in `processRPGRollover` — the atrophy decrement now loops until caught up, so a stat that
sat idle for 10 days with `atrophyDays=1` will lose all 10 points on the next launch.

### 10. Challenge deadline parser silently accepts garbage 🟨
Fixed in `ChallengeConfigDialog` — an unparseable time string now surfaces a `Design.warn(...)`
explaining that the value was rejected and saved as 00:00.

### 6. Challenge-page input flags the wrong task as a challenge 🟧
Fixed in `DynamicInputPanel.addTask` — the new task's `isChallengeCard` flag is set in the same
spot the task is created. `DynamicModule` no longer scans for "newest in DB", removing the race.

### 12. Stale dep ids after concurrent deletion 🟨
Fixed in `DependencyMenuBuilder.stripStale(...)` — Perk and Challenge editor save paths now
filter the selected list against the live task DB before persisting, so dead ids can't be saved.

---

## 🟡 Remaining

### 5. Perk state-mutation runs during view construction 🟧 medium
**Symptom.** `PerkCard` still calls `ProgressionService.recomputePerkState(...)` and
`StorageManager.saveTasks(...)` during its constructor. The pure transition is now testable but
the side-effect is still order-sensitive. Rendering twice in the same tick can fire two
transitions if the model is exactly at the threshold.

**Source.** `PerkCard` lines 87–93.

**Trigger.** Most easily seen when toggling between sections rapidly while a stat sits exactly at
a perk threshold; `refreshList()` re-creates the cards, re-evaluates the rule, and the saved
timestamps drift.

**Fix sketch.** Move the `recomputePerkState`/`saveTasks` call into the daily-rollover or an
app-tick path; let the card only **read** the model.

### 11. Blank-priority edge case (JSON-edited empty list) 🟨 low
**Symptom.** Setting `customPriorities: []` in `stats.json` and relaunching makes `addTask`
construct a task with `priority = null`. The UI currently renders that as a blank/garbage chip
in some renderers.

**Source.** `DynamicInputPanel.addTask` (fallback at line ~84), combined with the renderer's
priority chip code in `TaskCard.buildMetaBox`.

**Fix sketch.** Skip the chip entirely when `priority == null`; add a short-circuit in the input
panel that disables the priority dropdown when the list is empty.

---

## How to triage what's left

1. **#5** is the only behaviour bug left — move it as part of the larger "lift game logic out of
   view constructors" refactor described in `suggested_improvements.md` #9.
2. **#11** is only reachable via direct JSON edits and is cosmetic — fix opportunistically while
   touching the input panel.
