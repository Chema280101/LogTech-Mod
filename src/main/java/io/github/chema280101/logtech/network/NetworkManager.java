package io.github.chema280101.logtech.network;

import io.github.chema280101.logtech.block.CoreBlockEntity;
import io.github.chema280101.logtech.registry.ModBlocks;
import io.github.chema280101.logtech.registry.ModTags;
import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class NetworkManager {
    public static final int MAX_NETWORK_BLOCKS = 512;

    private static final Map<ResourceKey<Level>, NetworkManager> MANAGERS = new ConcurrentHashMap<>();

    private final Level level;

    public NetworkManager(Level level) {
        this.level = level;
    }

    public static NetworkManager get(Level level) {
        if (level == null || level.isClientSide()) {
            return null;
        }
        return MANAGERS.computeIfAbsent(level.dimension(), dim -> new NetworkManager(level));
    }

    public static void clear(Level level) {
        if (level != null && !level.isClientSide()) {
            MANAGERS.remove(level.dimension());
        }
    }

    /**
     * Escanea la red conectada a partir de startPos usando BFS a traves de las caras vecinas.
     * Nunca carga chunks que no esten cargados.
     *
     * @param startPos posicion inicial
     * @param ignoredPos posicion a ignorar (por ejemplo el bloque que esta siendo destruido)
     * @return objeto Network con los resultados del escaneo
     */
    public Network scan(BlockPos startPos, BlockPos ignoredPos) {
        if (!level.hasChunkAt(startPos) || startPos.equals(ignoredPos)) {
            return Network.EMPTY;
        }

        BlockState startState = level.getBlockState(startPos);
        if (!startState.is(ModTags.Blocks.NETWORK_COMPONENT)) {
            return Network.EMPTY;
        }

        Set<BlockPos> visited = new HashSet<>();
        Set<BlockPos> cores = new HashSet<>();
        Set<BlockPos> racks = new HashSet<>();
        Set<BlockPos> machines = new HashSet<>();
        Queue<BlockPos> queue = new ArrayDeque<>();

        BlockPos initialPos = startPos.immutable();
        visited.add(initialPos);

        if (isMachine(initialPos, startState)) {
            machines.add(initialPos);
            // Las maquinas no conducen la red: no se agregan a la cola de exploracion
        } else {
            queue.add(initialPos);
            if (startState.is(ModBlocks.CORE.get())) {
                cores.add(initialPos);
            } else if (startState.is(ModBlocks.RACK.get()) || startState.is(ModBlocks.COLD_RACK.get())) {
                racks.add(initialPos);
            }
        }

        boolean limitExceeded = false;

        while (!queue.isEmpty()) {
            BlockPos current = queue.poll();

            for (Direction dir : Direction.values()) {
                BlockPos neighborPos = current.relative(dir);

                if (neighborPos.equals(ignoredPos) || visited.contains(neighborPos)) {
                    continue;
                }

                // Regla critica: comprobar siempre que el chunk este cargado antes de consultar el bloque
                if (!level.hasChunkAt(neighborPos)) {
                    continue;
                }

                BlockState neighborState = level.getBlockState(neighborPos);
                if (neighborState.is(ModTags.Blocks.NETWORK_COMPONENT)) {
                    if (visited.size() >= MAX_NETWORK_BLOCKS) {
                        limitExceeded = true;
                        break;
                    }

                    BlockPos immutableNeighbor = neighborPos.immutable();
                    visited.add(immutableNeighbor);

                    if (isMachine(immutableNeighbor, neighborState)) {
                        machines.add(immutableNeighbor);
                        // HOJA: Las maquinas se conectan pero NO conducen la red.
                        // No se agregan a la cola, impidiendo que el recorrido pase a traves de ellas.
                    } else {
                        queue.add(immutableNeighbor);
                        if (neighborState.is(ModBlocks.CORE.get())) {
                            cores.add(immutableNeighbor);
                        } else if (neighborState.is(ModBlocks.RACK.get()) || neighborState.is(ModBlocks.COLD_RACK.get())) {
                            racks.add(immutableNeighbor);
                        }
                    }
                }
            }

            if (limitExceeded) {
                break;
            }
        }

        boolean valid = !limitExceeded && cores.size() == 1;
        return new Network(visited.size(), cores.size(), racks.size(), machines.size(), valid, visited, cores, racks, machines);
    }

    /**
     * Determina si el bloque en la posicion indicada es una maquina miembro de la red (hoja no conductora).
     */
    public boolean isMachine(BlockPos pos, BlockState state) {
        if (state.is(ModBlocks.CRUSHER.get()) || state.is(ModBlocks.DRILL.get()) || state.is(ModBlocks.ALARM.get()) || state.is(ModBlocks.COAL_GENERATOR.get())) {
            return true;
        }
        if (level != null && level.hasChunkAt(pos)) {
            BlockEntity be = level.getBlockEntity(pos);
            return be instanceof NetworkMember;
        }
        return false;
    }

    /**
     * Notifica que un componente de red fue anadido (colocado) en el nivel.
     */
    public void onComponentAdded(BlockPos pos) {
        if (level.isClientSide() || !level.hasChunkAt(pos)) {
            return;
        }
        BlockState state = level.getBlockState(pos);
        if (isMachine(pos, state)) {
            Set<BlockPos> alreadyVisitedAcrossBranches = new HashSet<>();
            for (Direction dir : Direction.values()) {
                BlockPos neighborPos = pos.relative(dir);
                if (alreadyVisitedAcrossBranches.contains(neighborPos) || !level.hasChunkAt(neighborPos)) {
                    continue;
                }
                if (!level.getBlockState(neighborPos).is(ModTags.Blocks.NETWORK_COMPONENT)) {
                    continue;
                }
                Network branchNetwork = scan(neighborPos, null);
                alreadyVisitedAcrossBranches.addAll(branchNetwork.getBlocks());
                updateCoresInNetwork(branchNetwork);
            }
            return;
        }
        Network network = scan(pos, null);
        updateCoresInNetwork(network);
    }

    /**
     * Notifica que un componente de red fue eliminado (roto) en el nivel.
     */
    public void onComponentRemoved(BlockPos pos) {
        if (level.isClientSide()) {
            return;
        }

        Set<BlockPos> alreadyVisitedAcrossBranches = new HashSet<>();

        for (Direction dir : Direction.values()) {
            BlockPos neighborPos = pos.relative(dir);
            if (alreadyVisitedAcrossBranches.contains(neighborPos)) {
                continue;
            }
            if (!level.hasChunkAt(neighborPos)) {
                continue;
            }
            if (!level.getBlockState(neighborPos).is(ModTags.Blocks.NETWORK_COMPONENT)) {
                continue;
            }

            Network branchNetwork = scan(neighborPos, pos);
            alreadyVisitedAcrossBranches.addAll(branchNetwork.getBlocks());
            updateCoresInNetwork(branchNetwork);
        }
    }

    /**
     * Actualiza el estado de red de todos los Nucleos encontrados en dicha red.
     */
    public void updateCoresInNetwork(Network network) {
        for (BlockPos corePos : network.getCores()) {
            if (level.hasChunkAt(corePos)) {
                BlockEntity be = level.getBlockEntity(corePos);
                if (be instanceof CoreBlockEntity coreBe) {
                    coreBe.setNetwork(network);
                }
            }
        }
    }
}
