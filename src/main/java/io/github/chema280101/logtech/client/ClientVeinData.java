package io.github.chema280101.logtech.client;

import io.github.chema280101.logtech.ore.VeinInfo;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Almacén en memoria del lado del cliente para las vetas descubiertas recibidas del servidor.
 * Servirá como fuente de datos para el renderizado del mapa en T14.
 */
public final class ClientVeinData {
    private static final Map<String, VeinInfo> DISCOVERED_VEINS = new LinkedHashMap<>();

    private ClientVeinData() {
    }

    public static synchronized void addOrUpdate(List<VeinInfo> veins) {
        for (VeinInfo vein : veins) {
            DISCOVERED_VEINS.put(vein.id(), vein);
        }
    }

    public static synchronized Collection<VeinInfo> getAll() {
        return Collections.unmodifiableCollection(DISCOVERED_VEINS.values());
    }

    public static synchronized VeinInfo get(String id) {
        return DISCOVERED_VEINS.get(id);
    }

    public static synchronized void clear() {
        DISCOVERED_VEINS.clear();
    }
}
