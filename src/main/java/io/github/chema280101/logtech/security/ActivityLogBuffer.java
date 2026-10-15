package io.github.chema280101.logtech.security;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.jetbrains.annotations.Nullable;

/**
 * Búfer circular de registro de actividad en la red de LogTech.
 * Desacoplado como clase Java pura para pruebas unitarias sin dependencias de Minecraft.
 *
 * Características:
 * - Agrupación de acciones repetidas del mismo jugador, ítem y tipo en una ventana temporal (< 10 segundos = 200 ticks).
 * - Búfer circular acotado: al superar la capacidad máxima, se descartan las entradas más antiguas (FIFO).
 * - Filtrado por jugador (nombre o UUID) y paginación acotada para cliente y red.
 *
 * @param <T> Tipo del identificador del ítem.
 */
public class ActivityLogBuffer<T> {

    public static final int DEFAULT_CAPACITY = 500;
    public static final long DEFAULT_GROUPING_WINDOW_TICKS = 200L; // 10 segundos @ 20 ticks/seg

    private final List<ActivityLogEntry<T>> entries = new ArrayList<>();
    private long groupingWindowTicks;

    public ActivityLogBuffer() {
        this(DEFAULT_GROUPING_WINDOW_TICKS);
    }

    public ActivityLogBuffer(long groupingWindowTicks) {
        this.groupingWindowTicks = Math.max(0L, groupingWindowTicks);
    }

    public synchronized long getGroupingWindowTicks() {
        return this.groupingWindowTicks;
    }

    public synchronized void setGroupingWindowTicks(long ticks) {
        this.groupingWindowTicks = Math.max(0L, ticks);
    }

    /**
     * Registra una acción de jugador. Si existe una acción repetida del mismo jugador, ítem y tipo
     * ocurrida hace menos de {@code groupingWindowTicks}, se agrupan sumando cantidades y actualizando
     * la marca de tiempo a la más reciente.
     */
    public synchronized void record(
            long gameTime,
            UUID playerUuid,
            String playerName,
            ActivityAction action,
            @Nullable T item,
            long amount,
            int maxCapacity
    ) {
        if (playerUuid == null || action == null || amount <= 0 || maxCapacity <= 0) {
            return;
        }
        String safeName = (playerName != null && !playerName.isBlank()) ? playerName : playerUuid.toString().substring(0, 8);

        int matchIndex = -1;
        if (groupingWindowTicks > 0) {
            for (int i = entries.size() - 1; i >= 0; i--) {
                ActivityLogEntry<T> existing = entries.get(i);
                if (Math.abs(gameTime - existing.gameTime()) > groupingWindowTicks) {
                    // La lista está ordenada cronológicamente; fuera de la ventana no se agrupa
                    break;
                }
                if (existing.playerUuid().equals(playerUuid)
                        && existing.action() == action
                        && Objects.equals(existing.item(), item)) {
                    matchIndex = i;
                    break;
                }
            }
        }

        if (matchIndex >= 0) {
            ActivityLogEntry<T> existing = entries.remove(matchIndex);
            long newAmount = existing.amount() + amount;
            ActivityLogEntry<T> updated = new ActivityLogEntry<>(
                    gameTime,
                    playerUuid,
                    safeName,
                    action,
                    item,
                    newAmount
            );
            entries.add(updated);
        } else {
            entries.add(new ActivityLogEntry<>(gameTime, playerUuid, safeName, action, item, amount));
        }

        enforceCapacity(maxCapacity);
    }

    /**
     * Añade una entrada directamente (útil al cargar desde NBT persistido).
     */
    public synchronized void addRawEntry(ActivityLogEntry<T> entry, int maxCapacity) {
        if (entry == null || maxCapacity <= 0) {
            return;
        }
        entries.add(entry);
        enforceCapacity(maxCapacity);
    }

    /**
     * Limita el tamaño del búfer circular eliminando las entradas más antiguas (FIFO).
     */
    public synchronized void enforceCapacity(int maxCapacity) {
        if (maxCapacity <= 0) {
            entries.clear();
            return;
        }
        while (entries.size() > maxCapacity) {
            entries.remove(0);
        }
    }

    public synchronized List<ActivityLogEntry<T>> getEntries() {
        return Collections.unmodifiableList(new ArrayList<>(entries));
    }

    public synchronized int size() {
        return entries.size();
    }

    public synchronized boolean isEmpty() {
        return entries.isEmpty();
    }

    public synchronized void clear() {
        entries.clear();
    }

    /**
     * Resultado paginado de consulta del registro.
     */
    public record PageResult<T>(
            List<ActivityLogEntry<T>> entries,
            int pageIndex,
            int totalPages,
            int totalMatchingEntries
    ) {}

    /**
     * Obtiene una página paginada de entradas filtradas por jugador.
     *
     * @param pageIndex     Índice de página (0-indexed).
     * @param pageSize      Tamaño de página (debe ser > 0).
     * @param playerFilter  Filtro opcional por nombre o UUID del jugador (coincidencia parcial, insensible a mayúsculas).
     * @param newestFirst   Si es true, las acciones más recientes aparecen primero (orden estándar para auditoría).
     * @return El resultado con la página solicitada.
     */
    public synchronized PageResult<T> getPage(
            int pageIndex,
            int pageSize,
            @Nullable String playerFilter,
            boolean newestFirst
    ) {
        int safePageSize = Math.max(1, pageSize);

        List<ActivityLogEntry<T>> matching = new ArrayList<>();
        String filterNormalized = (playerFilter != null) ? playerFilter.trim().toLowerCase() : "";

        for (ActivityLogEntry<T> entry : entries) {
            if (filterNormalized.isEmpty()) {
                matching.add(entry);
            } else {
                boolean matchName = entry.playerName().toLowerCase().contains(filterNormalized);
                boolean matchUuid = entry.playerUuid().toString().toLowerCase().contains(filterNormalized);
                if (matchName || matchUuid) {
                    matching.add(entry);
                }
            }
        }

        if (newestFirst) {
            Collections.reverse(matching);
        }

        int totalMatching = matching.size();
        int totalPages = (totalMatching == 0) ? 1 : (int) Math.ceil((double) totalMatching / safePageSize);
        int clampedPage = Math.max(0, Math.min(pageIndex, totalPages - 1));

        int fromIndex = clampedPage * safePageSize;
        int toIndex = Math.min(fromIndex + safePageSize, totalMatching);

        List<ActivityLogEntry<T>> pageEntries = (fromIndex < totalMatching)
                ? new ArrayList<>(matching.subList(fromIndex, toIndex))
                : Collections.emptyList();

        return new PageResult<>(pageEntries, clampedPage, totalPages, totalMatching);
    }
}
