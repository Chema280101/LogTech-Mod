package io.github.chema280101.logtech.item;

import io.github.chema280101.logtech.block.ConfigPackage;
import io.github.chema280101.logtech.block.ConfigTypes;
import io.github.chema280101.logtech.block.Configurable;
import io.github.chema280101.logtech.registry.ModDataComponents;
import io.github.chema280101.logtech.security.NetworkSecurity;
import io.github.chema280101.logtech.security.PlayerAction;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.List;

/**
 * Llave de configuración de LogTech.
 * Permite copiar ajustes de un bloque configurable mediante Shift + Clic derecho,
 * y pegarlos en otro bloque compatible mediante Clic derecho.
 */
public class ConfigKeyItem extends Item {

    public ConfigKeyItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        Player player = context.getPlayer();
        ItemStack stack = context.getItemInHand();

        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof Configurable configurable)) {
            return InteractionResult.PASS;
        }

        if (player != null && player.isShiftKeyDown()) {
            // COPIAR AJUSTES
            if (!level.isClientSide()) {
                CompoundTag exported = configurable.exportConfig(level.registryAccess());
                String type = configurable.getConfigType();
                ConfigPackage pkg = new ConfigPackage(type, exported);
                stack.set(ModDataComponents.CONFIG_DATA.get(), pkg);

                level.playSound(null, pos, SoundEvents.NOTE_BLOCK_CHIME.value(), SoundSource.PLAYERS, 0.6f, 1.3f);
                Component typeComp = Component.translatable("config_type.logtech." + type);
                player.displayClientMessage(
                        Component.translatable("action.logtech.config_key.copied", typeComp),
                        true
                );
            }
            return InteractionResult.sidedSuccess(level.isClientSide());
        } else {
            // PEGAR AJUSTES
            ConfigPackage pkg = stack.get(ModDataComponents.CONFIG_DATA.get());
            if (pkg == null) {
                if (!level.isClientSide() && player != null) {
                    player.displayClientMessage(
                            Component.translatable("action.logtech.config_key.empty"),
                            true
                    );
                }
                return InteractionResult.sidedSuccess(level.isClientSide());
            }

            // Comprobar compatibilidad de tipos
            if (!pkg.type().equals(configurable.getConfigType())) {
                if (!level.isClientSide() && player != null) {
                    Component sourceComp = Component.translatable("config_type.logtech." + pkg.type());
                    Component targetComp = Component.translatable("config_type.logtech." + configurable.getConfigType());
                    player.displayClientMessage(
                            Component.translatable("action.logtech.config_key.incompatible", sourceComp, targetComp),
                            true
                    );
                }
                return InteractionResult.sidedSuccess(level.isClientSide());
            }

            // Validar en el servidor y chequear permisos
            if (!level.isClientSide()) {
                if (!NetworkSecurity.canPlayerDo(player, PlayerAction.PASTE_CONFIG, be)) {
                    return InteractionResult.FAIL;
                }

                boolean accepted = configurable.importConfig(pkg.data(), level.registryAccess(), player);
                if (accepted) {
                    level.playSound(null, pos, SoundEvents.NOTE_BLOCK_BELL.value(), SoundSource.PLAYERS, 0.7f, 1.2f);
                    Component typeComp = Component.translatable("config_type.logtech." + pkg.type());
                    if (player != null) {
                        player.displayClientMessage(
                                Component.translatable("action.logtech.config_key.pasted", typeComp),
                                true
                        );
                    }
                } else {
                    if (player != null) {
                        player.displayClientMessage(
                                Component.translatable("action.logtech.config_key.version_mismatch"),
                                true
                        );
                    }
                }
            }
            return InteractionResult.sidedSuccess(level.isClientSide());
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        ConfigPackage pkg = stack.get(ModDataComponents.CONFIG_DATA.get());
        if (pkg == null) {
            tooltip.add(Component.translatable("tooltip.logtech.config_key.empty").withStyle(ChatFormatting.GRAY));
            return;
        }

        Component typeName = Component.translatable("config_type.logtech." + pkg.type()).withStyle(ChatFormatting.AQUA);
        tooltip.add(Component.translatable("tooltip.logtech.config_key.type", typeName).withStyle(ChatFormatting.YELLOW));

        CompoundTag data = pkg.data();
        if (ConfigTypes.MACHINE.equals(pkg.type())) {
            String prio = data.getString("energy_priority");
            Component prioName = Component.translatable("logtech.priority." + prio.toLowerCase()).withStyle(ChatFormatting.WHITE);
            tooltip.add(Component.translatable("tooltip.logtech.config_key.priority", prioName).withStyle(ChatFormatting.GRAY));
        } else if (ConfigTypes.ALARM.equals(pkg.type())) {
            int ch = data.getInt("channel");
            tooltip.add(Component.translatable("tooltip.logtech.config_key.channel", Component.literal(String.valueOf(ch)).withStyle(ChatFormatting.WHITE)).withStyle(ChatFormatting.GRAY));
        } else if (ConfigTypes.CORE.equals(pkg.type())) {
            int rules = 0;
            if (data.contains("automation_rules", Tag.TAG_LIST)) {
                rules = data.getList("automation_rules", Tag.TAG_COMPOUND).size();
            }
            int pins = 0;
            if (data.contains("item_policies", Tag.TAG_LIST)) {
                pins = data.getList("item_policies", Tag.TAG_COMPOUND).size();
            }
            tooltip.add(Component.translatable("tooltip.logtech.config_key.core_details",
                    Component.literal(String.valueOf(rules)).withStyle(ChatFormatting.WHITE),
                    Component.literal(String.valueOf(pins)).withStyle(ChatFormatting.WHITE)).withStyle(ChatFormatting.GRAY));
        }

        tooltip.add(Component.translatable("tooltip.logtech.config_key.paste_hint").withStyle(ChatFormatting.DARK_GRAY));
    }
}
