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

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<StorageContent>> COLD_STORAGE_CONTENT =
            DATA_COMPONENTS.registerComponentType("cold_storage_content", builder -> builder
                    .persistent(StorageContent.CODEC)
                    .networkSynchronized(StorageContent.STREAM_CODEC));

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> PURITY =
            DATA_COMPONENTS.registerComponentType("purity", builder -> builder
                    .persistent(com.mojang.serialization.Codec.intRange(1, 5))
                    .networkSynchronized(net.minecraft.network.codec.ByteBufCodecs.VAR_INT));

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<net.minecraft.world.item.Item>> FIRMWARE_FILTER =
            DATA_COMPONENTS.registerComponentType("firmware_filter", builder -> builder
                    .persistent(net.minecraft.core.registries.BuiltInRegistries.ITEM.byNameCodec())
                    .networkSynchronized(net.minecraft.network.codec.ByteBufCodecs.registry(net.minecraft.core.registries.Registries.ITEM)));

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<io.github.chema280101.logtech.block.ConfigPackage>> CONFIG_DATA =
            DATA_COMPONENTS.registerComponentType("config_data", builder -> builder
                    .persistent(io.github.chema280101.logtech.block.ConfigPackage.CODEC)
                    .networkSynchronized(io.github.chema280101.logtech.block.ConfigPackage.STREAM_CODEC));

    public static int getPurity(net.minecraft.world.item.ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return 1;
        }
        return stack.getOrDefault(PURITY.get(), 1);
    }

    public static void setPurity(net.minecraft.world.item.ItemStack stack, int purity) {
        if (stack != null && !stack.isEmpty()) {
            stack.set(PURITY.get(), Math.max(1, Math.min(5, purity)));
        }
    }

    public static net.minecraft.world.item.Item getFirmwareFilter(net.minecraft.world.item.ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return null;
        }
        return stack.get(FIRMWARE_FILTER.get());
    }

    public static void setFirmwareFilter(net.minecraft.world.item.ItemStack stack, net.minecraft.world.item.Item item) {
        if (stack != null && !stack.isEmpty()) {
            if (item != null) {
                stack.set(FIRMWARE_FILTER.get(), item);
            } else {
                stack.remove(FIRMWARE_FILTER.get());
            }
        }
    }
}
