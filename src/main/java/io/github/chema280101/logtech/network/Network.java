package io.github.chema280101.logtech.network;

import java.util.Collections;
import java.util.Set;
import net.minecraft.core.BlockPos;

public class Network {
    public static final Network EMPTY = new Network(0, 0, 0, 0, false, Collections.emptySet(), Collections.emptySet(), Collections.emptySet(), Collections.emptySet());

    private final int blockCount;
    private final int coreCount;
    private final int rackCount;
    private final int machineCount;
    private final boolean valid;
    private final Set<BlockPos> blocks;
    private final Set<BlockPos> cores;
    private final Set<BlockPos> racks;
    private final Set<BlockPos> machines;

    public Network(int blockCount, int coreCount, boolean valid, Set<BlockPos> blocks, Set<BlockPos> cores) {
        this(blockCount, coreCount, 0, 0, valid, blocks, cores, Collections.emptySet(), Collections.emptySet());
    }

    public Network(int blockCount, int coreCount, int rackCount, boolean valid, Set<BlockPos> blocks, Set<BlockPos> cores, Set<BlockPos> racks) {
        this(blockCount, coreCount, rackCount, 0, valid, blocks, cores, racks, Collections.emptySet());
    }

    public Network(int blockCount, int coreCount, int rackCount, int machineCount, boolean valid,
                   Set<BlockPos> blocks, Set<BlockPos> cores, Set<BlockPos> racks, Set<BlockPos> machines) {
        this.blockCount = blockCount;
        this.coreCount = coreCount;
        this.rackCount = rackCount;
        this.machineCount = machineCount;
        this.valid = valid;
        this.blocks = blocks != null ? Collections.unmodifiableSet(blocks) : Collections.emptySet();
        this.cores = cores != null ? Collections.unmodifiableSet(cores) : Collections.emptySet();
        this.racks = racks != null ? Collections.unmodifiableSet(racks) : Collections.emptySet();
        this.machines = machines != null ? Collections.unmodifiableSet(machines) : Collections.emptySet();
    }

    public int getBlockCount() {
        return blockCount;
    }

    public int getCoreCount() {
        return coreCount;
    }

    public int getRackCount() {
        return rackCount;
    }

    public int getMachineCount() {
        return machineCount;
    }

    public boolean isValid() {
        return valid;
    }

    public Set<BlockPos> getBlocks() {
        return blocks;
    }

    public Set<BlockPos> getCores() {
        return cores;
    }

    public Set<BlockPos> getRacks() {
        return racks;
    }

    public Set<BlockPos> getMachines() {
        return machines;
    }

    public Set<BlockPos> getMembers() {
        return machines;
    }
}
