package io.github.chema280101.logtech.item;

import io.github.chema280101.logtech.network.DiscoveredVeinsPayload;
import io.github.chema280101.logtech.network.OpenVeinMapPayload;
import io.github.chema280101.logtech.ore.DepositSavedData;
import io.github.chema280101.logtech.ore.DepositVein;
import io.github.chema280101.logtech.ore.VeinGenerator;
import io.github.chema280101.logtech.ore.VeinInfo;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Escáner de mano para prospección geológica.
 * Al usarlo, busca vetas de mineral deterministas en un radio de 96 bloques,
 * las marca como descubiertas de forma persistente y sincroniza los datos al cliente.
 * Con Shift + click derecho, abre el mapa 2D de vetas descubiertas.
 */
public class ScannerItem extends Item {
    public ScannerItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        // Si el jugador está agachado (Shift + click derecho), abre el mapa 2D de vetas descubiertas
        if (player.isShiftKeyDown()) {
            if (!level.isClientSide()) {
                if (level.dimension() != Level.OVERWORLD) {
                    player.displayClientMessage(Component.translatable("action.logtech.scanner.overworld_only"), true);
                    return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
                }

                if (level instanceof ServerLevel serverLevel && player instanceof ServerPlayer serverPlayer) {
                    DepositSavedData savedData = DepositSavedData.get(serverLevel);
                    List<VeinInfo> discovered = savedData.getAllDiscoveredVeinInfos(serverLevel.getSeed());
                    PacketDistributor.sendToPlayer(serverPlayer, new OpenVeinMapPayload(discovered));
                }
            }
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
        }

        if (player.getCooldowns().isOnCooldown(this)) {
            return InteractionResultHolder.fail(stack);
        }

        // Toda la lógica de búsqueda y persistencia se ejecuta únicamente en el servidor
        if (!level.isClientSide()) {
            player.getCooldowns().addCooldown(this, io.github.chema280101.logtech.LogTechConfig.SCANNER_COOLDOWN.get());

            if (level.dimension() != Level.OVERWORLD) {
                player.displayClientMessage(Component.translatable("action.logtech.scanner.overworld_only"), true);
                return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
            }

            if (level instanceof ServerLevel serverLevel) {
                long seed = serverLevel.getSeed();
                int px = (int) Math.floor(player.getX());
                int pz = (int) Math.floor(player.getZ());

                // Cálculo puro determinista sin recorrer chunks
                int radius = io.github.chema280101.logtech.LogTechConfig.SCANNER_RADIUS.get();
                List<DepositVein> nearby = VeinGenerator.getVeinsNear(seed, px, pz, radius);
                DepositSavedData savedData = DepositSavedData.get(serverLevel);

                List<VeinInfo> veinInfos = new ArrayList<>(nearby.size());
                for (DepositVein vein : nearby) {
                    savedData.setDiscovered(vein, true);
                    veinInfos.add(VeinInfo.from(vein, savedData));
                }

                int count = nearby.size();
                Component message;
                if (count == 0) {
                    message = Component.translatable("action.logtech.scanner.found_zero");
                } else if (count == 1) {
                    message = Component.translatable("action.logtech.scanner.found_single");
                } else {
                    message = Component.translatable("action.logtech.scanner.found_multiple", count);
                }
                player.displayClientMessage(message, true);

                level.playSound(
                        null,
                        player.getX(),
                        player.getY(),
                        player.getZ(),
                        SoundEvents.EXPERIENCE_ORB_PICKUP,
                        SoundSource.PLAYERS,
                        0.8F,
                        1.2F
                );

                if (player instanceof ServerPlayer serverPlayer) {
                    PacketDistributor.sendToPlayer(serverPlayer, new DiscoveredVeinsPayload(veinInfos));
                }
            }
        }

        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }
}
