package io.github.chema280101.logtech.block;

import net.minecraft.world.level.block.Block;

/**
 * Bloque disipador de calor pasivo.
 * Colocado adyacente a cualquier máquina de LogTech, absorbe y disipa el calor generado,
 * aumentando la tasa de enfriamiento continuo de la máquina.
 */
public class HeatsinkBlock extends Block {

    public HeatsinkBlock(Properties properties) {
        super(properties);
    }
}
