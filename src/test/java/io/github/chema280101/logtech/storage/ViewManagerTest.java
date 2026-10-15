package io.github.chema280101.logtech.storage;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ViewManagerTest {

    private ViewManager viewManager;

    @BeforeEach
    void setUp() {
        viewManager = new ViewManager();
    }

    @Test
    @DisplayName("Inicialmente tiene 8 slots vacíos y conteo 0")
    void testInitialState() {
        assertEquals(ViewManager.MAX_VIEWS, viewManager.getAll().size());
        assertEquals(0, viewManager.count());
        for (int i = 0; i < ViewManager.MAX_VIEWS; i++) {
            assertNull(viewManager.get(i));
        }
        assertFalse(viewManager.isDirty());
    }

    @Test
    @DisplayName("Guardar en índices fuera de rango [0, 8) es rechazado")
    void testSaveOutOfBounds() {
        SavedView view = new SavedView("Minerales", "ore", SavedView.SORT_AMOUNT, SavedView.TIER_ALL, 3);
        assertFalse(viewManager.save(-1, view));
        assertFalse(viewManager.save(8, view));
        assertFalse(viewManager.save(100, view));
        assertEquals(0, viewManager.count());
    }

    @Test
    @DisplayName("Guardar y recuperar vistas en slots válidos")
    void testSaveAndGet() {
        SavedView v1 = new SavedView("Minerales", "iron", SavedView.SORT_AMOUNT, SavedView.TIER_HOT, 3);
        SavedView v2 = new SavedView("Polvos", "dust", SavedView.SORT_NAME, SavedView.TIER_COLD, 5);

        assertTrue(viewManager.save(0, v1));
        assertTrue(viewManager.save(7, v2));

        assertEquals(2, viewManager.count());
        assertTrue(viewManager.isDirty());

        SavedView r1 = viewManager.get(0);
        assertNotNull(r1);
        assertEquals("Minerales", r1.name());
        assertEquals("iron", r1.searchText());
        assertEquals(SavedView.SORT_AMOUNT, r1.sortMode());
        assertEquals(SavedView.TIER_HOT, r1.tierFilter());
        assertEquals(3, r1.minPurity());

        SavedView r2 = viewManager.get(7);
        assertNotNull(r2);
        assertEquals("Polvos", r2.name());
        assertEquals("dust", r2.searchText());
        assertEquals(SavedView.SORT_NAME, r2.sortMode());
        assertEquals(SavedView.TIER_COLD, r2.tierFilter());
        assertEquals(5, r2.minPurity());
    }

    @Test
    @DisplayName("Sanitización de nombres: truncado a 20 caracteres y eliminación de caracteres de control")
    void testSanitizeName() {
        // Nombre muy largo
        String longName = "EsteNombreEsDemasiadoLargoParaUnaVista";
        String sanitizedLong = SavedView.sanitizeName(longName);
        assertNotNull(sanitizedLong);
        assertEquals(20, sanitizedLong.length());
        assertEquals(longName.substring(0, 20), sanitizedLong);

        // Nombres vacíos o con solo espacios
        assertNull(SavedView.sanitizeName(null));
        assertNull(SavedView.sanitizeName(""));
        assertNull(SavedView.sanitizeName("    "));

        // Nombres con caracteres de control
        String withControl = "Min\u0000eral\u0007es";
        assertEquals("Minerales", SavedView.sanitizeName(withControl));

        // Rechazo al intentar guardar con nombre inválido
        SavedView invalidView = new SavedView("   ", "", 0, 0, 0);
        assertFalse(viewManager.save(0, invalidView));
        assertNull(viewManager.get(0));
    }

    @Test
    @DisplayName("Sanitización de texto de búsqueda: truncado a 50 chars y sin chars de control")
    void testSanitizeSearch() {
        String longQuery = "a".repeat(80);
        String sanitized = SavedView.sanitizeSearch(longQuery);
        assertEquals(50, sanitized.length());

        assertEquals("", SavedView.sanitizeSearch(null));
        assertEquals("query", SavedView.sanitizeSearch("que\u0000ry"));
    }

    @Test
    @DisplayName("Acotamiento de parámetros numéricos al guardar")
    void testClampParameters() {
        // pureza 99 -> 5, sortMode 10 -> SORT_NAME, tierFilter 5 -> TIER_COLD
        SavedView view = new SavedView("Test", "search", 10, 5, 99);
        assertTrue(viewManager.save(0, view));

        SavedView saved = viewManager.get(0);
        assertNotNull(saved);
        assertEquals(SavedView.SORT_NAME, saved.sortMode());
        assertEquals(SavedView.TIER_COLD, saved.tierFilter());
        assertEquals(5, saved.minPurity());
    }

    @Test
    @DisplayName("Renombrar vista existente y rechazo en slots vacíos")
    void testRename() {
        SavedView v = new SavedView("Viejo", "", 0, 0, 0);
        viewManager.save(2, v);

        assertTrue(viewManager.rename(2, "Nuevo"));
        assertEquals("Nuevo", viewManager.get(2).name());

        // Renombrar en slot vacío debe fallar
        assertFalse(viewManager.rename(3, "Invalido"));

        // Renombrar con nombre vacío debe fallar
        assertFalse(viewManager.rename(2, "   "));
        assertEquals("Nuevo", viewManager.get(2).name());
    }

    @Test
    @DisplayName("Eliminar y limpiar vistas")
    void testDeleteAndClear() {
        viewManager.save(1, new SavedView("V1", "", 0, 0, 0));
        viewManager.save(3, new SavedView("V3", "", 0, 0, 0));
        assertEquals(2, viewManager.count());

        assertTrue(viewManager.delete(1));
        assertNull(viewManager.get(1));
        assertEquals(1, viewManager.count());

        // Borrar slot ya vacío devuelve false
        assertFalse(viewManager.delete(1));

        viewManager.clear();
        assertEquals(0, viewManager.count());
        assertNull(viewManager.get(3));
    }

    @Test
    @DisplayName("getAll() devuelve una lista inmutable de 8 elementos")
    void testGetAllImmutability() {
        viewManager.save(0, new SavedView("V1", "", 0, 0, 0));
        List<SavedView> all = viewManager.getAll();
        assertEquals(ViewManager.MAX_VIEWS, all.size());
        assertThrows(UnsupportedOperationException.class, () -> all.remove(0));
    }

    @Test
    @DisplayName("SavedView.defaultFor() genera la vista base esperada")
    void testDefaultFor() {
        SavedView def = SavedView.defaultFor(1);
        assertEquals("Vista 1", def.name());
        assertEquals("", def.searchText());
        assertEquals(SavedView.SORT_AMOUNT, def.sortMode());
        assertEquals(SavedView.TIER_ALL, def.tierFilter());
        assertEquals(0, def.minPurity());
    }

    @Test
    @DisplayName("SavedView equals y hashCode")
    void testEqualsAndHashCode() {
        SavedView a = new SavedView("Vista", "search", 0, 1, 2);
        SavedView b = new SavedView("Vista", "search", 0, 1, 2);
        SavedView c = new SavedView("Otra", "search", 0, 1, 2);

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
        assertNotEquals(a, c);
    }
}
