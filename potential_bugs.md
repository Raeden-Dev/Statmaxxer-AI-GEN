# Potential Bugs Catalogue

Each entry is a specific defect or fragile spot I found while reading the code base. The
**Trigger** line is the shortest path I could find to reproduce it; the **Source** line points to
the code that needs attention. None of these are theoretical — they're all visible in the current
source.

The companion guide [`section_bug_hunting.md`](section_bug_hunting.md) describes which **option
combinations** to exercise per-section; this file lists the **concrete defects** to look for.

---

## 1. Crash when only one custom priority exists 🟥 high

**Symptom.** Opening any normal section after deleting priorities down to a single entry throws
`IndexOutOfBoundsException: Index 1 out of bounds for length 1` and the section fails to build.

**Source.** `DynamicInputPanel` line 63:
```java
if (!appStats.getCustomPriorities().isEmpty()) priorityBox.setValue(appStats.getCustomPriorities().get(1));
```
The empty-check passes for size == 1, then `.get(1)` blows up.
The Priority Manager explicitly allows reducing the list down to 1
(`PriorityManagerPanel` lines 131–135).

**Trigger.**
1. Settings → Priority Manager.
2. Delete priorities one by one until exactly one remains (the "must have at least one" guard kicks in here).
3. Open or switch to any section that has `Show Priority` enabled and isn't a Notes/Perk page.
4. The section fails to render.

**Fix sketch.** Use index 0 (or `Math.min(1, size-1)`), since the original `1` was just "default to
the middle priority".

---

## 2. Challenges with stat-requirements get auto-levelled as perks 🟥 high

**Symptom.** A Challenge that has any `statRequirements` set will, on a Monday rollover, have its
`perkLevel` bumped (up to 5) and notifications fire ("Perk Leveled Up!"). A Perk hooked to such a
challenge then sees `getPerkLevel() > 0` and counts it as **unlocked** before the user has
actually clicked **Challenge Done**.

**Source.** `DailyRolloverManager.processRPGRollover` (lines 177–217) iterates every task with
`statRequirements` and treats them all as perks — no `isChallengeCard()` filter.
`ProgressionService.isDependencyUnlocked` short-circuits to true on `getPerkLevel() > 0`.

**Trigger.**
1. Create a custom stat (Stat Page).
2. On a Challenge Page, create a challenge "Run 5km" with unlock requirement = `Strength ≥ 10`.
3. On a Perk Page, create a perk hooked to that challenge.
4. Raise Strength to 10. Change the system date to a Monday and relaunch.
5. The perk shows as Unlocked even though the challenge was never marked done.

