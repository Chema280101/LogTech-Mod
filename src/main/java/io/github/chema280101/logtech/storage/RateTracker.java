package io.github.chema280101.logtech.storage;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Rastreador de ritmos, tendencias y estimaciones de llenado y agotamiento en Java puro.
 * Mantiene un historial de 5 cubos de un minuto (desacoplado de Minecraft y testeable en JUnit).
 *
 * <p>Manejo de memoria y podado:
 * <ul>
 *   <li>Los datos viven exclusivamente en memoria (RAM) y nunca se persisten en disco ni NBT.</li>
 *   <li>Solo se registran ítems que sufrieron inserciones o extracciones durante el minuto.</li>
 *   <li>Los ítems inactivos se podan automáticamente al salir de la ventana deslizante de 5 minutos.</li>
 *   <li>Las cancelaciones internas dentro del mismo minuto (neto = 0) se eliminan inmediatamente.</li>
 *   <li>Se establece un límite máximo de ítems rastreados (por defecto 10 000) para acotar la memoria.</li>
 * </ul>
 *
 * @param <K> tipo de clave del ítem (por ejemplo ItemKey o String)
 */
public class RateTracker<K> {
    public static final int MINUTES_WINDOW = 5;
    public static final int TICKS_PER_MINUTE = 1_200;
    public static final int DEFAULT_MAX_TRACKED_ITEMS = 10_000;

    /**
     * Nivel de almacenamiento en Java puro para independizar RateTracker de clases de Minecraft.
     */
    public enum Tier {
        HOT,
        COLD
    }

    /**
     * Cubo de telemetría que agrupa los cambios netos de un minuto.
     */
    public static class MinuteBucket<K> {
        private final Map<K, Long> itemDeltas = new HashMap<>();
        private long hotDelta = 0L;
        private long coldDelta = 0L;
        private long totalDelta = 0L;

        public void record(K item, long amount, long hotAmount, long coldAmount, int maxTrackedItems) {
            if (item != null && amount != 0L) {
                if (this.itemDeltas.size() < maxTrackedItems || this.itemDeltas.containsKey(item)) {
                    long current = this.itemDeltas.getOrDefault(item, 0L);
                    long updated = current + amount;
                    if (updated == 0L) {
                        this.itemDeltas.remove(item);
                    } else {
                        this.itemDeltas.put(item, updated);
                    }
                }
            }
            this.hotDelta += hotAmount;
            this.coldDelta += coldAmount;
            this.totalDelta += (hotAmount + coldAmount);
        }

        public Map<K, Long> getItemDeltas() {
            return this.itemDeltas;
        }

        public long getHotDelta() {
            return this.hotDelta;
        }

        public long getColdDelta() {
            return this.coldDelta;
        }

        public long getTotalDelta() {
            return this.totalDelta;
        }

        public void reset() {
            this.itemDeltas.clear();
            this.hotDelta = 0L;
            this.coldDelta = 0L;
            this.totalDelta = 0L;
        }
    }

    @SuppressWarnings("unchecked")
    private final MinuteBucket<K>[] completedBuckets = new MinuteBucket[MINUTES_WINDOW];
    private int bucketHead = 0;
    private int completedCount = 0;

    private final MinuteBucket<K> currentBucket = new MinuteBucket<>();
    private int currentMinuteTicks = 0;

    private final Map<K, Long> aggregatedRates = new HashMap<>();
    private long hotNetRate = 0L;
    private long coldNetRate = 0L;
    private long totalNetRate = 0L;

    private long version = 0L;
    private int maxTrackedItems = DEFAULT_MAX_TRACKED_ITEMS;

    public RateTracker() {
        for (int i = 0; i < MINUTES_WINDOW; i++) {
            this.completedBuckets[i] = new MinuteBucket<>();
        }
    }

    public int getMaxTrackedItems() {
        return this.maxTrackedItems;
    }

    public void setMaxTrackedItems(int maxTrackedItems) {
        this.maxTrackedItems = Math.max(10, maxTrackedItems);
    }

    /**
     * Registra un evento de inserción en el cubo en progreso.
     */
    public void recordInsert(K item, long amount, Tier tier) {
        if (amount <= 0L) return;
        long hot = (tier == Tier.HOT) ? amount : 0L;
        long cold = (tier == Tier.COLD) ? amount : 0L;
        this.currentBucket.record(item, amount, hot, cold, this.maxTrackedItems);
    }

    public void recordInsert(K item, long amount, long hotAmount, long coldAmount) {
        if (amount <= 0L) return;
        this.currentBucket.record(item, amount, hotAmount, coldAmount, this.maxTrackedItems);
    }

    /**
     * Registra un evento de extracción en el cubo en progreso.
     */
    public void recordExtract(K item, long amount, Tier tier) {
        if (amount <= 0L) return;
        long hot = (tier == Tier.HOT) ? amount : 0L;
        long cold = (tier == Tier.COLD) ? amount : 0L;
        this.currentBucket.record(item, -amount, -hot, -cold, this.maxTrackedItems);
    }

    public void recordExtract(K item, long amount, long hotAmount, long coldAmount) {
        if (amount <= 0L) return;
        this.currentBucket.record(item, -amount, -hotAmount, -coldAmount, this.maxTrackedItems);
    }

    /**
     * Avanza el contador de ticks del minuto. No realiza trabajo por ítem.
     */
    public void tick() {
        this.currentMinuteTicks++;
        if (this.currentMinuteTicks >= TICKS_PER_MINUTE) {
            advanceMinute();
        }
    }

