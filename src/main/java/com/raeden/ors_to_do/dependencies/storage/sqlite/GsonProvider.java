package com.raeden.ors_to_do.dependencies.storage.sqlite;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializer;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * Shared {@link Gson} instances used by every storage component.
 *
 * <p>Two flavours live here:</p>
 * <ul>
 *   <li>{@link #compact()} — single-line JSON used for the SQLite payload columns. The storage
 *   redesign explicitly drops pretty-printing on internal serialization (the doc's "60k lines"
 *   bloat is mostly whitespace), keeping each row tight.</li>
 *   <li>{@link #pretty()} — multi-line JSON kept only for {@code BackupManager}'s explicit
 *   user-facing export, where humans actually read the file.</li>
 * </ul>
 *
 * <p>Both share the same {@link LocalDate} / {@link LocalDateTime} / {@link LocalTime} adapters
 * the legacy {@code StorageManager} used, so JSON written before the SQLite cutover round-trips
 * cleanly during migration.</p>
 */
public final class GsonProvider {

    private static final Gson COMPACT = build(false);
    private static final Gson PRETTY = build(true);

    private GsonProvider() { }

    /** Single-line JSON. Use this for everything stored inside SQLite payload columns. */
    public static Gson compact() { return COMPACT; }

    /** Pretty-printed JSON. Reserved for explicit user-triggered exports. */
    public static Gson pretty() { return PRETTY; }

    private static Gson build(boolean pretty) {
        GsonBuilder b = new GsonBuilder()
                .registerTypeAdapter(LocalDateTime.class, (JsonSerializer<LocalDateTime>) (src, t, c) -> new JsonPrimitive(src.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)))
                .registerTypeAdapter(LocalDateTime.class, (JsonDeserializer<LocalDateTime>) (json, t, c) -> LocalDateTime.parse(json.getAsString(), DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                .registerTypeAdapter(LocalDate.class, (JsonSerializer<LocalDate>) (src, t, c) -> new JsonPrimitive(src.format(DateTimeFormatter.ISO_LOCAL_DATE)))
                .registerTypeAdapter(LocalDate.class, (JsonDeserializer<LocalDate>) (json, t, c) -> LocalDate.parse(json.getAsString(), DateTimeFormatter.ISO_LOCAL_DATE))
                .registerTypeAdapter(LocalTime.class, (JsonSerializer<LocalTime>) (src, t, c) -> new JsonPrimitive(src.format(DateTimeFormatter.ISO_LOCAL_TIME)))
                .registerTypeAdapter(LocalTime.class, (JsonDeserializer<LocalTime>) (json, t, c) -> LocalTime.parse(json.getAsString(), DateTimeFormatter.ISO_LOCAL_TIME));
        if (pretty) b.setPrettyPrinting();
        return b.create();
    }
}