**Fix sketch.** In `processRPGRollover`, skip tasks where `isChallengeCard()` is true (challenges
shouldn't level), and in `ProgressionService.isDependencyUnlocked`, for an `isChallengeCard` dep,
require `isFinished() && getPerkUnlockedDate() != null` (i.e. completed *and* not failed).

---

## 3. Failed challenges still satisfy hooked dependencies 🟧 medium

**Symptom.** A challenge marked **Fail Challenge** (added in the last pass) has
`isFinished() == true`. Anything hooked to it then counts as satisfied.

**Source.** `ProgressionService.isDependencyUnlocked` returns true on `depTask.isFinished()`, which
the new fail-path also sets (`ChallengeCard.java` fail action: `setFinished(true)`).

**Trigger.**
1. Create challenge A and perk P that depends on A.
2. Click **Fail Challenge** on A and confirm.
3. The perk unlocks.

**Fix sketch.** Treat a challenge as "passed" only when finished **and** `perkUnlockedDate != null`
(failed challenges have `perkUnlockedDate == null` / `perkLostDate != null`).

---

## 4. Daily rollover archives & finishes counter / repeating tasks 🟧 medium

**Symptom.** On a section with `Reset Interval > 0`, all tasks — including counter cards that
hadn't reached their target and repeating "farm" cards — are archived **and** flipped to finished
on rollover. Progress (`currentCount`, `repetitionCount`) is effectively lost.

**Source.** `DailyRolloverManager.processDailyRollover` lines 59–64:
```java
for (TaskItem task : taskDatabase) {
    if (section.getId().equals(task.getSectionId()) && !task.isArchived()) {
        task.setArchived(true);
        if (task.getDateCompleted() == null) task.setFinished(true);
    }
}
```
Note that `autoArchiveTasks` (used elsewhere) already excludes `isCounterMode()` — the daily
rollover does not.

**Trigger.**
1. Create a section with `Reset Interval = 24` hours.
2. Add a counter task with Target Count = 10, increment it twice.
3. Roll the system clock forward a day, relaunch.
4. The counter is archived and the perk hooked to it now thinks "finished" satisfies it (compounds
   with #3).

**Fix sketch.** In the rollover archive loop, skip `isCounterMode()` and `isRepeatingMode()` (or
respect `isPermaLock`), matching `autoArchiveTasks` semantics.

---

## 5. Perk state-mutation runs during view construction 🟧 medium

**Symptom.** Just **looking at** a perk page can change perk state and write to disk. Refreshing
the list, or two PerkCards being constructed for the same task during the same render, can
re-stamp `perkUnlockedDate`/`perkLostDate` to "now" repeatedly. The lost-date displayed to the user
is not the time the perk was actually lost; it's the time you last opened the page after losing it.

**Source.** `PerkCard` constructor still calls `ProgressionService.recomputePerkState(...)` and
`StorageManager.saveTasks(...)` as a side effect of rendering (lines 89–95 of the updated file).
The pure transition is now in `ProgressionService`, but it's still invoked from the view.

**Trigger.**
1. Unlock a perk (date stamped at, say, 10:00).
2. Let it lose requirements.
3. Open the Perk page at 11:00 → `perkLostDate = 11:00`.
4. Open it again at 12:00 — `perkLostDate` does *not* change, but if you re-gain & re-lose, the
   unlock/lost timestamps drift on every view.

**Fix sketch.** Move the `recomputePerkState`/`saveTasks` call into the daily-rollover or an
app-tick path; let the card only **read** the model.

---

## 6. Challenge-page input flags the wrong task as a challenge 🟧 medium

**Symptom.** Typing a new challenge name and clicking Add can mark the **wrong** task as a
challenge when another section's input has appended to the global list a moment earlier.

**Source.** `DynamicModule` lines 92–99:
```java
if (config.isChallengePage() && !globalDatabase.isEmpty()) {
    TaskItem newest = globalDatabase.get(globalDatabase.size() - 1);
    if (newest.getSectionId() != null && newest.getSectionId().equals(config.getId())) {
        newest.setChallengeCard(true);
        ...
    }
}
```
"The newest task in the global list" is assumed to be the one we just added; that's fragile.
`DynamicInputPanel.addTask` appends then triggers the refresh callback, but `forceSortMode`
fires a separate refresh that also runs this block — race-prone.

**Trigger.** Most reliably: open two windows or add a daily-rollover-generated task into the same
list between Input and refresh. In practice the section-id guard saves it most of the time, but
the design is wrong.

**Fix sketch.** Have `DynamicInputPanel` accept a post-add callback that takes the newly-created
`TaskItem` and pass it explicitly to the challenge-flagging code (no "last item" lookup).

---

## 7. Challenge "expired" state is computed but never persisted 🟨 low

**Symptom.** A challenge whose deadline has passed shows "Expired!" and disables the Complete
button (because `meetsRequirements` is false), but its `isFinished` / `perkLostDate` are not set.
If the section is on a `Reset Interval`, rollover **will** finish it (bug #4) — silently and as a
"success", since `perkUnlockedDate` stays null and only `isFinished` flips.

**Source.** `ChallengeCard` lines 76–79 mark `isExpired` and feed it into `meetsRequirements`, but
nothing writes anything back. Bug #4's blanket archive then promotes it.

**Trigger.** Create a challenge with a 5-minute deadline on a daily-reset section, do nothing, wait
for both deadline + next-day rollover.

**Fix sketch.** On expiry detection, treat the same as Fail Challenge (set finished + lost date),
then skip in rollover (#4 fix).

---

## 8. `notified*h` flags reset on **every** deadline edit 🟨 low

**Symptom.** Re-saving a task with the same deadline still re-arms its notifications, so users get
duplicate "X hours left" pings any time they edit anything that ends up calling
`setDeadline(existing)`.

**Source.** `TaskItem.setDeadline` (lines 139–145):
```java
public void setDeadline(LocalDateTime deadline) {
    this.deadline = deadline;
    this.notified24h = false; this.notified12h = false;
    this.notified4h = false;  this.notified2h = false;
}
```
There's no "did the value actually change?" guard.

**Trigger.** Open Challenge or Task edit dialog, change nothing, click OK. The dialog's save path
calls `setDeadline(oldDeadline)`. Next notification scan re-fires the highest threshold.

**Fix sketch.** Only reset flags when `!Objects.equals(this.deadline, deadline)`.

---

## 9. Stat atrophy date math advances by exactly 1 day per atrophy event 🟨 low

**Symptom.** A stat that has been idle for, say, 10 days only loses **1** point on the next
rollover, even if `atrophyDays == 1`. The user expects "10 atrophy events worth" of decay.

**Source.** `DailyRolloverManager.processRPGRollover` lines 156–172. The block runs once per
launch; `lastGain` is bumped by exactly `+1` day, so multi-day catch-up isn't applied.

**Trigger.** Set a stat's `atrophyDays = 1`. Raise its current amount to 10. Close the app, change
the clock +10 days, relaunch. Stat decays from 10 → 9, not 10 → 0.

**Fix sketch.** Replace the single-shot decrement with a loop:
`while (daysSinceGain >= atrophyDays) { decrement; lastGain += atrophyDays; daysSinceGain -= atrophyDays; }`.

---

## 10. Challenge deadline parser silently accepts garbage 🟨 low

**Symptom.** Typing a bad time like `25:99` into the challenge editor saves the deadline as
**midnight** of the chosen date with no warning. The user thinks the late time stuck.

**Source.** `ChallengeConfigDialog` deadline save (`catch (Exception ex) { setDeadline(LocalDateTime.of(datePicker.getValue(), LocalTime.MIDNIGHT)); }`).
Same pattern lives in the Task edit form.

**Trigger.** Challenge config → date picked → time field `25:99` → OK. Reopen the challenge; you'll
see 00:00.

**Fix sketch.** Surface the parse failure via `Design.warn(...)`, or guard the OK button until the
time string parses.

---

## 11. `SectionEditDialog` reads `customPriorities.get(0)` indirectly via fallback prio 🟨 low

**Symptom.** `DynamicInputPanel.addTask` uses `appStats.getCustomPriorities().get(0)` as a fallback
when `priorityBox` exists but its value is null, but for Notes/Perk pages it skips priority
entirely. Edge case: a section with `Show Priority` on but **zero** custom priorities (e.g. the
last one was removed by a future rules update) — the `addTask` path silently constructs a task
with `priority = null`, which then renders as a blank/garbage priority chip.

**Source.** `DynamicInputPanel.addTask` lines 82–86, combined with the Priority Manager allowing
the count to dip — currently can't reach zero from the UI, but JSON edits can.

**Trigger.** Set `customPriorities: []` in `stats.json`, relaunch, add a task.

**Fix sketch.** Render `null` priority as a styled "—" and short-circuit
`config.isShowPriority() && customPriorities.isEmpty()` to skip the chip entirely.

---

## 12. Drag-reorder updates `globalDatabase` but not the dialog's selected-deps list 🟨 low

**Symptom.** Edge case in the editor dialogs: while the Perk/Challenge editor is open, if another
window/refresh removes one of the hooked dependency tasks (e.g. via context-menu delete), the
`selectedDeps` list still contains the dead id. OK then saves an invalid hook id.

**Source.** `DependencyMenuBuilder.build` constructs the menu once at dialog open; nothing watches
the `globalDatabase` for changes. PerkCard/ChallengeConfigDialog then write `selectedDeps` back
verbatim.

**Trigger.** Open the Perk edit dialog while a delete confirm from the Challenge page is still
focused (rare UX, but possible).

**Fix sketch.** Before saving, filter `selectedDeps` to ids actually present in the live database.

---

## How to triage these

1. **#1, #2, #3, #4** are user-visible defects in the perk/challenge progression system — fix
   together to avoid re-introducing each other.
2. **#5** is a maintainability landmine more than a daily bug; fix when refactoring the rollover.
3. **#6, #7, #8, #10, #11, #12** are small footguns. Cover with the new test scaffolding
   (`ProgressionServiceTest` style) when fixing.
4. **#9** is only visible to users who change their system clock or close the app for long
   stretches — fix opportunistically.
