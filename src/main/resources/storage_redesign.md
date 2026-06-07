# Storage Redesign — Statmaxxer / TaskTracker

A proposal to replace the single `tasks.json` / `stats.json` scheme with a SQLite-backed
document store. Goal: end full-file rewrites, remove corruption risk, keep load fast as data
grows past 5,000 records — while changing as little of the existing code as possible.

---

## Why the current setup hurts

`StorageManager` keeps one `List<TaskItem>` in `tasks.json` and one `AppStats` object in
`stats.json`, both pretty-printed, both rewritten in full on nearly every interaction.

The pain comes from four things stacking:

1. **Pretty-printing** puts every field and array element on its own line — roughly a 10x line
   multiplier. Most of your "60k lines" is whitespace, not data.
2. **`TaskItem` is a god-object** (~50 fields). Empty tasks still serialize dozens of `false`,
   `null`, `{}`, `[]` entries because Gson writes defaults.
3. **Archived tasks never leave the hot file.** Completed/archived items live in the same list
   you rewrite on every checkbox toggle, so the file only ever grows.
4. **Every change rewrites the whole list** (plus rotates three `.bak` copies). `AppStats` also
   accumulates unbounded `historyLog`, `advancedHistoryLog`, and `deletedTaskHistory`.

---

## The scheme: a document store on top of SQLite

One file, `tasktracker.db`, in `%APPDATA%/TaskTracker`. Each task is a **row**, not a slice of a
giant blob. The trick that makes this fit a fast-evolving model: **the full `TaskItem` is stored
as a JSON payload column (the source of truth), with a handful of denormalized columns copied out
purely for fast querying and sorting.**

Why this pattern specifically: your model changes constantly (the prompt history shows new
`TaskItem` fields every version). A rigid relational schema would force a schema migration every
time you add a field. With a JSON payload, new fields land automatically — you only add a real
column when you want to *query* by that field. You get database robustness without database
rigidity.

### Tasks table

```sql
CREATE TABLE tasks (
  id            TEXT PRIMARY KEY,
  section_id    TEXT,
  text_content  TEXT,
  is_finished   INTEGER NOT NULL DEFAULT 0,
  is_archived   INTEGER NOT NULL DEFAULT 0,
  is_favorite   INTEGER NOT NULL DEFAULT 0,
  is_pinned     INTEGER NOT NULL DEFAULT 0,
  date_created  TEXT,
  date_completed TEXT,
  deadline      TEXT,
  reward_points INTEGER NOT NULL DEFAULT 0,
  payload       TEXT NOT NULL          -- full TaskItem serialized via your existing Gson instance
);

CREATE INDEX idx_tasks_section  ON tasks(section_id);
CREATE INDEX idx_tasks_archived ON tasks(is_archived);
CREATE INDEX idx_tasks_deadline ON tasks(deadline);
CREATE INDEX idx_tasks_pinned   ON tasks(is_pinned);
```

The columns are just whatever you filter or sort by in the UI (section grouping, archive split,
deadline countdowns, pinned-on-top). Everything else — colors, perks, counters, the five stat
maps, subtasks, links, dependencies, notification flags — lives inside `payload` and never needs
its own column unless you start querying it.

On save you serialize the whole task to `payload` and copy the indexed fields out. On load you
deserialize `payload` back into a `TaskItem` (the columns are throwaway derivatives).

### App config + history (replacing stats.json)

`AppStats` is really config-plus-logs. Split the bounded config from the unbounded logs so the
logs stop bloating the object you load on every launch:

```sql
-- single-row config blob: AppStats minus the big logs
CREATE TABLE app_meta (
  id      INTEGER PRIMARY KEY CHECK (id = 1),
  payload TEXT NOT NULL
);

-- date-keyed, trimmable
CREATE TABLE history_log (
  day        TEXT PRIMARY KEY,   -- ISO LocalDate
  completion REAL
);

CREATE TABLE advanced_history_log (
  day     TEXT PRIMARY KEY,
  payload TEXT                   -- the int[] as JSON
);

-- append-only, cap to last N on write
CREATE TABLE deleted_task_history (
  rowid INTEGER PRIMARY KEY AUTOINCREMENT,
  entry TEXT,
  ts    TEXT
);

CREATE TABLE schema_version (version INTEGER NOT NULL);
```

Sections, custom stats, priorities, debuff templates, base dailies stay inside the `app_meta`
payload — they're small, bounded config.

---

