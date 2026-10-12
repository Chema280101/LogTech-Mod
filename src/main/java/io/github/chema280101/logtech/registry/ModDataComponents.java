package io.github.chema280101.logtech.registry;

import io.github.chema280101.logtech.LogTech;
import io.github.chema280101.logtech.storage.StorageContent;
import net.minecraft.core.component.DataComponentType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModDataComponents {
    public static final DeferredRegister.DataComponents DATA_COMPONENTS = DeferredRegister.createDataComponents(LogTech.MOD_ID);

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<StorageContent>> STORAGE_CONTENT =
            DATA_COMPONENTS.registerComponentType("storage_content", builder -> builder
                    .persistent(StorageContent.CODEC)
                    .networkSynchronized(StorageContent.STREAM_CODEC));
}
