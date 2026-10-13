package io.github.chema280101.logtech.compat.jade;

import io.github.chema280101.logtech.block.ColdRackBlockEntity;
import io.github.chema280101.logtech.block.CoreBlock;
import io.github.chema280101.logtech.block.CoreBlockEntity;
import io.github.chema280101.logtech.block.RackBlock;
import io.github.chema280101.logtech.block.RackBlockEntity;
import io.github.chema280101.logtech.machine.CentrifugeBlock;
import io.github.chema280101.logtech.machine.CentrifugeBlockEntity;
import io.github.chema280101.logtech.machine.CrusherBlock;
import io.github.chema280101.logtech.machine.CrusherBlockEntity;
import io.github.chema280101.logtech.machine.DrillBlock;
import io.github.chema280101.logtech.machine.DrillBlockEntity;
import io.github.chema280101.logtech.machine.WasherBlock;
import io.github.chema280101.logtech.machine.WasherBlockEntity;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

@WailaPlugin
public class LogTechJadePlugin implements IWailaPlugin {

    @Override
    public void register(IWailaCommonRegistration registration) {
        // Máquinas (servidor)
        registration.registerBlockDataProvider(MachineJadeProvider.INSTANCE, CrusherBlockEntity.class);
        registration.registerBlockDataProvider(MachineJadeProvider.INSTANCE, WasherBlockEntity.class);
        registration.registerBlockDataProvider(MachineJadeProvider.INSTANCE, CentrifugeBlockEntity.class);
        registration.registerBlockDataProvider(MachineJadeProvider.INSTANCE, DrillBlockEntity.class);

        // Núcleo (servidor)
        registration.registerBlockDataProvider(CoreJadeProvider.INSTANCE, CoreBlockEntity.class);

        // Racks caliente y frío (servidor)
        registration.registerBlockDataProvider(RackJadeProvider.INSTANCE, RackBlockEntity.class);
        registration.registerBlockDataProvider(RackJadeProvider.INSTANCE, ColdRackBlockEntity.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        // Máquinas (cliente)
        registration.registerBlockComponent(MachineJadeProvider.INSTANCE, CrusherBlock.class);
        registration.registerBlockComponent(MachineJadeProvider.INSTANCE, WasherBlock.class);
        registration.registerBlockComponent(MachineJadeProvider.INSTANCE, CentrifugeBlock.class);
        registration.registerBlockComponent(MachineJadeProvider.INSTANCE, DrillBlock.class);

        // Núcleo (cliente)
        registration.registerBlockComponent(CoreJadeProvider.INSTANCE, CoreBlock.class);

        // Racks caliente y frío (cliente)
        registration.registerBlockComponent(RackJadeProvider.INSTANCE, RackBlock.class);
    }
}