## The new StorageManager API

Keep the existing method names working so nothing breaks on day one, then move hot paths to
per-row writes.

```java
// drop-in replacements (Phase 1) — same signatures the app already calls
List<TaskItem> loadTasks();              // SELECT * (active + archived), deserialize payloads
void           saveTasks(List<TaskItem> tasks);  // one transaction: upsert all, delete missing
AppStats       loadStats();
void           saveStats(AppStats stats);

// new, surgical writes (Phase 2) — call these from checkbox/edit/add/archive handlers
void upsertTask(TaskItem t);             // one row, one statement
void deleteTask(String id);

// lazy slices (Phase 3) — stop loading the archive at startup
List<TaskItem> loadActiveTasks();        // WHERE is_archived = 0
List<TaskItem> loadArchivedTasks();      // WHERE is_archived = 1, called only when Archive opens
List<TaskItem> loadTasksBySection(String sectionId);
```

The win: toggling a checkbox becomes one `UPDATE` of one row instead of re-serializing thousands
of tasks and rotating three backup files.

### Connection settings (durability)

```sql
PRAGMA journal_mode = WAL;        -- crash-safe, readers don't block the writer
PRAGMA synchronous  = NORMAL;     -- good balance; use FULL if you want maximum paranoia
PRAGMA foreign_keys = ON;
```

Open one long-lived connection for the app session rather than per-write.

---

## Corruption safety and backups

This directly answers the "what if the single file corrupts" worry — and SQLite is far harder to
corrupt than a JSON blob you overwrite by hand:

- **WAL + transactions** mean a crash mid-write rolls back cleanly instead of leaving a
  half-written file.
- **Consistent snapshots** via SQLite's Online Backup API (supported by `sqlite-jdbc`): on app
  start, or once a day, copy the live DB to `backups/tasktracker_YYYYMMDD.db` and keep the last N.
  Each snapshot is itself a complete, openable database — not a fragile partial text file.
- **Keep `BackupManager`'s JSON export** for portable, human-readable backups and as your
  "I want to eyeball/edit it" escape hatch. Make internal/auto serialization **compact** (drop
  `setPrettyPrinting()`); keep pretty-printing only for the explicit user-triggered export.

So: SQLite is the working store, rotating `.db` snapshots are the safety net, JSON export is the
portable/readable format.

---

## Migration (first launch on the new version)

Idempotent and reversible — never deletes your old data:

1. Open/create `tasktracker.db`. If `schema_version` is missing, create the schema and stamp it.
2. If `tasks.json` / `stats.json` (or legacy `.dat`) exist **and** the `tasks` table is empty,
   import: load them through your *existing* Gson + legacy-`.dat` paths, then batch-upsert into the
   DB inside one transaction.
3. Rename the old files to `tasks.json.imported` / `stats.json.imported` (keep, don't delete).
4. Done — subsequent launches read straight from the DB.

The only new dependency is `org.xerial:sqlite-jdbc` (pure Java, bundles its own native libs, no
installer). One line in your `pom.xml` / `build.gradle`.

---

## Rollout in phases

**Phase 1 — Swap the backend, keep the API.** Add the SQLite layer + schema + the migration
importer behind the existing `saveTasks/loadTasks/saveStats/loadStats` names. Zero UI changes.
You immediately get robustness, a tiny file, and the end of pretty-print bloat.

**Phase 2 — Per-row writes.** Point the checkbox, edit, add, and archive handlers at
`upsertTask` / `deleteTask`. This is what actually kills the full-rewrite cost.

**Phase 3 — Lazy archive.** Load only active tasks at startup; fetch archived tasks when the
Archive page opens. This is what keeps startup fast as you push past 5,000 records.

**Phase 4 — Backup cutover.** Replace the JSON `.bak1/.bak2/.bak3` rotation with `.db` snapshots
via the Online Backup API; keep JSON export in `BackupManager`.

---

## How this maps to your three pains

- *"Everything cramped in one file, hard to read/write"* — tasks become rows you can open in a
  free tool like **DB Browser for SQLite** and edit one at a time in a table view, instead of
  scrolling a 60k-line blob.
- *"Worried about corruption/loss"* — transactional writes, WAL, and consistent `.db` snapshots,
  plus the JSON export as a second independent copy.
- *"Might load slower with more data"* — indexed queries and a lazily-loaded archive; 5,000 rows
  is trivial for SQLite and the same design scales comfortably to 100,000+.
