package io.github.chema280101.logtech.data;

import io.github.chema280101.logtech.LogTech;
import io.github.chema280101.logtech.registry.ModBlocks;
import io.github.chema280101.logtech.registry.ModItems;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.LanguageProvider;

public class ModLanguageProvider {
    public static class English extends LanguageProvider {
        public English(PackOutput output) {
            super(output, LogTech.MOD_ID, "en_us");
        }

        @Override
        protected void addTranslations() {
            addBlock(ModBlocks.CORE, "Core");
            addBlock(ModBlocks.BACKBONE, "Backbone");
            addBlock(ModBlocks.RACK, "Rack");
            addBlock(ModBlocks.TERMINAL, "Terminal");
            addBlock(ModBlocks.CRUSHER, "Crusher");
            addBlock(ModBlocks.DRILL, "Drill");
            addItem(ModItems.DATA_MODULE, "Data Module");
            addItem(ModItems.RAW_NEOFERRITE, "Raw Neoferrite");
            addItem(ModItems.NEOFERRITE_DUST, "Neoferrite Dust");
            addItem(ModItems.NEOFERRITE_INGOT, "Neoferrite Ingot");
            addItem(ModItems.RAW_CRYSTALITE, "Raw Crystalite");
            addItem(ModItems.CRYSTALITE_DUST, "Crystalite Dust");
            addItem(ModItems.DATA_CRYSTAL, "Data Crystal");
            addItem(ModItems.SCANNER, "Scanner");
            add("itemGroup.logtech", "LogTech");
            add("container.logtech.terminal", "Storage Terminal");
            add("container.logtech.crusher", "Crusher");
            add("gui.logtech.machine_status.working", "Working");
            add("gui.logtech.machine_status.idle", "Idle");
            add("gui.logtech.machine_status.no_energy", "No Energy");
            add("gui.logtech.machine_status.no_input", "No Material");
            add("gui.logtech.machine_status.output_full", "Output Full");
            add("gui.logtech.machine_status.no_resource", "No Resource");
            add("gui.logtech.machine_status.cause.working", "Active: Working");
            add("gui.logtech.machine_status.cause.idle", "Inactive: Idle");
            add("gui.logtech.machine_status.cause.no_energy", "Inactive: No power");
            add("gui.logtech.machine_status.cause.no_input", "Inactive: No material");
            add("gui.logtech.machine_status.cause.output_full", "Inactive: Output full");
            add("gui.logtech.machine_status.cause.no_resource", "Inactive: No resource");
            add("chat.logtech.drill.status", "Drill: %s | Energy: %s / %s FE | Buffer: %s / 64");
            add("gui.logtech.terminal.stored_amount", "Stored: %s");
            add("gui.logtech.terminal.network_invalid", "Network Invalid / Disconnected");
            add("gui.logtech.terminal.no_energy", "Network Offline: No Power");
            add("gui.logtech.terminal.search", "Search");
            add("gui.logtech.terminal.search_hint", "Search...");
            add("gui.logtech.terminal.sort.amount_icon", "123");
            add("gui.logtech.terminal.sort.name_icon", "AZ");
            add("gui.logtech.terminal.sort.by_amount", "Sort by: Quantity");
            add("gui.logtech.terminal.sort.by_name", "Sort by: Name");
            add("gui.logtech.terminal.tab.items", "Items");
            add("gui.logtech.terminal.tab.stats", "Stats");
            add("gui.logtech.terminal.stats.storage", "Storage: %s / %s (%s%%)");
            add("gui.logtech.terminal.stats.items_per_minute", "Input: %s/min (5m avg: %s/min)");
            add("gui.logtech.terminal.stats.energy", "Energy: %s / %s FE (%s FE/t)");
            add("gui.logtech.terminal.stats.chart_title", "60m Insertion History:");
            add("gui.logtech.terminal.stats.chart_tooltip_current", "Current min: %s items");
            add("gui.logtech.terminal.stats.chart_tooltip_past", "%s min ago: %s items");
            add("gui.logtech.terminal.stats.machines_title", "Active Machines (%s):");
            add("gui.logtech.terminal.stats.machine_entry", "%s: %s (%s/min)");
            add("gui.logtech.terminal.stats.no_machines", "No machines connected to network");
            add("gui.logtech.terminal.stats.loading", "Loading statistics...");
            add("chat.logtech.core.status", "Core storage: %s item types (Total: %s items)");
            add("chat.logtech.core.status_capacity", "Core storage: %s item types (%s / %s items)");
            add("chat.logtech.core.status_overloaded", "Core storage: %s item types (%s / %s items - Overloaded!)");
            add("chat.logtech.core.status_no_energy", "Network Offline: No Power (%s / %s FE, requires %s FE/t)");
            add("chat.logtech.core.status_energy", "Energy: %s / %s FE (%s FE/t consumption)");
            add("chat.logtech.core.machine_status", "%s: %s");
            add("chat.logtech.core.inserted", "Stored %sx %s (Total: %s)");
            add("chat.logtech.core.cannot_insert_invalid", "Cannot store items: Invalid network!");
            add("chat.logtech.core.cannot_insert_overloaded", "Cannot store items: Network overloaded!");
            add("chat.logtech.core.cannot_insert_no_capacity", "Cannot store items: No storage capacity (requires racks with data modules)!");
            add("chat.logtech.core.cannot_insert_no_energy", "Cannot store items: No power!");
            add("chat.logtech.core.cannot_insert_full", "Cannot store items: Storage full!");
            add("action.logtech.core.network_valid", "Network: %s blocks (Valid)");
            add("action.logtech.core.network_valid_singular", "Network: %s block (Valid)");
            add("action.logtech.core.network_invalid", "Network: %s blocks (Invalid)");
            add("action.logtech.core.network_invalid_singular", "Network: %s block (Invalid)");
            add("action.logtech.core.network_overloaded", "Network: %s blocks (Overloaded: %s / %s)");
            add("action.logtech.core.network_no_energy", "Network: %s blocks (No Power: %s / %s FE)");
            add("chat.logtech.rack.inserted", "Data Module inserted (%s/%s - Capacity: %s)");
            add("chat.logtech.rack.extracted", "Data Module extracted (%s/%s - Capacity: %s)");
            add("chat.logtech.rack.full", "Rack is full (%s/%s)");
            add("chat.logtech.rack.empty", "Rack is empty");
            add("chat.logtech.rack.status", "Rack: %s/%s modules (Capacity: %s)");
            add("action.logtech.scanner.found_zero", "No ore veins found");
            add("action.logtech.scanner.found_single", "1 ore vein found");
            add("action.logtech.scanner.found_multiple", "%s ore veins found");
            add("action.logtech.scanner.overworld_only", "Ore veins can only be found in the Overworld");
            add("gui.logtech.vein_map.title", "Geological Survey Map");
            add("gui.logtech.vein_map.snapshot_notice", "* Static values recorded at opening time");
            add("gui.logtech.vein_map.type", "Type: %s");
            add("gui.logtech.vein_map.purity", "Purity: %s / 5");
            add("gui.logtech.vein_map.coordinates", "Coordinates: X: %s, Z: %s");
            add("gui.logtech.vein_map.reserve", "Reserve: %s / %s (%s%%)");
            add("gui.logtech.vein_map.static_value_note", "(Value at opening time)");
            add("gui.logtech.vein_map.player", "Player Position");
            add("gui.logtech.vein_map.help", "Discovered: %s | Zoom: Scroll (%.0f%%) | Pan: Drag | [Space/R] Reset");
            add("gui.logtech.vein_map.no_veins", "No veins discovered yet. Use the Scanner to prospect.");
        }
    }

