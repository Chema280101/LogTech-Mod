package io.github.chema280101.logtech.block;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

/**
 * Interfaz que define un bloque o entidad de bloque capaz de exportar e importar
 * sus ajustes de configuración a través de la Llave de Configuración (config_key).
 */
public interface Configurable {

    /**
     * Devuelve el identificador del tipo de ajustes que soporta este bloque
     * (por ejemplo: "machine", "alarm", "core", "wall_display").
     *
     * @return El identificador del tipo de configuración.
     */
    String getConfigType();

    /**
     * Exporta los ajustes actuales de este bloque en un {@link CompoundTag}.
     *
     * @param registries Proveedor de registros del nivel para serialización NBT/Codecs.
     * @return Un CompoundTag con los datos exportados.
     */
    CompoundTag exportConfig(HolderLookup.Provider registries);

    /**
     * Importa y valida los ajustes desde un {@link CompoundTag}.
     *
     * @param tag Datos de configuración a importar.
     * @param registries Proveedor de registros del nivel.
     * @param player Jugador que realiza la acción (puede ser null).
     * @return true si los ajustes fueron aceptados y aplicados, false en caso contrario.
     */
    boolean importConfig(CompoundTag tag, HolderLookup.Provider registries, @Nullable Player player);
}
