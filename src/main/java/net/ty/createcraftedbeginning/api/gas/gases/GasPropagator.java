package net.ty.createcraftedbeginning.api.gas.gases;

import com.simibubi.create.AllTags.AllBlockTags;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.utility.BlockHelper;
import net.createmod.catnip.data.Iterate;
import net.createmod.catnip.data.Pair;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.ty.createcraftedbeginning.api.gas.gases.GasPipeConnection.AirFlow;
import net.ty.createcraftedbeginning.api.gas.gases.behaviours.GasTransportBehaviour;
import net.ty.createcraftedbeginning.config.CCBConfig;
import net.ty.createcraftedbeginning.content.airtights.airtightpump.AirtightPumpBlock;
import net.ty.createcraftedbeginning.content.airtights.airtightpump.AirtightPumpBlockEntity;
import net.ty.createcraftedbeginning.registry.CCBBlocks;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class GasPropagator {
    private GasPropagator() {
    }

    public static void propagatePipe(Level level, BlockPos pipePos, BlockState pipeState) {
        Deque<Pair<Integer, BlockPos>> frontier = new ArrayDeque<>();
        Set<BlockPos> visited = new HashSet<>();
        Set<Pair<AirtightPumpBlockEntity, Direction>> discoveredPumps = new HashSet<>();

        frontier.add(Pair.of(0, pipePos));
        visited.add(pipePos);
        int pumpRange = getAirtightPumpMaxRange();
        while (!frontier.isEmpty()) {
            Pair<Integer, BlockPos> pair = frontier.poll();
            int distance = pair.getFirst();
            BlockPos currentPos = pair.getSecond();
            GasTransportBehaviour behaviour = getBehaviour(level, currentPos);
            if (behaviour == null) {
                continue;
            }

            behaviour.wipePressure();
            BlockState currentState = currentPos.equals(pipePos) ? pipeState : level.getBlockState(currentPos);
            for (Direction direction : getPipeConnections(currentState, behaviour)) {
                BlockPos targetPos = currentPos.relative(direction);
                if (!level.isLoaded(targetPos) || visited.contains(targetPos)) {
                    continue;
                }

                visited.add(targetPos);
                BlockState targetState = level.getBlockState(targetPos);
                if (targetState.is(CCBBlocks.AIRTIGHT_PUMP_BLOCK)) {
                    Direction facing = targetState.getValue(AirtightPumpBlock.FACING);
                    if (facing.getAxis() == direction.getAxis() && level.getBlockEntity(targetPos) instanceof AirtightPumpBlockEntity pump) {
                        discoveredPumps.add(Pair.of(pump, direction.getOpposite()));
                    }
                    continue;
                }

                GasTransportBehaviour targetBehaviour = getBehaviour(level, targetPos);
                if (targetBehaviour == null) {
                    continue;
                }

                int newDistance = distance + 1;
                if (newDistance > pumpRange && !targetBehaviour.hasAnyPressure()) {
                    continue;
                }
                if (!targetBehaviour.canHaveFlowToward(targetState, direction.getOpposite())) {
                    continue;
                }

                frontier.add(Pair.of(newDistance, targetPos));
            }
        }

        discoveredPumps.forEach(p -> p.getFirst().updatePipesOnSide(p.getSecond()));
    }

    @Nullable
    public static GasTransportBehaviour getBehaviour(BlockGetter level, BlockPos pos) {
        return BlockEntityBehaviour.get(level, pos, GasTransportBehaviour.TYPE);
    }

    public static List<Direction> getPipeConnections(BlockState state, GasTransportBehaviour behaviour) {
        List<Direction> list = new ArrayList<>();
        for (Direction direction : Iterate.directions) {
            if (!behaviour.canHaveFlowToward(state, direction)) {
                continue;
            }

            list.add(direction);
        }
        return list;
    }

    public static int getAirtightPumpMaxRange() {
        return CCBConfig.server().airtights.maxPumpRange.get();
    }

    public static void resetAffectedNetworks(Level level, BlockPos start, Direction side) {
        Deque<BlockPos> frontier = new ArrayDeque<>();
        Set<BlockPos> visited = new HashSet<>();
        frontier.add(start);
        visited.add(start);
        while (!frontier.isEmpty()) {
            BlockPos pos = frontier.poll();
            GasTransportBehaviour behaviour = getBehaviour(level, pos);
            if (behaviour == null) {
                continue;
            }

            if (pos.equals(start)) {
                resetNetworkInDirection(level, pos, behaviour, side, frontier, visited);
            }
            else {
                for (Direction direction : Iterate.directions) {
                    resetNetworkInDirection(level, pos, behaviour, direction, frontier, visited);
                }
            }
        }
    }

    public static @Nullable Direction getChangedNeighbourSide(Level level, BlockPos pos, BlockPos neighborPos) {
        if (level.isClientSide) {
            return null;
        }

        Block otherBlock = level.getBlockState(neighborPos).getBlock();
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

    public static boolean isOpenEnded(Level level, BlockPos pos, Direction side) {
        BlockPos targetPos = pos.relative(side);
        BlockState targetState = level.getBlockState(targetPos);
        GasTransportBehaviour behaviour = getBehaviour(level, targetPos);
        if (behaviour != null && behaviour.canHaveFlowToward(targetState, side.getOpposite())) {
            return false;
        }

        if (AirtightPumpBlock.isPump(targetState) && targetState.getValue(AirtightPumpBlock.FACING).getAxis() == side.getAxis()) {
            return false;
        }

        if (BlockHelper.hasBlockSolidSide(targetState, level, targetPos, side.getOpposite()) && !AllBlockTags.FAN_TRANSPARENT.matches(targetState)) {
            return false;
        }

        if (GasCapabilities.hasGasCapability(level, targetPos, side.getOpposite())) {
            return false;
        }
        return targetState.canBeReplaced() && targetState.getDestroySpeed(level, targetPos) != -1;
    }

    private static void resetNetworkInDirection(Level level, BlockPos pos, GasTransportBehaviour behaviour, Direction direction, Deque<BlockPos> frontier, Set<BlockPos> visited) {
        BlockPos targetPos = pos.relative(direction);
        if (!level.isLoaded(targetPos) || visited.contains(targetPos)) {
            return;
        }

        GasPipeConnection connection = behaviour.getConnection(direction);
        if (connection == null || connection.flow.isEmpty()) {
            return;
        }

        AirFlow flow = connection.flow.get();
        if (!flow.inbound) {
            return;
        }

        connection.resetNetwork();
        frontier.add(targetPos);
        visited.add(targetPos);
    }
}
