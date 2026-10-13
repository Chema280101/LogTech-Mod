package io.github.chema280101.logtech.registry;

import io.github.chema280101.logtech.LogTech;
import io.github.chema280101.logtech.block.CoreBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, LogTech.MOD_ID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CoreBlockEntity>> CORE =
            BLOCK_ENTITIES.register("core", () ->
                    BlockEntityType.Builder.of(CoreBlockEntity::new, ModBlocks.CORE.get()).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<io.github.chema280101.logtech.block.RackBlockEntity>> RACK =
            BLOCK_ENTITIES.register("rack", () ->
                    BlockEntityType.Builder.of(io.github.chema280101.logtech.block.RackBlockEntity::new, ModBlocks.RACK.get()).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<io.github.chema280101.logtech.block.ColdRackBlockEntity>> COLD_RACK =
            BLOCK_ENTITIES.register("cold_rack", () ->
                    BlockEntityType.Builder.of(io.github.chema280101.logtech.block.ColdRackBlockEntity::new, ModBlocks.COLD_RACK.get()).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<io.github.chema280101.logtech.machine.CrusherBlockEntity>> CRUSHER =
            BLOCK_ENTITIES.register("crusher", () ->
                    BlockEntityType.Builder.of(io.github.chema280101.logtech.machine.CrusherBlockEntity::new, ModBlocks.CRUSHER.get()).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<io.github.chema280101.logtech.machine.DrillBlockEntity>> DRILL =
            BLOCK_ENTITIES.register("drill", () ->
                    BlockEntityType.Builder.of(io.github.chema280101.logtech.machine.DrillBlockEntity::new, ModBlocks.DRILL.get()).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<io.github.chema280101.logtech.block.AlarmBlockEntity>> ALARM =
            BLOCK_ENTITIES.register("alarm", () ->
                    BlockEntityType.Builder.of(io.github.chema280101.logtech.block.AlarmBlockEntity::new, ModBlocks.ALARM.get()).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<io.github.chema280101.logtech.machine.CoalGeneratorBlockEntity>> COAL_GENERATOR =
            BLOCK_ENTITIES.register("coal_generator", () ->
                    BlockEntityType.Builder.of(io.github.chema280101.logtech.machine.CoalGeneratorBlockEntity::new, ModBlocks.COAL_GENERATOR.get()).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<io.github.chema280101.logtech.machine.CentrifugeBlockEntity>> CENTRIFUGE =
            BLOCK_ENTITIES.register("centrifuge", () ->
                    BlockEntityType.Builder.of(io.github.chema280101.logtech.machine.CentrifugeBlockEntity::new, ModBlocks.CENTRIFUGE.get()).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<io.github.chema280101.logtech.machine.WasherBlockEntity>> WASHER =
            BLOCK_ENTITIES.register("washer", () ->
                    BlockEntityType.Builder.of(io.github.chema280101.logtech.machine.WasherBlockEntity::new, ModBlocks.WASHER.get()).build(null)
            );
}
