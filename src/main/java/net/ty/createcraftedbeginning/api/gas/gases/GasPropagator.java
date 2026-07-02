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
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.ty.createcraftedbeginning.api.gas.gases.GasCapabilities.GasHandler;
import net.ty.createcraftedbeginning.api.gas.gases.GasPipeConnection.AirFlow;
import net.ty.createcraftedbeginning.api.gas.gases.behaviours.GasTransportBehaviour;
import net.ty.createcraftedbeginning.api.gas.gases.interfaces.IGasHandler;
import net.ty.createcraftedbeginning.config.CCBConfig;
import net.ty.createcraftedbeginning.content.airtights.airtightpump.AirtightPumpBlock;
import net.ty.createcraftedbeginning.content.airtights.airtightpump.AirtightPumpBlockEntity;
import net.ty.createcraftedbeginning.registry.CCBBlocks;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class GasPropagator {
    public static void propagateChangedPipe(LevelAccessor level, BlockPos pipePos, BlockState pipeState) {
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
            BlockState currentState = currentPos.equals(pipePos) ? pipeState : level.getBlockState(currentPos);
            GasTransportBehaviour pipe = getPipe(level, currentPos);
            if (pipe == null) {
                continue;
            }

            pipe.wipePressure();

            for (Direction direction : getPipeConnections(currentState, pipe)) {
                BlockPos target = currentPos.relative(direction);
                if (level instanceof Level l && !l.isLoaded(target)) {
                    continue;
                }
                if (visited.contains(target)) {
                    continue;
                }

                visited.add(target);
                BlockState targetState = level.getBlockState(target);

                if (CCBBlocks.AIRTIGHT_PUMP_BLOCK.has(targetState)) {
                    Direction pumpFacing = targetState.getValue(AirtightPumpBlock.FACING);
                    if (pumpFacing.getAxis() == direction.getAxis()) {
                        BlockEntity be = level.getBlockEntity(target);
                        if (be instanceof AirtightPumpBlockEntity pump) {
                            discoveredPumps.add(Pair.of(pump, direction.getOpposite()));
                        }
                    }
                    continue;
                }

                GasTransportBehaviour targetPipe = getPipe(level, target);
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

    @Nullable
    public static GasTransportBehaviour getPipe(BlockGetter level, BlockPos pos) {
        return BlockEntityBehaviour.get(level, pos, GasTransportBehaviour.TYPE);
    }

    public static List<Direction> getPipeConnections(BlockState state, GasTransportBehaviour pipe) {
        List<Direction> list = new ArrayList<>();
        for (Direction direction : Iterate.directions) {
            if (pipe.canHaveFlowToward(state, direction)) {
                list.add(direction);
            }
        }
        return list;
    }

    public static int getAirtightPumpRange() {
        return CCBConfig.server().airtights.maxPumpRange.get();
    }

    public static void resetAffectedNetworks(Level level, BlockPos start, Direction side) {
        Deque<BlockPos> frontier = new ArrayDeque<>();
        Set<BlockPos> visited = new HashSet<>();

        frontier.add(start);
        visited.add(start);

        while (!frontier.isEmpty()) {
            BlockPos pos = frontier.poll();
            GasTransportBehaviour pipe = getPipe(level, pos);
            if (pipe == null) {
                continue;
            }

            List<Direction> directionsToCheck = new ArrayList<>();
            if (pos.equals(start)) {
                directionsToCheck.add(side);
            }
            else {
                Collections.addAll(directionsToCheck, Iterate.directions);
            }

            for (Direction d : directionsToCheck) {
                BlockPos target = pos.relative(d);
                if (!level.isLoaded(target) || visited.contains(target)) {
                    continue;
                }

                GasPipeConnection connection = pipe.getConnection(d);
                if (connection == null) {
                    continue;
                }
                if (connection.flow.isEmpty()) {
                    continue;
                }

                AirFlow flow = connection.flow.get();
                if (!flow.inbound) {
                    continue;
                }

                connection.resetNetwork();

                frontier.add(target);
                visited.add(target);
            }
        }
    }

    public static @Nullable Direction validateNeighbourChange(BlockState state, Level level, BlockPos pos, BlockPos neighborPos, boolean isMoving) {
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

    public static boolean isOpenEnd(BlockGetter level, BlockPos pos, Direction side) {
        BlockPos connectedPos = pos.relative(side);
        BlockState connectedState = level.getBlockState(connectedPos);
        GasTransportBehaviour pipe = getPipe(level, connectedPos);
        if (pipe != null && pipe.canHaveFlowToward(connectedState, side.getOpposite())) {
            return false;
        }
        if (AirtightPumpBlock.isPump(connectedState) && connectedState.getValue(AirtightPumpBlock.FACING).getAxis() == side.getAxis()) {
            return false;
        }
        if (BlockHelper.hasBlockSolidSide(connectedState, level, connectedPos, side.getOpposite()) && !AllBlockTags.FAN_TRANSPARENT.matches(connectedState)) {
            return false;
        }
        if (hasGasCapability(level, connectedPos, side.getOpposite())) {
            return false;
        }
        return connectedState.canBeReplaced() && connectedState.getDestroySpeed(level, connectedPos) != -1 || connectedState.hasProperty(BlockStateProperties.WATERLOGGED);
    }

    public static boolean hasGasCapability(BlockGetter level, BlockPos pos, Direction side) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity == null || blockEntity.getLevel() == null) {
            return false;
        }

        IGasHandler capability = blockEntity.getLevel().getCapability(GasHandler.BLOCK, blockEntity.getBlockPos(), side);
        return capability != null;
    }
}
