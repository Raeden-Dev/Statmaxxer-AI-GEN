package com.raeden.ors_to_do.dependencies.storage.sqlite;

import com.raeden.ors_to_do.dependencies.models.TaskItem;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Tasks table CRUD.
 *
 * <p>Implements every Phase the storage redesign calls for:</p>
 * <ul>
 *   <li><b>Phase 1</b> — {@link #loadAll(Db)} / {@link #saveAll(Db, List)} preserve the existing
 *   "rewrite the whole list" semantics by doing a single transactional upsert-all + delete-missing.
 *   That's how {@code StorageManager.saveTasks(...)} keeps the legacy API working on day one.</li>
 *   <li><b>Phase 2</b> — {@link #upsert(Db, TaskItem)} / {@link #delete(Db, String)} are the
 *   surgical one-row writes the redesign wants checkbox/edit/add/archive handlers to migrate to,
 *   removing the full-rewrite cost.</li>
 *   <li><b>Phase 3</b> — {@link #loadActive(Db)} / {@link #loadArchived(Db)} /
 *   {@link #loadBySection(Db, String)} are the lazy slices used to stop loading the entire
 *   archive at startup.</li>
 * </ul>
 *
 * <p>Every method synchronizes on {@code db.lock()} so the single {@link Connection} stays
 * safe to share across the UI's threads.</p>
 */
public final class TaskRepository {

    private TaskRepository() { }

    /** Loads every task (active + archived) — the Phase-1 drop-in replacement for legacy load. */
    public static List<TaskItem> loadAll(Db db) throws SQLException {
        return loadWhere(db, null);
    }

    /** Active tasks only. Cheap startup load — what Phase 3 routes the UI to. */
    public static List<TaskItem> loadActive(Db db) throws SQLException {
        return loadWhere(db, "is_archived = 0");
    }

    /** Archived tasks only. Fetched lazily when the Archive page opens. */
    public static List<TaskItem> loadArchived(Db db) throws SQLException {
        return loadWhere(db, "is_archived = 1");
    }

    /** Tasks belonging to a single section. Indexed scan via {@code idx_tasks_section}. */
    public static List<TaskItem> loadBySection(Db db, String sectionId) throws SQLException {
        synchronized (db.lock()) {
            String sql = "SELECT payload FROM tasks WHERE section_id = ?";
            try (PreparedStatement ps = db.connection().prepareStatement(sql)) {
                ps.setString(1, sectionId);
                return readPayloads(ps);
            }
        }
    }

    /**
     * Replaces the entire tasks table with {@code tasks} in a single transaction: upserts every
     * supplied row, then deletes any row whose id wasn't in the list. Preserves legacy
     * "save the whole list" semantics so the higher-level facade can drop in without callers
     * changing.
     */
    public static void saveAll(Db db, List<TaskItem> tasks) throws SQLException {
        synchronized (db.lock()) {
            Connection c = db.connection();
            boolean prevAutoCommit = c.getAutoCommit();
            c.setAutoCommit(false);
            try {
                // 1. Upsert every task in the supplied list.
                try (PreparedStatement up = c.prepareStatement(upsertSql())) {
                    for (TaskItem t : tasks) bind(up, t);
                }

                // 2. Delete any row whose id is no longer in the supplied list. We build the set
                //    of kept ids upfront and use a single DELETE with NOT IN, batched in chunks
                //    to stay well under SQLite's 999-parameter limit on the off chance someone is
                //    running with > 999 tasks.
                Set<String> keepIds = new HashSet<>();
                for (TaskItem t : tasks) keepIds.add(t.getId());
                deleteAllExcept(c, keepIds);

                c.commit();
            } catch (SQLException e) {
                c.rollback();
                throw e;
            } finally {
                c.setAutoCommit(prevAutoCommit);
            }
        }
    }

    /** Surgical one-row write — Phase 2's payoff. */
    public static void upsert(Db db, TaskItem task) throws SQLException {
        if (task == null || task.getId() == null) return;
        synchronized (db.lock()) {
            try (PreparedStatement ps = db.connection().prepareStatement(upsertSql())) {
                bind(ps, task);
            }
        }
    }

    /** Surgical one-row delete. */
    public static void delete(Db db, String taskId) throws SQLException {
        if (taskId == null) return;
        synchronized (db.lock()) {
            try (PreparedStatement ps = db.connection().prepareStatement("DELETE FROM tasks WHERE id = ?")) {
                ps.setString(1, taskId);
                ps.executeUpdate();
            }
        }
    }

    /** Count of rows — handy for the importer's "is the table empty?" check. */
    public static int count(Db db) throws SQLException {
        synchronized (db.lock()) {
            try (Statement st = db.connection().createStatement();
                 ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM tasks")) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    // ---------- internals ----------

    private static List<TaskItem> loadWhere(Db db, String whereClauseOrNull) throws SQLException {
        synchronized (db.lock()) {
            String sql = "SELECT payload FROM tasks" + (whereClauseOrNull == null ? "" : " WHERE " + whereClauseOrNull);
            try (PreparedStatement ps = db.connection().prepareStatement(sql)) {
                return readPayloads(ps);
            }
        }
    }

    private static List<TaskItem> readPayloads(PreparedStatement ps) throws SQLException {
        List<TaskItem> out = new ArrayList<>();
        try (ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                String json = rs.getString(1);
                TaskItem t = GsonProvider.compact().fromJson(json, TaskItem.class);
                if (t != null) out.add(t);
            }
        }
        return out;
    }

    private static String upsertSql() {
        return "INSERT INTO tasks(id, section_id, text_content, is_finished, is_archived, is_favorite, is_pinned, " +
                "date_created, date_completed, deadline, reward_points, payload) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) " +
                "ON CONFLICT(id) DO UPDATE SET " +
                "section_id=excluded.section_id, text_content=excluded.text_content, " +
                "is_finished=excluded.is_finished, is_archived=excluded.is_archived, " +
                "is_favorite=excluded.is_favorite, is_pinned=excluded.is_pinned, " +
                "date_created=excluded.date_created, date_completed=excluded.date_completed, " +
                "deadline=excluded.deadline, reward_points=excluded.reward_points, payload=excluded.payload";
    }

    private static void bind(PreparedStatement ps, TaskItem t) throws SQLException {
        ps.setString(1, t.getId());
        ps.setString(2, t.getSectionId());
        ps.setString(3, t.getTextContent());
        ps.setInt(4, t.isFinished() ? 1 : 0);
        ps.setInt(5, t.isArchived() ? 1 : 0);
        ps.setInt(6, t.isFavorite() ? 1 : 0);
        ps.setInt(7, t.isPinned() ? 1 : 0);
        ps.setString(8, t.getDateCreated() == null ? null : t.getDateCreated().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        ps.setString(9, t.getDateCompleted() == null ? null : t.getDateCompleted().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        ps.setString(10, t.getDeadline() == null ? null : t.getDeadline().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        ps.setInt(11, t.getRewardPoints());
        ps.setString(12, GsonProvider.compact().toJson(t));
        ps.executeUpdate();
    }

    /**
     * Deletes every task whose id is not in {@code keepIds}. SQLite caps prepared statement
     * parameters at 999, so when the kept set is small we use a NOT-IN list with binds; for
     * larger sets we fall back to a temp-table dance to avoid hitting the limit.
     */
    private static void deleteAllExcept(Connection c, Set<String> keepIds) throws SQLException {
        if (keepIds.isEmpty()) {
            try (Statement st = c.createStatement()) {
                st.executeUpdate("DELETE FROM tasks");
            }
            return;
        }
        if (keepIds.size() <= 900) {
            StringBuilder sb = new StringBuilder("DELETE FROM tasks WHERE id NOT IN (");
            for (int i = 0; i < keepIds.size(); i++) {
                if (i > 0) sb.append(',');
                sb.append('?');
            }
            sb.append(')');
            try (PreparedStatement ps = c.prepareStatement(sb.toString())) {
                int i = 1;
                for (String id : keepIds) ps.setString(i++, id);
                ps.executeUpdate();
            }
        } else {
            // Larger lists: stage them in a temp table.
            try (Statement st = c.createStatement()) {
                st.executeUpdate("CREATE TEMP TABLE IF NOT EXISTS _keep_ids (id TEXT PRIMARY KEY)");
                st.executeUpdate("DELETE FROM _keep_ids");
            }
            try (PreparedStatement ps = c.prepareStatement("INSERT INTO _keep_ids(id) VALUES (?)")) {
                for (String id : keepIds) {
                    ps.setString(1, id);
                    ps.addBatch();
                }
                ps.executeBatch();
            }
            try (Statement st = c.createStatement()) {
                st.executeUpdate("DELETE FROM tasks WHERE id NOT IN (SELECT id FROM _keep_ids)");
            }
        }
    }
}
