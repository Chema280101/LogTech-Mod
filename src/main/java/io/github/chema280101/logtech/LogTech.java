package io.github.chema280101.logtech;

import io.github.chema280101.logtech.block.CoreBlockEntity;
import io.github.chema280101.logtech.data.DataGenerators;
import io.github.chema280101.logtech.registry.ModBlockEntities;
import io.github.chema280101.logtech.registry.ModBlocks;
import io.github.chema280101.logtech.registry.ModCreativeTabs;
import io.github.chema280101.logtech.registry.ModDataComponents;
import io.github.chema280101.logtech.registry.ModItems;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;

@Mod(LogTech.MOD_ID)
public class LogTech {
    public static final String MOD_ID = "logtech";

    public LogTech(IEventBus modEventBus, ModContainer modContainer) {
        // Registro de elementos del mod
        ModBlocks.BLOCKS.register(modEventBus);
        ModBlockEntities.BLOCK_ENTITIES.register(modEventBus);
        ModItems.ITEMS.register(modEventBus);
        ModDataComponents.DATA_COMPONENTS.register(modEventBus);
        ModCreativeTabs.TABS.register(modEventBus);
        io.github.chema280101.logtech.registry.ModMenuTypes.MENUS.register(modEventBus);
        io.github.chema280101.logtech.registry.ModRecipeTypes.RECIPE_TYPES.register(modEventBus);
        io.github.chema280101.logtech.registry.ModRecipeSerializers.RECIPE_SERIALIZERS.register(modEventBus);

        // Registro de configuración del servidor
        modContainer.registerConfig(net.neoforged.fml.config.ModConfig.Type.SERVER, LogTechConfig.SPEC);

        // Registro de data generators
        modEventBus.addListener(DataGenerators::gatherData);

        // Registro de payloads de red
        modEventBus.addListener(io.github.chema280101.logtech.network.ModNetwork::registerPayloads);

        // Registro de capacidades
        modEventBus.addListener(io.github.chema280101.logtech.registry.ModCapabilities::register);

        // Limpieza de gestores de red al descargar dimensiones
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.level.LevelEvent.Unload event) -> {
            if (event.getLevel() instanceof net.minecraft.world.level.Level level) {
                io.github.chema280101.logtech.network.NetworkManager.clear(level);
                if (level.isClientSide()) {
                    io.github.chema280101.logtech.client.ClientVeinData.clear();
                }
            }
        });

        // Registro de comandos
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(io.github.chema280101.logtech.ore.DepositCommands::registerCommands);

        // Recálculo de pares de compactación al arrancar el servidor
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.server.ServerStartedEvent event) -> {
            net.minecraft.server.MinecraftServer server = event.getServer();
            io.github.chema280101.logtech.storage.CompactionRegistry.reload(server.getRecipeManager(), server.registryAccess());
        });

        // Recálculo de pares de compactación al recargar datapacks
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.OnDatapackSyncEvent event) -> {
            if (event.getPlayer() == null) {
                net.minecraft.server.MinecraftServer server = event.getPlayerList().getServer();
                if (server != null) {
                    io.github.chema280101.logtech.storage.CompactionRegistry.reload(server.getRecipeManager(), server.registryAccess());
                } else {
                    io.github.chema280101.logtech.storage.CompactionRegistry.markDirty();
                }
            }
        });

        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.AddReloadListenerEvent event) -> {
            io.github.chema280101.logtech.storage.CompactionRegistry.markDirty();
        });

        // Protección contra rotura del Núcleo en redes privadas
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.level.BlockEvent.BreakEvent event) -> {
            if (event.getState().is(ModBlocks.CORE.get()) && event.getLevel() instanceof net.minecraft.world.level.Level level && !level.isClientSide()) {
                net.minecraft.world.level.block.entity.BlockEntity be = level.getBlockEntity(event.getPos());
                if (be instanceof CoreBlockEntity coreBe) {
                    if (!io.github.chema280101.logtech.security.NetworkSecurity.canPlayerDo(event.getPlayer(), io.github.chema280101.logtech.security.PlayerAction.BREAK_CORE, coreBe)) {
                        event.setCanceled(true);
                        event.getPlayer().displayClientMessage(
                                net.minecraft.network.chat.Component.translatable("chat.logtech.security.cannot_break_private"),
                                true
                        );
                    }
                }
            }
        });
    }
}
