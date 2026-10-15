package io.github.chema280101.logtech.item;

import io.github.chema280101.logtech.machine.firmware.FirmwareType;
import io.github.chema280101.logtech.registry.ModDataComponents;
import io.github.chema280101.logtech.security.NetworkSecurity;
import io.github.chema280101.logtech.security.PlayerAction;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

public class FirmwareItem extends Item {
    private final FirmwareType firmwareType;

    public FirmwareItem(FirmwareType firmwareType, Properties properties) {
        super(properties.stacksTo(1));
        this.firmwareType = firmwareType;
    }

    public FirmwareType getFirmwareType() {
        return this.firmwareType;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack chipStack = player.getItemInHand(hand);

        if (this.firmwareType == FirmwareType.SELECTIVE && player.isShiftKeyDown()) {
            if (!NetworkSecurity.canPlayerDo(player, PlayerAction.FLASH_FIRMWARE)) {
                return InteractionResultHolder.fail(chipStack);
            }

            InteractionHand otherHand = (hand == InteractionHand.MAIN_HAND) ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
            ItemStack otherStack = player.getItemInHand(otherHand);

            if (!otherStack.isEmpty()) {
                Item targetItem = otherStack.getItem();
                ModDataComponents.setFirmwareFilter(chipStack, targetItem);

                if (!level.isClientSide()) {
                    player.displayClientMessage(
                            Component.translatable("chat.logtech.firmware.selective_configured", targetItem.getName(otherStack))
                                    .withStyle(ChatFormatting.GREEN),
                            true
                    );
                }

                return InteractionResultHolder.sidedSuccess(chipStack, level.isClientSide());
            } else {
                if (!level.isClientSide()) {
                    // Si la otra mano está vacía con Shift + clic derecho, limpia el filtro
                    ModDataComponents.setFirmwareFilter(chipStack, null);
                    player.displayClientMessage(
                            Component.translatable("chat.logtech.firmware.selective_cleared")
                                    .withStyle(ChatFormatting.YELLOW),
                            true
                    );
                }
                return InteractionResultHolder.sidedSuccess(chipStack, level.isClientSide());
            }
        }

        return super.use(level, player, hand);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);

        switch (this.firmwareType) {
            case ECO -> tooltipComponents.add(Component.translatable("item.logtech.firmware_eco.desc").withStyle(ChatFormatting.GRAY));
            case TURBO -> tooltipComponents.add(Component.translatable("item.logtech.firmware_turbo.desc").withStyle(ChatFormatting.GRAY));
            case SAFE -> tooltipComponents.add(Component.translatable("item.logtech.firmware_safe.desc").withStyle(ChatFormatting.GRAY));
            case SELECTIVE -> {
                Item filter = ModDataComponents.getFirmwareFilter(stack);
                if (filter != null) {
                    tooltipComponents.add(
                            Component.translatable("item.logtech.firmware_selective.desc_set", filter.getName(filter.getDefaultInstance()))
                                    .withStyle(ChatFormatting.GOLD)
                    );
                } else {
                    tooltipComponents.add(
                            Component.translatable("item.logtech.firmware_selective.desc_unset")
                                    .withStyle(ChatFormatting.GRAY)
                    );
                }
            }
            case NONE -> {}
        }
    }
}
