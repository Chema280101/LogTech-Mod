package io.github.chema280101.logtech.storage;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Gestor de vistas guardadas del Terminal.
 * Puro Java: sin ticks ni dependencias de Minecraft.
 *
 * <p>Invariantes:
 * <ul>
 *   <li>Máximo {@value #MAX_VIEWS} vistas.</li>
 *   <li>Los nombres siempre están saneados (longitud 1–20, sin chars de control).</li>
 *   <li>Los slots vacíos se representan como {@code null} en la lista interna.</li>
 * </ul>
 */
public class ViewManager {

    public static final int MAX_VIEWS = 8;

    /** Slots 0..7; null = slot vacío. */
    private final List<SavedView> views = new ArrayList<>(Collections.nCopies(MAX_VIEWS, null));
    private boolean dirty = false;

    // ── Acceso ────────────────────────────────────────────────────────────────

    /** Devuelve la vista del slot indicado (0-indexed) o null si está vacío. */
    public SavedView get(int index) {
        if (index < 0 || index >= MAX_VIEWS) {
            return null;
        }
        return views.get(index);
    }

    /** Vista de solo lectura de todos los slots (puede contener nulls). */
    public List<SavedView> getAll() {
        return Collections.unmodifiableList(views);
    }

    /** Número de slots ocupados. */
    public int count() {
        int n = 0;
        for (SavedView v : views) {
            if (v != null) {
                n++;
            }
        }
        return n;
    }

    public boolean isDirty() {
        return dirty;
    }

    public void clearDirty() {
        dirty = false;
    }

    // ── Mutación ──────────────────────────────────────────────────────────────

    /**
     * Guarda o actualiza la vista en el slot {@code index} (0-indexed).
     *
     * @param index slot de destino [0, MAX_VIEWS)
     * @param view  vista a guardar (nombre ya saneado exteriormente)
     * @return true si se guardó correctamente
     */
    public boolean save(int index, SavedView view) {
        return save(index, view, MAX_VIEWS);
    }

    /**
     * Guarda o actualiza la vista en el slot {@code index} validando contra un límite configurado.
     *
     * @param index      slot de destino
     * @param view       vista a guardar
     * @param maxAllowed límite máximo de slots activos permitidos
     * @return true si se guardó correctamente
     */
    public boolean save(int index, SavedView view, int maxAllowed) {
        if (index < 0 || index >= maxAllowed || index >= MAX_VIEWS || view == null) {
            return false;
        }
        String sanitized = SavedView.sanitizeName(view.name());
        if (sanitized == null) {
            return false;
        }
        SavedView sanitizedView = new SavedView(
                sanitized,
                SavedView.sanitizeSearch(view.searchText()),
                clamp(view.sortMode(), SavedView.SORT_AMOUNT, SavedView.SORT_NAME),
                clamp(view.tierFilter(), SavedView.TIER_ALL, SavedView.TIER_COLD),
                clamp(view.minPurity(), 0, 5)
        );
        views.set(index, sanitizedView);
        dirty = true;
        return true;
    }

    /**
     * Renombra la vista del slot {@code index}.
     *
     * @param index   slot [0, MAX_VIEWS)
     * @param newName nuevo nombre (se sanea internamente)
     * @return true si se renombró correctamente
     */
    public boolean rename(int index, String newName) {
        if (index < 0 || index >= MAX_VIEWS) {
            return false;
        }
        SavedView old = views.get(index);
        if (old == null) {
            return false;
        }
        String sanitized = SavedView.sanitizeName(newName);
        if (sanitized == null) {
            return false;
        }
        views.set(index, new SavedView(sanitized, old.searchText(), old.sortMode(), old.tierFilter(), old.minPurity()));
        dirty = true;
        return true;
    }

    /**
     * Elimina la vista del slot {@code index}.
     *
     * @param index slot [0, MAX_VIEWS)
     * @return true si había algo que eliminar
     */
    public boolean delete(int index) {
        if (index < 0 || index >= MAX_VIEWS) {
            return false;
        }
        if (views.get(index) == null) {
            return false;
        }
        views.set(index, null);
        dirty = true;
        return true;
    }

    /** Elimina todas las vistas (para migración o reinicio). */
    public void clear() {
        for (int i = 0; i < MAX_VIEWS; i++) {
            views.set(i, null);
        }
        dirty = true;
    }

    public void setDirect(int index, SavedView view) {
        if (index >= 0 && index < MAX_VIEWS) {
            views.set(index, view);
        }
    }

    // ── Utilidad ──────────────────────────────────────────────────────────────

    private static int clamp(int v, int min, int max) {
        return Math.max(min, Math.min(max, v));
    }
}
