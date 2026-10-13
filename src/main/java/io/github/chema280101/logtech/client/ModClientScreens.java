package io.github.chema280101.logtech.client;

import io.github.chema280101.logtech.LogTech;
import io.github.chema280101.logtech.registry.ModMenuTypes;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

@EventBusSubscriber(modid = LogTech.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ModClientScreens {
    @SubscribeEvent
    public static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(ModMenuTypes.TERMINAL.get(), TerminalScreen::new);
        event.register(ModMenuTypes.CRUSHER.get(), CrusherScreen::new);
        event.register(ModMenuTypes.CORE.get(), CoreScreen::new);
        event.register(ModMenuTypes.COAL_GENERATOR.get(), CoalGeneratorScreen::new);
        event.register(ModMenuTypes.CENTRIFUGE.get(), CentrifugeScreen::new);
        event.register(ModMenuTypes.WASHER.get(), WasherScreen::new);
    }
}
