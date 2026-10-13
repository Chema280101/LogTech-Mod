package io.github.chema280101.logtech.registry;

import io.github.chema280101.logtech.block.BackboneItemHandler;
import io.github.chema280101.logtech.block.CoreBlockEntity;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

public class ModCapabilities {
    public static void register(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(
                Capabilities.EnergyStorage.BLOCK,
                ModBlockEntities.CORE.get(),
                (coreBe, side) -> coreBe.getEnergyStorage()
        );

        event.registerBlock(
                Capabilities.ItemHandler.BLOCK,
                (level, pos, state, blockEntity, side) -> {
                    if (level.isClientSide()) {
                        return null;
                    }
                    return new BackboneItemHandler(level, pos);
                },
                ModBlocks.BACKBONE.get()
        );

        event.registerBlock(
                Capabilities.EnergyStorage.BLOCK,
                (level, pos, state, blockEntity, side) -> {
                    if (level.isClientSide()) {
                        return null;
                    }
                    return new io.github.chema280101.logtech.block.BackboneEnergyStorage(level, pos);
                },
                ModBlocks.BACKBONE.get()
        );

        event.registerBlockEntity(
                Capabilities.EnergyStorage.BLOCK,
                ModBlockEntities.CRUSHER.get(),
                (crusherBe, side) -> crusherBe.getEnergyStorage()
        );

        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                ModBlockEntities.CRUSHER.get(),
                (crusherBe, side) -> crusherBe.getItemHandler(side)
        );

        event.registerBlockEntity(
                Capabilities.EnergyStorage.BLOCK,
                ModBlockEntities.DRILL.get(),
                (drillBe, side) -> drillBe.getEnergyStorage()
        );

        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                ModBlockEntities.DRILL.get(),
                (drillBe, side) -> drillBe.getItemHandler(side)
        );

        event.registerBlockEntity(
                Capabilities.EnergyStorage.BLOCK,
                ModBlockEntities.COAL_GENERATOR.get(),
                (genBe, side) -> genBe.getEnergyStorage()
        );

        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                ModBlockEntities.COAL_GENERATOR.get(),
                (genBe, side) -> genBe.getItemHandler()
        );

        event.registerBlockEntity(
                Capabilities.EnergyStorage.BLOCK,
                ModBlockEntities.CENTRIFUGE.get(),
                (centrifugeBe, side) -> centrifugeBe.getEnergyStorage()
        );

        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                ModBlockEntities.CENTRIFUGE.get(),
                (centrifugeBe, side) -> centrifugeBe.getItemHandler(side)
        );

        event.registerBlockEntity(
                Capabilities.EnergyStorage.BLOCK,
                ModBlockEntities.WASHER.get(),
                (washerBe, side) -> washerBe.getEnergyStorage()
        );

        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                ModBlockEntities.WASHER.get(),
                (washerBe, side) -> washerBe.getItemHandler(side)
        );

        event.registerBlockEntity(
                Capabilities.FluidHandler.BLOCK,
                ModBlockEntities.WASHER.get(),
                (washerBe, side) -> washerBe.getFluidTank()
        );
    }
}
