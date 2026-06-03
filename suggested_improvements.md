# Suggested Improvements

A running list of concrete, high-value changes for the ORS-To-Do code base. Items are
roughly ordered by impact-to-effort. Several were surfaced while implementing the
`Lang`/`LangManager`, `Design`, and challenge/perk refactors.

---

## 1. Finish the `Lang` / `LangManager` migration across the whole app

The new `com.raeden.ors_to_do.i18n.Lang` enum + `LangManager` now hold the card subsystem's
copy (Challenge, Perk, Repeatable cards and their dialogs). The rest of the UI still hard-codes
strings inline — notably the settings panels (`GeneralSettingsPanel`, `StatsManagerPanel`,
`DangerZonePanel`, …), the services (`BackupManager`, `NotificationManager`,
`DailyRolloverManager`), and the help/credits/about dialogs in `TaskDialogs`.

**Action:** sweep the remaining files and replace every literal sentence with a `Lang` key.
Once everything routes through one place, add a `locale` field to `LangManager` so the same keys
can resolve to different languages — true localization becomes a config switch rather than a rewrite.

## 2. Extract the shared Perk/Challenge config-dialog code

`PerkCard.openPerkConfigDialog` and `ChallengeConfigDialog.open` are ~80% identical: the
icon/background/outline color pickers, the "🎲 Randomize Style" button, the stat-requirement
adder, and the "Hook Tasks / Challenges" dependency menu are duplicated almost verbatim (and
`toHexString` is copy-pasted into at least three classes).

**Action:** pull these into reusable builders, e.g. a `CardStyleSection`, `StatHookSection`, and
`DependencyMenuBuilder` under `ui/forms` or `ui/components`, plus a single `ColorUtil.toHex`.
This removes hundreds of duplicated lines and guarantees the two editors stay in sync.

## 3. Move game logic out of view constructors into a testable service layer

`PerkCard`'s constructor evaluates unlock requirements **and mutates the model** (sets
`perkUnlockedDate`/`perkLostDate`/`perkLevel`) and calls `StorageManager.saveTasks(...)` as a
side effect of *rendering*. Computing-and-persisting state during UI construction is fragile and
order-dependent, and it's exactly why the counter-card unlock bug (now fixed in
`TaskLinkUtil.isDependencyUnlocked`) was hard to spot.

**Action:** introduce a `PerkService` / `ProgressionService` that owns "is this unlocked?" and
"recompute unlock state" as pure, unit-testable methods. Cards should only read the result and draw.

## 4. Add automated tests — starting with progression/dependency rules

There is currently no test source set at all. The counter→perk bug would have been caught by a
single unit test asserting that a counter card at `currentCount < maxCount` does **not** satisfy a
dependent perk.

**Action:** add JUnit 5 under `src/test/java`, and cover `isDependencyUnlocked`, the stat-threshold
checks, deadline/expiry logic, and the daily-rollover behavior. Wire `mvn test` into the build.

## 5. Replace Java serialization with JSON persistence

Models implement `Serializable` with a fixed `serialVersionUID`. Java serialization is brittle:
renaming/moving a class or changing field shapes can silently break loading of existing user data,
and the `.dat` files are opaque (hard to back up, diff, or hand-repair).

**Action:** migrate `StorageManager` to a JSON format (Gson or Jackson). Benefits: refactor-safe
field evolution, human-readable backups (`BackupManager`/`BackupBundle` already exist), and easier
import/export. Provide a one-time migration that reads the old serialized format and rewrites JSON.

## 6. Keep splitting classes that are trending toward "god class" status

`ChallengeCard` was split (631 → 340 + a 340-line dialog) to stay under the 500-line budget. A few
others are approaching the line and mix several responsibilities:

- `PomodoroTimer` (459) — timer state machine + UI + notifications.
- `AppStats` (393) — a data model that has grown into a grab-bag of unrelated settings.
- `FilterSortHeader` (362), `GeneralSettingsPanel` (362), `TemplateEditDialog` (361).

**Action:** adopt a soft 300-line / single-responsibility guideline for UI classes and split when a
class starts owning both layout and behavior. `AppStats` in particular would benefit from grouping
its fields into cohesive sub-objects (e.g. `DisplaySettings`, `RpgSettings`).

## 7. Enforce the counter card's maximum in the UI

`TaskItem` has `maxCount`, and `TaskActionHandler` finishes a counter task when
`currentCount >= maxCount`, but `RepeatableTaskCard`'s `+` button increments with no visible target
and no stop at the cap. Now that counter completion gates perk unlocks, the user can't easily see
progress.

**Action:** show `currentCount / maxCount` (a small progress bar would be ideal) on the repeatable
card, disable/relabel the `+` button at max, and make the "at max = done" state obvious.

---

### Already addressed in this pass
- Centralized card/dialog copy in `Lang` + `LangManager`.
- New `Design` class as the single home for long-string / confirmation dialogs.
- Split `ChallengeCard` below the 500-line god-class threshold (`ChallengeConfigDialog`).
- Added a **Fail Challenge** button alongside **Challenge Done**.
- Fixed counter-card → perk unlock (`TaskLinkUtil.isDependencyUnlocked`).