    public static class Spanish extends LanguageProvider {
        public Spanish(PackOutput output, String locale) {
            super(output, LogTech.MOD_ID, locale);
        }

        @Override
        protected void addTranslations() {
            addBlock(ModBlocks.CORE, "Núcleo");
            addBlock(ModBlocks.BACKBONE, "Backbone");
            addBlock(ModBlocks.RACK, "Rack");
            addBlock(ModBlocks.TERMINAL, "Terminal");
            addBlock(ModBlocks.CRUSHER, "Triturador");
            addBlock(ModBlocks.DRILL, "Perforadora");
            addItem(ModItems.DATA_MODULE, "Módulo de datos");
            addItem(ModItems.RAW_NEOFERRITE, "Neoferrita bruta");
            addItem(ModItems.NEOFERRITE_DUST, "Polvo de neoferrita");
            addItem(ModItems.NEOFERRITE_INGOT, "Lingote de neoferrita");
            addItem(ModItems.RAW_CRYSTALITE, "Cristalita bruta");
            addItem(ModItems.CRYSTALITE_DUST, "Polvo de cristalita");
            addItem(ModItems.DATA_CRYSTAL, "Cristal de datos");
            addItem(ModItems.SCANNER, "Escáner");
            add("itemGroup.logtech", "LogTech");
            add("container.logtech.terminal", "Terminal de almacenamiento");
            add("container.logtech.crusher", "Triturador");
            add("gui.logtech.machine_status.working", "Trabajando");
            add("gui.logtech.machine_status.idle", "Inactiva");
            add("gui.logtech.machine_status.no_energy", "Sin energía");
            add("gui.logtech.machine_status.no_input", "Sin material");
            add("gui.logtech.machine_status.output_full", "Salida llena");
            add("gui.logtech.machine_status.no_resource", "Sin recurso");
            add("gui.logtech.machine_status.cause.working", "Activa: trabajando");
            add("gui.logtech.machine_status.cause.idle", "Inactiva: en espera");
            add("gui.logtech.machine_status.cause.no_energy", "Inactiva: sin energía");
            add("gui.logtech.machine_status.cause.no_input", "Inactiva: sin material");
            add("gui.logtech.machine_status.cause.output_full", "Inactiva: salida llena");
            add("gui.logtech.machine_status.cause.no_resource", "Inactiva: sin recurso");
            add("chat.logtech.drill.status", "Perforadora: %s | Energía: %s / %s FE | Búfer: %s / 64");
            add("gui.logtech.terminal.stored_amount", "Guardado: %s");
            add("gui.logtech.terminal.network_invalid", "Red inválida / Desconectada");
            add("gui.logtech.terminal.no_energy", "Red apagada: sin energía");
            add("gui.logtech.terminal.search", "Buscar");
            add("gui.logtech.terminal.search_hint", "Buscar...");
            add("gui.logtech.terminal.sort.amount_icon", "123");
            add("gui.logtech.terminal.sort.name_icon", "AZ");
            add("gui.logtech.terminal.sort.by_amount", "Ordenar por: Cantidad");
            add("gui.logtech.terminal.sort.by_name", "Ordenar por: Nombre");
            add("gui.logtech.terminal.tab.items", "Ítems");
            add("gui.logtech.terminal.tab.stats", "Estadísticas");
            add("gui.logtech.terminal.stats.storage", "Almacenamiento: %s / %s (%s%%)");
            add("gui.logtech.terminal.stats.items_per_minute", "Entrada: %s/min (media 5m: %s/min)");
            add("gui.logtech.terminal.stats.energy", "Energía: %s / %s FE (%s FE/t)");
            add("gui.logtech.terminal.stats.chart_title", "Historial de inserción (60 min):");
            add("gui.logtech.terminal.stats.chart_tooltip_current", "Minuto actual: %s ítems");
            add("gui.logtech.terminal.stats.chart_tooltip_past", "Hace %s min: %s ítems");
            add("gui.logtech.terminal.stats.machines_title", "Máquinas activas (%s):");
            add("gui.logtech.terminal.stats.machine_entry", "%s: %s (%s/min)");
            add("gui.logtech.terminal.stats.no_machines", "No hay máquinas conectadas a la red");
            add("gui.logtech.terminal.stats.loading", "Cargando estadísticas...");
            add("chat.logtech.core.status", "Almacenamiento del Núcleo: %s tipos de ítems (Total: %s ítems)");
            add("chat.logtech.core.status_capacity", "Almacenamiento del Núcleo: %s tipos de ítems (%s / %s ítems)");
            add("chat.logtech.core.status_overloaded", "Almacenamiento del Núcleo: %s tipos de ítems (%s / %s ítems - ¡Sobrecargada!)");
            add("chat.logtech.core.status_no_energy", "Red apagada: Sin energía (%s / %s FE, requiere %s FE/t)");
            add("chat.logtech.core.status_energy", "Energía: %s / %s FE (Consumo: %s FE/t)");
            add("chat.logtech.core.machine_status", "%s: %s");
            add("chat.logtech.core.inserted", "Guardado %sx %s (Total: %s)");
            add("chat.logtech.core.cannot_insert_invalid", "No se pueden guardar ítems: ¡Red inválida!");
            add("chat.logtech.core.cannot_insert_overloaded", "No se pueden guardar ítems: ¡Red sobrecargada!");
            add("chat.logtech.core.cannot_insert_no_capacity", "No se pueden guardar ítems: Sin capacidad (requiere racks con módulos de datos)");
            add("chat.logtech.core.cannot_insert_no_energy", "No se pueden guardar ítems: ¡Sin energía!");
            add("chat.logtech.core.cannot_insert_full", "No se pueden guardar ítems: ¡Almacenamiento lleno!");
            add("action.logtech.core.network_valid", "Red: %s bloques (Válida)");
            add("action.logtech.core.network_valid_singular", "Red: %s bloque (Válida)");
            add("action.logtech.core.network_invalid", "Red: %s bloques (Inválida)");
            add("action.logtech.core.network_invalid_singular", "Red: %s bloque (Inválida)");
            add("action.logtech.core.network_overloaded", "Red: %s bloques (Sobrecargada: %s / %s)");
            add("action.logtech.core.network_no_energy", "Red: %s bloques (Sin energía: %s / %s FE)");
            add("chat.logtech.rack.inserted", "Módulo de datos insertado (%s/%s - Capacidad: %s)");
            add("chat.logtech.rack.extracted", "Módulo de datos extraído (%s/%s - Capacidad: %s)");
            add("chat.logtech.rack.full", "El Rack está lleno (%s/%s)");
            add("chat.logtech.rack.empty", "El Rack está vacío");
            add("chat.logtech.rack.status", "Rack: %s/%s módulos (Capacidad: %s)");
            add("action.logtech.scanner.found_zero", "No se encontraron vetas");
            add("action.logtech.scanner.found_single", "1 veta encontrada");
            add("action.logtech.scanner.found_multiple", "%s vetas encontradas");
            add("action.logtech.scanner.overworld_only", "Las vetas de mineral solo se encuentran en el Overworld");
            add("gui.logtech.vein_map.title", "Mapa de Prospección Geológica");
            add("gui.logtech.vein_map.snapshot_notice", "* Datos estáticos registrados al abrir la pantalla");
            add("gui.logtech.vein_map.type", "Tipo: %s");
            add("gui.logtech.vein_map.purity", "Pureza: %s / 5");
            add("gui.logtech.vein_map.coordinates", "Coordenadas: X: %s, Z: %s");
            add("gui.logtech.vein_map.reserve", "Reserva: %s / %s (%s%%)");
            add("gui.logtech.vein_map.static_value_note", "(Valor al abrir la pantalla)");
            add("gui.logtech.vein_map.player", "Posición del jugador");
            add("gui.logtech.vein_map.help", "Descubiertas: %s | Zoom: Rueda (%.0f%%) | Mover: Arrastrar | [Espacio/R] Centrar");
            add("gui.logtech.vein_map.no_veins", "No hay vetas descubiertas aún. Usa el Escáner para prospectar.");
        }
    }
}
