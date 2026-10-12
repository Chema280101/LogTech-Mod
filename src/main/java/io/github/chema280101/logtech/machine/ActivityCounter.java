package io.github.chema280101.logtech.machine;

/**
 * Contador de actividad en Java puro para telemetría de máquinas.
 * Utiliza un búfer circular de 60 cubos (1 por minuto = 1 200 ticks de juego).
 * Cada cubo guarda items entrados, items salidos, ticks activos y ticks inactivos.
 * No genera objetos en cada tick para garantizar máximo rendimiento.
 */
public class ActivityCounter {
    public static final int TICKS_PER_MINUTE = 1_200;
    public static final int BUFFER_SIZE = 60;

    private final MinuteBucket[] buckets;
    private int currentIndex;
    private int currentTickInMinute;
    private int minutesRecorded;
    private long lastGameTick = -1;

    public ActivityCounter() {
        this.buckets = new MinuteBucket[BUFFER_SIZE];
        for (int i = 0; i < BUFFER_SIZE; i++) {
            this.buckets[i] = new MinuteBucket();
        }
        this.currentIndex = 0;
        this.currentTickInMinute = 0;
        this.minutesRecorded = 0;
    }

    /**
     * Registra items que entraron a la máquina en el minuto actual.
     */
    public void recordItemIn(int count) {
        if (count > 0) {
            this.buckets[this.currentIndex].addItemsIn(count);
        }
    }

    public void recordItemIn() {
        recordItemIn(1);
    }

    /**
     * Registra items que salieron de la máquina en el minuto actual.
     */
    public void recordItemOut(int count) {
        if (count > 0) {
            this.buckets[this.currentIndex].addItemsOut(count);
        }
    }

    public void recordItemOut() {
        recordItemOut(1);
    }

    /**
     * Avanza un tick registrando si la máquina estuvo activa o inactiva.
     * Si se alcanzan 1 200 ticks, avanza automáticamente al siguiente cubo del búfer circular.
     */
    public void tick(boolean active) {
        advanceTicks(1, active);
    }

    /**
     * Registra una cantidad específica de ticks activos.
     */
    public void recordActiveTicks(int ticks) {
        if (ticks > 0) {
            advanceTicks(ticks, true);
        }
    }

    /**
     * Registra una cantidad específica de ticks inactivos.
     */
    public void recordInactiveTicks(int ticks) {
        if (ticks > 0) {
            advanceTicks(ticks, false);
        }
    }

    /**
     * Permite registrar el tiempo transcurrido en base al tiempo de juego (game tick).
     * Calcula la duración transcurrida desde la última llamada y la asigna al estado indicado.
     */
    public void update(long currentGameTick, boolean active) {
        if (this.lastGameTick < 0) {
            this.lastGameTick = currentGameTick;
            return;
        }
        long elapsed = currentGameTick - this.lastGameTick;
        if (elapsed > 0) {
            advanceTicks((int) elapsed, active);
            this.lastGameTick = currentGameTick;
        }
    }

    /**
     * Avanza un número determinado de ticks distribuyéndolos en los cubos de minutos correspondientes.
     */
    public void advanceTicks(int ticks, boolean active) {
        while (ticks > 0) {
            int remainingInMinute = TICKS_PER_MINUTE - this.currentTickInMinute;
            int step = Math.min(ticks, remainingInMinute);

            if (active) {
                this.buckets[this.currentIndex].addActiveTicks(step);
            } else {
                this.buckets[this.currentIndex].addInactiveTicks(step);
            }

            this.currentTickInMinute += step;
            ticks -= step;

            if (this.currentTickInMinute >= TICKS_PER_MINUTE) {
                advanceMinute();
            }
        }
    }

    /**
     * Avanza al siguiente minuto en el búfer circular, limpiando el cubo objetivo.
     */
    public void advanceMinute() {
        this.currentIndex = (this.currentIndex + 1) % BUFFER_SIZE;
        this.buckets[this.currentIndex].reset();
        this.currentTickInMinute = 0;
        if (this.minutesRecorded < BUFFER_SIZE) {
            this.minutesRecorded++;
        }
    }

    /**
     * Determina el índice de referencia más reciente con datos activos.
     * Si el minuto actual apenas inició y no tiene actividad registrada, toma el último minuto completado.
     */
    public int getLatestActiveIndex() {
        if (this.currentTickInMinute > 0 || this.buckets[this.currentIndex].hasActivity()) {
            return this.currentIndex;
        }
        if (this.minutesRecorded > 0) {
            return (this.currentIndex - 1 + BUFFER_SIZE) % BUFFER_SIZE;
        }
        return this.currentIndex;
    }

    /**
     * Devuelve el cubo correspondiente al minuto actual en curso.
     */
    public MinuteBucket getCurrentBucket() {
        return this.buckets[this.currentIndex];
    }