    /**
     * Cierra el minuto actual, traslada los deltas acumulados a la ventana circular de 5 minutos,
     * recalcula los ritmos agregados y poda los ítems sin actividad.
     */
    public void advanceMinute() {
        this.currentMinuteTicks = 0;

        MinuteBucket<K> target = this.completedBuckets[this.bucketHead];
        target.reset();
        for (Map.Entry<K, Long> entry : this.currentBucket.getItemDeltas().entrySet()) {
            target.record(entry.getKey(), entry.getValue(), 0L, 0L, this.maxTrackedItems);
        }
        target.hotDelta = this.currentBucket.getHotDelta();
        target.coldDelta = this.currentBucket.getColdDelta();
        target.totalDelta = this.currentBucket.getTotalDelta();

        this.bucketHead = (this.bucketHead + 1) % MINUTES_WINDOW;
        if (this.completedCount < MINUTES_WINDOW) {
            this.completedCount++;
        }

        this.currentBucket.reset();

        aggregateAndPrune();
        this.version++;
    }

    private void aggregateAndPrune() {
        this.aggregatedRates.clear();
        this.hotNetRate = 0L;
        this.coldNetRate = 0L;
        this.totalNetRate = 0L;

        if (this.completedCount == 0) {
            return;
        }

        long sumHot = 0L;
        long sumCold = 0L;
        long sumTotal = 0L;
        Map<K, Long> sumPerItem = new HashMap<>();

        for (int i = 0; i < this.completedCount; i++) {
            MinuteBucket<K> b = this.completedBuckets[i];
            sumHot += b.getHotDelta();
            sumCold += b.getColdDelta();
            sumTotal += b.getTotalDelta();
            for (Map.Entry<K, Long> e : b.getItemDeltas().entrySet()) {
                sumPerItem.merge(e.getKey(), e.getValue(), Long::sum);
            }
        }

        int window = this.completedCount;
        this.hotNetRate = Math.round((double) sumHot / window);
        this.coldNetRate = Math.round((double) sumCold / window);
        this.totalNetRate = Math.round((double) sumTotal / window);

        // Poda: solo se conservan en memoria ítems cuyo ritmo neto redondeado no sea 0
        for (Map.Entry<K, Long> e : sumPerItem.entrySet()) {
            long delta = e.getValue();
            long rate = Math.round((double) delta / window);
            if (rate != 0L) {
                this.aggregatedRates.put(e.getKey(), rate);
            }
        }
    }

    /**
     * Obtiene el ritmo neto por minuto de un ítem (+X/min o -X/min).
     * Devuelve 0L si el ítem está inactivo o fue podado.
     */
    public long getItemRate(K item) {
        if (item == null) {
            return 0L;
        }
        return this.aggregatedRates.getOrDefault(item, 0L);
    }

    public long getHotNetRate() {
        return this.hotNetRate;
    }

    public long getColdNetRate() {
        return this.coldNetRate;
    }

    public long getTotalNetRate() {
        return this.totalNetRate;
    }

    public Map<K, Long> getAggregatedRates() {
        return Collections.unmodifiableMap(this.aggregatedRates);
    }

    public int getTrackedItemCount() {
        return this.aggregatedRates.size();
    }

    public long getVersion() {
        return this.version;
    }

    public int getCompletedMinutes() {
        return this.completedCount;
    }

    // ── Métodos estáticos de estimación y formateo ──────────────────────────────────────

    /**
     * Calcula los minutos necesarios para llenar el espacio restante.
     * @param remainingSpace capacidad libre (capacidad - almacenado)
     * @param netRatePerMinute ritmo neto por minuto (debe ser > 0)
     * @return minutos redondeados hacia arriba, o -1 si es estable o ya está lleno
     */
    public static long calculateFillTimeMinutes(long remainingSpace, long netRatePerMinute) {
        if (remainingSpace <= 0L || netRatePerMinute <= 0L) {
            return -1L;
        }
        return (long) Math.ceil((double) remainingSpace / netRatePerMinute);
    }

    /**
     * Calcula los minutos necesarios para que un ítem se agote.
     * @param currentAmount cantidad actual en la red
     * @param netRatePerMinute ritmo neto por minuto (debe ser < 0)
     * @return minutos redondeados hacia arriba, o -1 si el ítem no está disminuyendo
     */
    public static long calculateDepletionTimeMinutes(long currentAmount, long netRatePerMinute) {
        if (currentAmount <= 0L || netRatePerMinute >= 0L) {
            return -1L;
        }
        long absRate = Math.abs(netRatePerMinute);
        return (long) Math.ceil((double) currentAmount / absRate);
    }

    /**
     * Formatea una duración en minutos en una cadena legible como «2 h 14 min» o «6 min».
     */
    public static String formatDuration(long minutes) {
        if (minutes < 1L) {
            return "< 1 min";
        }
        if (minutes < 60L) {
            return minutes + " min";
        }
        long hours = minutes / 60L;
        long remMins = minutes % 60L;
        if (hours < 24L) {
            if (remMins == 0L) {
                return hours + " h";
            }
            return hours + " h " + remMins + " min";
        }
        long days = hours / 24L;
        long remHours = hours % 24L;
        if (remHours == 0L) {
            return days + " d";
        }
        return days + " d " + remHours + " h";
    }

    /**
     * Genera la estimación de llenado en formato de texto directo.
     */
    public static String formatFillEstimate(long remainingSpace, long netRatePerMinute) {
        if (remainingSpace <= 0L || netRatePerMinute <= 0L) {
            return "estable";
        }
        long mins = calculateFillTimeMinutes(remainingSpace, netRatePerMinute);
        return "se llena en " + formatDuration(mins);
    }
}
