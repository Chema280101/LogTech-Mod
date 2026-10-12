package io.github.chema280101.logtech.ore;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import java.util.List;

public class DepositCommands {

    public static void registerCommands(RegisterCommandsEvent event) {
        register(event.getDispatcher());
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
            Commands.literal("logtech")
                .then(Commands.literal("deposits")
                    .requires(source -> source.hasPermission(2))
                    .executes(context -> listDeposits(context.getSource()))
                    .then(Commands.literal("set_reserve")
                        .then(Commands.argument("vein_id", StringArgumentType.word())
                            .then(Commands.argument("amount", IntegerArgumentType.integer(0))
                                .executes(context -> setReserve(
                                        context.getSource(),
                                        StringArgumentType.getString(context, "vein_id"),
                                        IntegerArgumentType.getInteger(context, "amount")
                                ))
                            )
                        )
                    )
                    .then(Commands.literal("set_discovered")
                        .then(Commands.argument("vein_id", StringArgumentType.word())
                            .then(Commands.argument("discovered", BoolArgumentType.bool())
                                .executes(context -> setDiscovered(
                                        context.getSource(),
                                        StringArgumentType.getString(context, "vein_id"),
                                        BoolArgumentType.getBool(context, "discovered")
                                ))
                            )
                        )
                    )
                )
        );
    }

    private static int listDeposits(CommandSourceStack source) {
        ServerLevel level = source.getLevel();
        if (level.dimension() != Level.OVERWORLD) {
            source.sendFailure(Component.literal("§c[LogTech] Las vetas de mineral solo se generan en el Overworld."));
            return 0;
        }

        long seed = level.getSeed();
        Vec3 pos = source.getPosition();
        int px = (int) Math.floor(pos.x);
        int pz = (int) Math.floor(pos.z);

        List<DepositVein> nearby = VeinGenerator.getVeinsNear(seed, px, pz, 256);
        DepositSavedData savedData = DepositSavedData.get(level);

        if (nearby.isEmpty()) {
            source.sendSuccess(() -> Component.literal("§e[LogTech] No se encontraron vetas de mineral en un radio de 256 bloques."), false);
            return 1;
        }

        source.sendSuccess(() -> Component.literal("§6=== Vetas de mineral en radio de 256 bloques (Total: " + nearby.size() + ") ==="), false);

        for (DepositVein vein : nearby) {
            DepositType type = vein.getType();
            int remaining = savedData.getRemainingReserve(vein);
            boolean discovered = savedData.isDiscovered(vein);
            double dist = vein.distanceTo(px, pz);

            String typeColor = (type == DepositType.NEOFERRITE) ? "§b" : "§d";
            String typeName = type.getSpanishName();
            String discColor = discovered ? "§a" : "§7";
            String discText = discovered ? "Sí" : "No";
            String resColor = (remaining > 0) ? "§a" : "§c";

            Component line = Component.literal(String.format(
                    "§e[%s] %s%s§r §7| Centro: §f[%d, %d] §7(§f%.0fm§7) | Rad: §f%d §7| Pur: §6%d/5 §7| Res: %s%,d§7/§f%,d §7| Descubierta: %s%s",
                    vein.getId(),
                    typeColor,
                    typeName,
                    vein.getCenterX(),
                    vein.getCenterZ(),
                    dist,
                    vein.getRadius(),
                    vein.getPurity(),
                    resColor,
                    remaining,
                    vein.getInitialReserve(),
                    discColor,
                    discText
            ));

            source.sendSuccess(() -> line, false);
        }

        return nearby.size();
    }

    private static int setReserve(CommandSourceStack source, String veinId, int amount) {
        ServerLevel level = source.getLevel();
        long seed = level.getSeed();
        DepositVein vein = VeinGenerator.getVeinById(seed, veinId);
        if (vein == null) {
            source.sendFailure(Component.literal("§c[LogTech] No se encontró ninguna veta con ID '" + veinId + "' en esta semilla."));
            return 0;
        }

        DepositSavedData savedData = DepositSavedData.get(level);
        savedData.setRemainingReserve(vein, amount);
        source.sendSuccess(() -> Component.literal(String.format("§a[LogTech] Reserva de %s actualizada a %,d unidades.", veinId, amount)), true);
        return 1;
    }

    private static int setDiscovered(CommandSourceStack source, String veinId, boolean discovered) {
        ServerLevel level = source.getLevel();
        long seed = level.getSeed();
        DepositVein vein = VeinGenerator.getVeinById(seed, veinId);
        if (vein == null) {
            source.sendFailure(Component.literal("§c[LogTech] No se encontró ninguna veta con ID '" + veinId + "' en esta semilla."));
            return 0;
        }

        DepositSavedData savedData = DepositSavedData.get(level);
        savedData.setDiscovered(vein, discovered);
        source.sendSuccess(() -> Component.literal(String.format("§a[LogTech] Estado descubierto de %s actualizado a %s.", veinId, discovered ? "Sí" : "No")), true);
        return 1;
    }
}