    /**
     * Obtiene un cubo histórico a partir del último minuto con actividad. 0 = minuto más reciente, 1 = anterior, etc.
     */
    public MinuteBucket getBucket(int minutesAgo) {
        if (minutesAgo < 0 || minutesAgo >= BUFFER_SIZE) {
            throw new IndexOutOfBoundsException("Minutes ago must be between 0 and " + (BUFFER_SIZE - 1));
        }
        int baseIndex = getLatestActiveIndex();
        int index = (baseIndex - minutesAgo + BUFFER_SIZE) % BUFFER_SIZE;
        return this.buckets[index];
    }

    /**
     * Consulta y suma la actividad de los últimos N minutos.
     */
    public ActivitySummary getSummary(int minutes) {
        if (minutes <= 0) {
            return ActivitySummary.EMPTY;
        }
        int count = Math.min(minutes, BUFFER_SIZE);
        int baseIndex = getLatestActiveIndex();
        int totalIn = 0;
        int totalOut = 0;
        int totalActive = 0;
        int totalInactive = 0;

        for (int i = 0; i < count; i++) {
            int index = (baseIndex - i + BUFFER_SIZE) % BUFFER_SIZE;
            MinuteBucket bucket = this.buckets[index];
            totalIn += bucket.getItemsIn();
            totalOut += bucket.getItemsOut();
            totalActive += bucket.getActiveTicks();
            totalInactive += bucket.getInactiveTicks();
        }

        return new ActivitySummary(totalIn, totalOut, totalActive, totalInactive);
    }

    /**
     * Consulta y suma la actividad de los últimos N minutos completados anteriores al más reciente.
     */
    public ActivitySummary getPastMinutesSummary(int minutes) {
        if (minutes <= 0) {
            return ActivitySummary.EMPTY;
        }
        int count = Math.min(minutes, BUFFER_SIZE - 1);
        int baseIndex = getLatestActiveIndex();
        int totalIn = 0;
        int totalOut = 0;
        int totalActive = 0;
        int totalInactive = 0;

        for (int i = 1; i <= count; i++) {
            int index = (baseIndex - i + BUFFER_SIZE) % BUFFER_SIZE;
            MinuteBucket bucket = this.buckets[index];
            totalIn += bucket.getItemsIn();
            totalOut += bucket.getItemsOut();
            totalActive += bucket.getActiveTicks();
            totalInactive += bucket.getInactiveTicks();
        }

        return new ActivitySummary(totalIn, totalOut, totalActive, totalInactive);
    }

    public int getItemsIn(int minutes) {
        return getSummary(minutes).itemsIn();
    }

    public int getItemsOut(int minutes) {
        return getSummary(minutes).itemsOut();
    }

    public int getActiveTicks(int minutes) {
        return getSummary(minutes).activeTicks();
    }

    public int getInactiveTicks(int minutes) {
        return getSummary(minutes).inactiveTicks();
    }

    public int getCurrentTickInMinute() {
        return this.currentTickInMinute;
    }

    public int getMinutesRecorded() {
        return this.minutesRecorded;
    }

    /**
     * Estructura mutable reutilizada en el búfer circular para evitar asignación de memoria.
     */
    public static final class MinuteBucket {
        private int itemsIn;
        private int itemsOut;
        private int activeTicks;
        private int inactiveTicks;

        public MinuteBucket() {
            reset();
        }

        public void reset() {
            this.itemsIn = 0;
            this.itemsOut = 0;
            this.activeTicks = 0;
            this.inactiveTicks = 0;
        }

        public boolean hasActivity() {
            return this.itemsIn > 0 || this.itemsOut > 0 || this.activeTicks > 0 || this.inactiveTicks > 0;
        }

        public void addItemsIn(int count) {
            this.itemsIn += count;
        }

        public void addItemsOut(int count) {
            this.itemsOut += count;
        }

        public void addActiveTicks(int ticks) {
            this.activeTicks += ticks;
        }

        public void addInactiveTicks(int ticks) {
            this.inactiveTicks += ticks;
        }

        public int getItemsIn() {
            return this.itemsIn;
        }

        public int getItemsOut() {
            return this.itemsOut;
        }

        public int getActiveTicks() {
            return this.activeTicks;
        }

        public int getInactiveTicks() {
            return this.inactiveTicks;
        }
    }

    /**
     * Resumen inmutable del total de actividad calculado.
     */
    public record ActivitySummary(int itemsIn, int itemsOut, int activeTicks, int inactiveTicks) {
        public static final ActivitySummary EMPTY = new ActivitySummary(0, 0, 0, 0);

        public int totalTicks() {
            return activeTicks + inactiveTicks;
        }

        public double getActivePercentage() {
            int total = totalTicks();
            return total == 0 ? 0.0 : (double) activeTicks / total;
        }
    }
}
