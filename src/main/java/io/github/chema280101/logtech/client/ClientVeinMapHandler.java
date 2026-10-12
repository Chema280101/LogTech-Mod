package io.github.chema280101.logtech.client;

import io.github.chema280101.logtech.network.OpenVeinMapPayload;
import net.minecraft.client.Minecraft;

/**
 * Manejador exclusivo del lado cliente para abrir la pantalla del mapa 2D de vetas descubiertas.
 * Aísla completamente la llamada a Minecraft.getInstance() y a VeinMapScreen del código común.
 */
public final class ClientVeinMapHandler {
    private ClientVeinMapHandler() {
    }

    public static void handle(OpenVeinMapPayload payload) {
        ClientVeinData.addOrUpdate(payload.veins());
        Minecraft.getInstance().setScreen(new VeinMapScreen(payload.veins()));
    }
}
