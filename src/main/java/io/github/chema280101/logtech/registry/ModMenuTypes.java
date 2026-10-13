package io.github.chema280101.logtech.registry;

import io.github.chema280101.logtech.LogTech;
import io.github.chema280101.logtech.block.TerminalMenu;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModMenuTypes {
    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(Registries.MENU, LogTech.MOD_ID);

    public static final DeferredHolder<MenuType<?>, MenuType<TerminalMenu>> TERMINAL =
            MENUS.register("terminal", () -> IMenuTypeExtension.create(TerminalMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<io.github.chema280101.logtech.machine.CrusherMenu>> CRUSHER =
            MENUS.register("crusher", () -> IMenuTypeExtension.create(io.github.chema280101.logtech.machine.CrusherMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<io.github.chema280101.logtech.block.CoreMenu>> CORE =
            MENUS.register("core", () -> IMenuTypeExtension.create(io.github.chema280101.logtech.block.CoreMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<io.github.chema280101.logtech.machine.CoalGeneratorMenu>> COAL_GENERATOR =
            MENUS.register("coal_generator", () -> IMenuTypeExtension.create(io.github.chema280101.logtech.machine.CoalGeneratorMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<io.github.chema280101.logtech.machine.CentrifugeMenu>> CENTRIFUGE =
            MENUS.register("centrifuge", () -> IMenuTypeExtension.create(io.github.chema280101.logtech.machine.CentrifugeMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<io.github.chema280101.logtech.machine.WasherMenu>> WASHER =
            MENUS.register("washer", () -> IMenuTypeExtension.create(io.github.chema280101.logtech.machine.WasherMenu::new));
}
