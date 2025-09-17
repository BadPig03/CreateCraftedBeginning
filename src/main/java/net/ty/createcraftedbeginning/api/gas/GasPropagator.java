package net.ty.createcraftedbeginning.api.gas;

import com.simibubi.create.AllTags.AllBlockTags;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.utility.BlockHelper;
import net.createmod.catnip.data.Iterate;
import net.createmod.catnip.data.Pair;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.ty.createcraftedbeginning.api.gas.interfaces.IGasHandler;
import net.ty.createcraftedbeginning.config.CCBConfig;
import net.ty.createcraftedbeginning.content.airtights.airtightpump.AirtightPumpBlock;
import net.ty.createcraftedbeginning.content.airtights.airtightpump.AirtightPumpBlockEntity;
import net.ty.createcraftedbeginning.registry.CCBBlocks;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@SuppressWarnings("unused")
public class GasPropagator {
    public static void propagateChangedPipe(LevelAccessor world, BlockPos pipePos, BlockState pipeState) {
        Deque<Pair<Integer, BlockPos>> frontier = new ArrayDeque<>();
        Set<BlockPos> visited = new HashSet<>();
        Set<Pair<AirtightPumpBlockEntity, Direction>> discoveredPumps = new HashSet<>();

        frontier.add(Pair.of(0, pipePos));
        visited.add(pipePos);
        int pumpRange = getAirtightPumpRange();

        while (!frontier.isEmpty()) {
            Pair<Integer, BlockPos> pair = frontier.poll();
            int currentDistance = pair.getFirst();
            BlockPos currentPos = pair.getSecond();

            BlockState currentState = currentPos.equals(pipePos) ? pipeState : world.getBlockState(currentPos);
            GasTransportBehaviour pipe = getPipe(world, currentPos);
            if (pipe == null) {
                continue;
            }
            pipe.wipePressure();

            for (Direction direction : getPipeConnections(currentState, pipe)) {
                BlockPos target = currentPos.relative(direction);
                if (world instanceof Level l && !l.isLoaded(target)) {
                    continue;
                }
                if (visited.contains(target)) {
                    continue;
                }

                visited.add(target);
                BlockState targetState = world.getBlockState(target);

                if (CCBBlocks.AIRTIGHT_PUMP_BLOCK.has(targetState)) {
                    Direction pumpFacing = targetState.getValue(AirtightPumpBlock.FACING);
                    if (pumpFacing.getAxis() == direction.getAxis()) {
                        BlockEntity be = world.getBlockEntity(target);
                        if (be instanceof AirtightPumpBlockEntity pump) {
                            discoveredPumps.add(Pair.of(pump, direction.getOpposite()));
                        }
                    }
                    continue;
                }

                GasTransportBehaviour targetPipe = getPipe(world, target);
                if (targetPipe == null) {
                    continue;
                }

                int newDistance = currentDistance + 1;
                if (newDistance > pumpRange && !targetPipe.hasAnyPressure()) {
                    continue;
                }

                if (targetPipe.canHaveFlowToward(targetState, direction.getOpposite())) {
                    frontier.add(Pair.of(newDistance, target));
                }
            }
        }

        discoveredPumps.forEach(p -> p.getFirst().updatePipesOnSide(p.getSecond()));
    }

    public static void resetAffectedNetworks(Level world, BlockPos start, Direction side) {
        Deque<BlockPos> frontier = new ArrayDeque<>();
        Set<BlockPos> visited = new HashSet<>();

        frontier.add(start);
        visited.add(start);

        while (!frontier.isEmpty()) {
            BlockPos pos = frontier.poll();
            GasTransportBehaviour pipe = getPipe(world, pos);
            if (pipe == null) {
                continue;
            }

            List<Direction> directionsToCheck = new ArrayList<>();
            if (pos.equals(start)) {
                directionsToCheck.add(side);
            } else {
                Collections.addAll(directionsToCheck, Iterate.directions);
            }

            for (Direction d : directionsToCheck) {
                BlockPos target = pos.relative(d);
                if (!world.isLoaded(target) || visited.contains(target)) {
                    continue;
                }

                GasPipeConnection connection = pipe.getConnection(d);
                if (connection == null) {
                    continue;
                }
                if (connection.flow.isEmpty()) {
                    continue;
                }

                GasPipeConnection.AirFlow flow = connection.flow.get();
                if (!flow.inbound) {
                    continue;
                }

                connection.resetNetwork();

                frontier.add(target);
                visited.add(target);
            }
        }
    }

    public static @Nullable Direction validateNeighbourChange(BlockState state, @NotNull Level world, BlockPos pos, BlockPos neighborPos, boolean isMoving) {
        if (world.isClientSide) {
            return null;
        }

        Block otherBlock = world.getBlockState(neighborPos).getBlock();
        if (otherBlock instanceof AirtightPumpBlock) {
            return null;
        }

        for (Direction direction : Iterate.directions) {
            if (!pos.relative(direction).equals(neighborPos)) {
                continue;
            }
            return direction;
        }
        return null;
    }

    public static GasTransportBehaviour getPipe(BlockGetter reader, BlockPos pos) {
        return BlockEntityBehaviour.get(reader, pos, GasTransportBehaviour.TYPE);
    }

    public static boolean isOpenEnd(@NotNull BlockGetter reader, @NotNull BlockPos pos, Direction side) {
        BlockPos connectedPos = pos.relative(side);
        BlockState connectedState = reader.getBlockState(connectedPos);
        GasTransportBehaviour pipe = GasPropagator.getPipe(reader, connectedPos);

        if (pipe != null && pipe.canHaveFlowToward(connectedState, side.getOpposite())) {
            return false;
        }
        if (AirtightPumpBlock.isPump(connectedState) && connectedState.getValue(AirtightPumpBlock.FACING).getAxis() == side.getAxis()) {
            return false;
        }
        if (BlockHelper.hasBlockSolidSide(connectedState, reader, connectedPos, side.getOpposite()) && !AllBlockTags.FAN_TRANSPARENT.matches(connectedState)) {
            return false;
        }
        if (hasGasCapability(reader, connectedPos, side.getOpposite())) {
            return false;
        }
        return connectedState.isAir();
    }

    public static @NotNull List<Direction> getPipeConnections(BlockState state, GasTransportBehaviour pipe) {
        List<Direction> list = new ArrayList<>();
        for (Direction d : Iterate.directions) {
            if (pipe.canHaveFlowToward(state, d)) {
                list.add(d);
            }
        }
        return list;
    }

    public static int getAirtightPumpRange() {
        return CCBConfig.server().compressedAir.airtightPumpRange.get();
    }

    public static boolean hasGasCapability(@NotNull BlockGetter world, BlockPos pos, Direction side) {
        BlockEntity blockEntity = world.getBlockEntity(pos);
        if (blockEntity == null || blockEntity.getLevel() == null) {
            return false;
        }

        IGasHandler capability = blockEntity.getLevel().getCapability(GasCapabilities.GasHandler.BLOCK, blockEntity.getBlockPos(), side);
        return capability != null;
    }
}
