package com.raeden.ors_to_do.dependencies.storage.sqlite;

import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializer;
import com.raeden.ors_to_do.dependencies.models.AppStats;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Reads / writes the {@link AppStats} object across {@code app_meta} (the bounded config blob)
 * plus the three log tables ({@code history_log}, {@code advanced_history_log},
 * {@code deleted_task_history}).
 *
 * <p>The split is what the storage redesign calls for: the AppStats payload that gets loaded on
 * every launch stays small and bounded, while the unbounded logs live in their own date-keyed
 * tables that can be trimmed and queried independently.</p>
 *
 * <p>{@link #DELETED_HISTORY_CAP} is enforced on every save so {@code deletedTaskHistory} can't
 * grow without bound the way it did under the old JSON scheme.</p>
 */
public final class AppMetaRepository {

    /** Hard cap on rows kept in {@code deleted_task_history}. Anything beyond is trimmed on save. */
    public static final int DELETED_HISTORY_CAP = 500;

    /**
     * Set of {@link AppStats} field names that live in their own tables. The Gson used for the
     * {@code app_meta} payload skips these so the blob stays bounded.
     */
    private static final Set<String> SPLIT_OUT_FIELDS = Set.of(
            "historyLog",
            "advancedHistoryLog",
            "deletedTaskHistory"
    );

    /** Gson tailored to the meta blob: same type adapters as {@link GsonProvider}, but skips the log fields. */
    private static final Gson META_GSON = buildMetaGson();

    private AppMetaRepository() { }

    /** Loads the full AppStats (config + every log entry currently in the database). */
    public static AppStats load(Db db) throws SQLException {
        synchronized (db.lock()) {
            Connection c = db.connection();
            AppStats stats = loadConfigPayload(c);
            if (stats == null) stats = new AppStats();

            // Populate the three logs from their dedicated tables.
            loadHistoryLog(c, stats);
            loadAdvancedHistoryLog(c, stats);
            loadDeletedTaskHistory(c, stats);
            return stats;
        }
    }

    /**
     * Writes the AppStats config blob and replaces every log table to match the supplied object.
     * Wrapped in a single transaction so a crash mid-save can't leave the DB partially updated.
     */
    public static void save(Db db, AppStats stats) throws SQLException {
        if (stats == null) return;
        synchronized (db.lock()) {
            Connection c = db.connection();
            boolean prevAutoCommit = c.getAutoCommit();
            c.setAutoCommit(false);
            try {
                writeConfigPayload(c, stats);
                replaceHistoryLog(c, stats);
                replaceAdvancedHistoryLog(c, stats);
                replaceDeletedTaskHistory(c, stats);
                c.commit();
            } catch (SQLException e) {
                c.rollback();
                throw e;
            } finally {
                c.setAutoCommit(prevAutoCommit);
            }
        }
    }

    /** True if {@code app_meta} hasn't been populated yet — used by the JSON importer. */
    public static boolean isEmpty(Db db) throws SQLException {
        synchronized (db.lock()) {
            try (Statement st = db.connection().createStatement();
                 ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM app_meta")) {
                return rs.next() && rs.getInt(1) == 0;
            }
        }
    }

    // ---------------------------------------------------------------------
    // app_meta — single-row config payload
    // ---------------------------------------------------------------------

    private static AppStats loadConfigPayload(Connection c) throws SQLException {
        try (Statement st = c.createStatement();
             ResultSet rs = st.executeQuery("SELECT payload FROM app_meta WHERE id = 1")) {
            if (!rs.next()) return null;
            String json = rs.getString(1);
            return META_GSON.fromJson(json, AppStats.class);
        }
    }

    private static void writeConfigPayload(Connection c, AppStats stats) throws SQLException {
        String json = META_GSON.toJson(stats);
        try (PreparedStatement ps = c.prepareStatement(
                "INSERT INTO app_meta(id, payload) VALUES (1, ?) " +
                        "ON CONFLICT(id) DO UPDATE SET payload = excluded.payload")) {
            ps.setString(1, json);
            ps.executeUpdate();
        }
    }

    // ---------------------------------------------------------------------
    // history_log — date → percentage
    // ---------------------------------------------------------------------

    private static void loadHistoryLog(Connection c, AppStats stats) throws SQLException {
        Map<LocalDate, Double> ordered = new LinkedHashMap<>();
        try (Statement st = c.createStatement();
             ResultSet rs = st.executeQuery("SELECT day, completion FROM history_log ORDER BY day")) {
            while (rs.next()) {
                ordered.put(LocalDate.parse(rs.getString(1)), rs.getDouble(2));
            }
        }
        stats.getHistoryLog().clear();
        stats.getHistoryLog().putAll(ordered);
    }

    private static void replaceHistoryLog(Connection c, AppStats stats) throws SQLException {
        try (Statement st = c.createStatement()) {
            st.executeUpdate("DELETE FROM history_log");
        }
        if (stats.getHistoryLog() == null || stats.getHistoryLog().isEmpty()) return;
        try (PreparedStatement ps = c.prepareStatement(
                "INSERT OR REPLACE INTO history_log(day, completion) VALUES (?, ?)")) {
            for (Map.Entry<LocalDate, Double> e : stats.getHistoryLog().entrySet()) {
                if (e.getKey() == null) continue;
                ps.setString(1, e.getKey().toString());
                ps.setDouble(2, e.getValue() == null ? 0.0 : e.getValue());
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    // ---------------------------------------------------------------------
    // advanced_history_log — date → int[]
    // ---------------------------------------------------------------------

    private static void loadAdvancedHistoryLog(Connection c, AppStats stats) throws SQLException {
        Map<LocalDate, int[]> ordered = new LinkedHashMap<>();
        try (Statement st = c.createStatement();
             ResultSet rs = st.executeQuery("SELECT day, payload FROM advanced_history_log ORDER BY day")) {
            while (rs.next()) {
                int[] arr = GsonProvider.compact().fromJson(rs.getString(2), int[].class);
                ordered.put(LocalDate.parse(rs.getString(1)), arr);
            }
        }
        stats.getAdvancedHistoryLog().clear();
        stats.getAdvancedHistoryLog().putAll(ordered);
    }

    private static void replaceAdvancedHistoryLog(Connection c, AppStats stats) throws SQLException {
        try (Statement st = c.createStatement()) {
            st.executeUpdate("DELETE FROM advanced_history_log");
        }
        if (stats.getAdvancedHistoryLog() == null || stats.getAdvancedHistoryLog().isEmpty()) return;
        try (PreparedStatement ps = c.prepareStatement(
                "INSERT OR REPLACE INTO advanced_history_log(day, payload) VALUES (?, ?)")) {
            for (Map.Entry<LocalDate, int[]> e : stats.getAdvancedHistoryLog().entrySet()) {
                if (e.getKey() == null) continue;
                ps.setString(1, e.getKey().toString());
                ps.setString(2, GsonProvider.compact().toJson(e.getValue()));
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    // ---------------------------------------------------------------------
    // deleted_task_history — capped append-only
    // ---------------------------------------------------------------------

    private static void loadDeletedTaskHistory(Connection c, AppStats stats) throws SQLException {
        List<String> rows = new ArrayList<>();
        try (Statement st = c.createStatement();
             ResultSet rs = st.executeQuery("SELECT entry FROM deleted_task_history ORDER BY rowid")) {
            while (rs.next()) rows.add(rs.getString(1));
        }
        stats.getDeletedTaskHistory().clear();
        stats.getDeletedTaskHistory().addAll(rows);
    }

    private static void replaceDeletedTaskHistory(Connection c, AppStats stats) throws SQLException {
        try (Statement st = c.createStatement()) {
            st.executeUpdate("DELETE FROM deleted_task_history");
        }
        List<String> entries = stats.getDeletedTaskHistory();
        if (entries == null || entries.isEmpty()) return;

        // Keep only the most recent DELETED_HISTORY_CAP entries.
        int start = Math.max(0, entries.size() - DELETED_HISTORY_CAP);
        String nowIso = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        try (PreparedStatement ps = c.prepareStatement(
                "INSERT INTO deleted_task_history(entry, ts) VALUES (?, ?)")) {
            for (int i = start; i < entries.size(); i++) {
                ps.setString(1, entries.get(i));
                ps.setString(2, nowIso);
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    // ---------------------------------------------------------------------
    // Gson for the meta blob — skips log fields
    // ---------------------------------------------------------------------

    private static Gson buildMetaGson() {
        return new GsonBuilder()
                .registerTypeAdapter(LocalDateTime.class, (JsonSerializer<LocalDateTime>) (s, t, c) -> new JsonPrimitive(s.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)))
                .registerTypeAdapter(LocalDateTime.class, (JsonDeserializer<LocalDateTime>) (j, t, c) -> LocalDateTime.parse(j.getAsString(), DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                .registerTypeAdapter(LocalDate.class, (JsonSerializer<LocalDate>) (s, t, c) -> new JsonPrimitive(s.format(DateTimeFormatter.ISO_LOCAL_DATE)))
                .registerTypeAdapter(LocalDate.class, (JsonDeserializer<LocalDate>) (j, t, c) -> LocalDate.parse(j.getAsString(), DateTimeFormatter.ISO_LOCAL_DATE))
                .registerTypeAdapter(LocalTime.class, (JsonSerializer<LocalTime>) (s, t, c) -> new JsonPrimitive(s.format(DateTimeFormatter.ISO_LOCAL_TIME)))
                .registerTypeAdapter(LocalTime.class, (JsonDeserializer<LocalTime>) (j, t, c) -> LocalTime.parse(j.getAsString(), DateTimeFormatter.ISO_LOCAL_TIME))
                .addSerializationExclusionStrategy(new ExclusionStrategy() {
                    @Override public boolean shouldSkipField(FieldAttributes f) {
                        return f.getDeclaringClass() == AppStats.class && SPLIT_OUT_FIELDS.contains(f.getName());
                    }
                    @Override public boolean shouldSkipClass(Class<?> aClass) { return false; }
                })
                .create();
    }
}
