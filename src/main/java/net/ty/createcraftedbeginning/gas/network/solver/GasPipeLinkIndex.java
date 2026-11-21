package net.ty.createcraftedbeginning.gas.network.solver;

import net.createmod.catnip.data.Iterate;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.ty.createcraftedbeginning.gas.behaviour.GasTransportBehaviour;
import net.ty.createcraftedbeginning.gas.network.GasConnectionResolver;
import net.ty.createcraftedbeginning.gas.network.GasConnectionResolver.AdjacentConnection;
import net.ty.createcraftedbeginning.gas.network.solver.GasNetworkState.PipeLinks;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
final class GasPipeLinkIndex {
    private static final int ALL_FACES = (1 << Direction.values().length) - 1;

    private final Map<ChunkPos, Map<BlockPos, Links>> byChunk = new HashMap<>();

    @Nullable
    Links resolve(Level level, BlockPos pos, Map<ChunkPos, Boolean> chunks) {
        ChunkPos chunk = new ChunkPos(pos);
        if (!chunks.computeIfAbsent(chunk, key -> level.hasChunk(key.x, key.z))) {
            return null;
        }

        int loaded = ALL_FACES;
        int x = pos.getX() & 15;
        int z = pos.getZ() & 15;
        boolean boundary = x == 0 || x == 15 || z == 0 || z == 15;
        if (boundary) {
            for (Direction side : Iterate.horizontalDirections) {
                ChunkPos adjacent = new ChunkPos(pos.relative(side));
                if (chunks.computeIfAbsent(adjacent, key -> level.hasChunk(key.x, key.z))) {
                    continue;
                }

                loaded &= ~(1 << side.ordinal());
            }
        }
        Map<BlockPos, Links> members = byChunk.get(chunk);
        Links previous = members == null ? null : members.get(pos);
        if (previous != null && previous.loaded == loaded) {
            return previous;
        }

        GasTransportBehaviour behaviour = GasConnectionResolver.getTransportBehaviour(level, pos);
        if (behaviour == null) {
            return null;
        }

        if (boundary) {
            behaviour.refreshConnectionFaces();
        }

        int neighbors = 0;
        int endpoints = 0;
        int atmosphere = 0;
        for (Direction side : Iterate.directions) {
            int bit = 1 << side.ordinal();
            if ((loaded & bit) == 0 || behaviour.getConnection(side) == null) {
                continue;
            }

            AdjacentConnection adjacent = GasConnectionResolver.resolveAdjacentConnection(level, pos, side);
            if (adjacent.isAtmospheric()) {
                atmosphere |= bit;
                continue;
            }

            if (adjacent.hasGasHandler()) {
                endpoints |= bit;
                continue;
            }

            GasTransportBehaviour other = adjacent.behaviour();
            if (other == null) {
                continue;
            }

            if (boundary && !chunk.equals(new ChunkPos(adjacent.pos()))) {
                other.refreshConnectionFaces();
            }
            if (other.getConnection(side.getOpposite()) == null) {
                continue;
            }

            neighbors |= bit;
        }
        Links links = new Links(neighbors, endpoints, atmosphere, loaded, behaviour.isOrdinaryPressurePipe());
        byChunk.computeIfAbsent(chunk, ignored -> new HashMap<>()).put(pos.immutable(), links);
        return links;
    }

    void forget(BlockPos pos) {
        ChunkPos chunk = new ChunkPos(pos);
        Map<BlockPos, Links> members = byChunk.get(chunk);
        if (members == null) {
            return;
        }

        members.remove(pos);
        if (!members.isEmpty()) {
            return;
        }

        byChunk.remove(chunk);
    }

    Set<BlockPos> touching(ChunkPos chunk) {
        Set<BlockPos> positions = new HashSet<>();
        Map<BlockPos, Links> members = byChunk.get(chunk);
        if (members != null) {
            positions.addAll(members.keySet());
        }
        for (Direction direction : Iterate.horizontalDirections) {
            ChunkPos neighbor = new ChunkPos(chunk.x + direction.getStepX(), chunk.z + direction.getStepZ());
            Map<BlockPos, Links> border = byChunk.get(neighbor);
            if (border == null) {
                continue;
            }

            for (BlockPos pos : border.keySet()) {
                if (!new ChunkPos(pos.relative(direction.getOpposite())).equals(chunk)) {
                    continue;
                }

                positions.add(pos);
            }
        }
        return positions;
    }

    record Links(int neighbors, int endpoints, int atmosphere, int loaded, boolean ordinary) implements PipeLinks {}
}
