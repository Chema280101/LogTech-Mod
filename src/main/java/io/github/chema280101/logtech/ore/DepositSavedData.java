package io.github.chema280101.logtech.ore;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.Map;

/**
 * Almacena el estado persistente mutable de las vetas en el mundo.
 * Solo almacena las vetas cuya reserva ha cambiado o que han sido descubiertas,
 * manteniendo el almacenamiento ligero y sin dependencias de la generación previa.
 */
public class DepositSavedData extends SavedData {
    public static final String DATA_NAME = "logtech_deposits";

    public static class VeinState {
        private int remainingReserve;
        private boolean discovered;

        public VeinState(int remainingReserve, boolean discovered) {
            this.remainingReserve = remainingReserve;
            this.discovered = discovered;
        }

        public int getRemainingReserve() {
            return remainingReserve;
        }

        public void setRemainingReserve(int remainingReserve) {
            this.remainingReserve = remainingReserve;
        }

        public boolean isDiscovered() {
            return discovered;
        }

        public void setDiscovered(boolean discovered) {
            this.discovered = discovered;
        }
    }

    private final Map<String, VeinState> states = new HashMap<>();

    public DepositSavedData() {
    }

    public static final SavedData.Factory<DepositSavedData> FACTORY = new SavedData.Factory<>(
            DepositSavedData::new,
            DepositSavedData::load
    );

    /**
     * Obtiene o crea la instancia de datos guardados asociada al Overworld.
     */
    public static DepositSavedData get(ServerLevel level) {
        ServerLevel overworld = level.getServer().getLevel(Level.OVERWORLD);
        if (overworld == null) {
            overworld = level;
        }
        return overworld.getDataStorage().computeIfAbsent(FACTORY, DATA_NAME);
    }

    public int getRemainingReserve(DepositVein vein) {
        VeinState state = states.get(vein.getId());
        return state != null ? state.getRemainingReserve() : vein.getInitialReserve();
    }

    public boolean isDiscovered(DepositVein vein) {
        VeinState state = states.get(vein.getId());
        return state != null && state.isDiscovered();
    }

    public void setRemainingReserve(DepositVein vein, int remaining) {
        VeinState state = states.computeIfAbsent(vein.getId(), k -> new VeinState(vein.getInitialReserve(), false));
        state.setRemainingReserve(Math.max(0, remaining));
        setDirty();
    }

    public void setDiscovered(DepositVein vein, boolean discovered) {
        VeinState state = states.computeIfAbsent(vein.getId(), k -> new VeinState(vein.getInitialReserve(), false));
        state.setDiscovered(discovered);
        setDirty();
    }

    public int modifyReserve(DepositVein vein, int delta) {
        int current = getRemainingReserve(vein);
        int updated = Math.max(0, current + delta);
        setRemainingReserve(vein, updated);
        return updated;
    }

    /**
     * Devuelve la lista de todas las vetas que han sido descubiertas hasta el momento.
     */
    public java.util.List<VeinInfo> getAllDiscoveredVeinInfos(long worldSeed) {
        java.util.List<VeinInfo> result = new java.util.ArrayList<>();
        for (Map.Entry<String, VeinState> entry : states.entrySet()) {
            if (entry.getValue().isDiscovered()) {
                DepositVein vein = VeinGenerator.getVeinById(worldSeed, entry.getKey());
                if (vein != null) {
                    result.add(VeinInfo.from(vein, this));
                }
            }
        }
        return result;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        for (Map.Entry<String, VeinState> entry : states.entrySet()) {
            CompoundTag veinTag = new CompoundTag();
            veinTag.putString("id", entry.getKey());
            veinTag.putInt("remaining", entry.getValue().getRemainingReserve());
            veinTag.putBoolean("discovered", entry.getValue().isDiscovered());
            list.add(veinTag);
        }
        tag.put("VeinStates", list);
        return tag;
    }

    public static DepositSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        DepositSavedData data = new DepositSavedData();
        if (tag.contains("VeinStates", Tag.TAG_LIST)) {
            ListTag list = tag.getList("VeinStates", Tag.TAG_COMPOUND);
            for (int i = 0; i < list.size(); i++) {
                CompoundTag veinTag = list.getCompound(i);
                String id = veinTag.getString("id");
                int remaining = veinTag.getInt("remaining");
                boolean discovered = veinTag.getBoolean("discovered");
                data.states.put(id, new VeinState(remaining, discovered));
            }
        }
        return data;
    }
}
