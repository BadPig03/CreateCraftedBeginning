package net.ty.createcraftedbeginning.content.airtights.airtightpump;

import com.simibubi.create.content.kinetics.base.IRotate;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.createmod.catnip.data.Couple;
import net.createmod.catnip.data.Iterate;
import net.createmod.catnip.data.Pair;
import net.createmod.catnip.math.BlockFace;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.ty.createcraftedbeginning.api.gas.GasCapabilities;
import net.ty.createcraftedbeginning.api.gas.GasStack;
import net.ty.createcraftedbeginning.api.gas.interfaces.IGasHandler;
import net.ty.createcraftedbeginning.api.gas.GasPipeConnection;
import net.ty.createcraftedbeginning.api.gas.GasPropagator;
import net.ty.createcraftedbeginning.api.gas.GasTransportBehaviour;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class AirtightPumpBlockEntity extends KineticBlockEntity {
    Couple<MutableBoolean> sidesToUpdate;
    boolean pressureUpdate;
    private GasPumpTransportBehaviour transportBehaviour;

    public AirtightPumpBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        sidesToUpdate = Couple.create(MutableBoolean::new);
        setLazyTickRate(10);
    }

    @Override
    public void tick() {
        super.tick();

        if (level == null || (level.isClientSide && !isVirtual())) {
            return;
        }

        if (pressureUpdate) {
            pressureUpdate = false;
            updatePressureChange();
        }

        sidesToUpdate.forEachWithContext((update, isFront) -> {
            if (update.isFalse()) {
                return;
            }
            update.setFalse();
            Direction front = getFront();
            if (front == null) {
                return;
            }
            distributePressureTo(isFront ? front : front.getOpposite());
        });
    }

    @Override
    public void onSpeedChanged(float previousSpeed) {
        super.onSpeedChanged(previousSpeed);

        if (Mth.abs(previousSpeed - getSpeed()) == 0f) {
            return;
        }
        if (level == null || (level.isClientSide && !isVirtual())) {
            return;
        }

        updatePressureChange();
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        transportBehaviour = new GasPumpTransportBehaviour(this);
        behaviours.add(transportBehaviour);
        super.addBehaviours(behaviours);
    }

    @Override
    public void lazyTick() {
        boolean foundGas = false;
        for (Map.Entry<Direction, GasPipeConnection> entry : transportBehaviour.interfaces.entrySet()) {
            GasStack gasStack = transportBehaviour.getProvidedOutwardGas(entry.getKey());
            if (!gasStack.isEmpty()) {
                foundGas = true;
            }
        }

        if (!foundGas) {
            updatePressureChange();
        }
        else {
            Direction front = getFront();
            if (front == null) {
                return;
            }

            GasTransportBehaviour pipe = GasPropagator.getPipe(level, worldPosition.relative(front));
            if (pipe == null || pipe.hasAnyPressure()) {
                return;
            }

            updatePressureChange();
        }
    }

    public boolean isSideAccessible(Direction direction) {
        BlockState blockState = getBlockState();
        if (!(blockState.getBlock() instanceof AirtightPumpBlock)) {
            return false;
        }
        return blockState.getValue(AirtightPumpBlock.FACING).getAxis() == direction.getAxis();
    }

    protected void distributePressureTo(Direction side) {
        if (getSpeed() == 0 || level == null) {
            return;
        }

        BlockFace start = new BlockFace(worldPosition, side);
        boolean pull = isPullingOnSide(isFront(side));
        Set<BlockFace> targets = new HashSet<>();
        Map<BlockPos, Pair<Integer, Map<Direction, Boolean>>> pipeGraph = new HashMap<>();

        if (!pull) {
            GasPropagator.resetAffectedNetworks(level, worldPosition, side.getOpposite());
        }

        if (!hasReachedValidEndpoint(level, start, pull)) {
            pipeGraph.computeIfAbsent(worldPosition, $ -> Pair.of(0, new IdentityHashMap<>())).getSecond().put(side, pull);
            pipeGraph.computeIfAbsent(start.getConnectedPos(), $ -> Pair.of(1, new IdentityHashMap<>())).getSecond().put(side.getOpposite(), !pull);

            List<Pair<Integer, BlockPos>> frontier = new ArrayList<>();
            Set<BlockPos> visited = new HashSet<>();
            int maxDistance = GasPropagator.getAirtightPumpRange();
            frontier.add(Pair.of(1, start.getConnectedPos()));

            while (!frontier.isEmpty()) {
                Pair<Integer, BlockPos> entry = frontier.removeFirst();
                int distance = entry.getFirst();
                BlockPos currentPos = entry.getSecond();

                if (!level.isLoaded(currentPos)) {
                    continue;
                }
                if (visited.contains(currentPos)) {
                    continue;
                }
                visited.add(currentPos);
                BlockState currentState = level.getBlockState(currentPos);
                GasTransportBehaviour pipe = GasPropagator.getPipe(level, currentPos);
                if (pipe == null) {
                    continue;
                }

                for (Direction face : GasPropagator.getPipeConnections(currentState, pipe)) {
                    BlockFace blockFace = new BlockFace(currentPos, face);
                    BlockPos connectedPos = blockFace.getConnectedPos();

                    if (!level.isLoaded(connectedPos)) {
                        continue;
                    }
                    if (blockFace.isEquivalent(start)) {
                        continue;
                    }
                    if (hasReachedValidEndpoint(level, blockFace, pull)) {
                        pipeGraph.computeIfAbsent(currentPos, $ -> Pair.of(distance, new IdentityHashMap<>())).getSecond().put(face, pull);
                        targets.add(blockFace);
                        continue;
                    }

                    GasTransportBehaviour pipeBehaviour = GasPropagator.getPipe(level, connectedPos);
                    if (pipeBehaviour == null) {
                        continue;
                    }
                    if (pipeBehaviour instanceof GasPumpTransportBehaviour) {
                        continue;
                    }
                    if (visited.contains(connectedPos)) {
                        continue;
                    }
                    if (distance + 1 >= maxDistance) {
                        pipeGraph.computeIfAbsent(currentPos, $ -> Pair.of(distance, new IdentityHashMap<>())).getSecond().put(face, pull);
                        targets.add(blockFace);
                        continue;
                    }

                    pipeGraph.computeIfAbsent(currentPos, $ -> Pair.of(distance, new IdentityHashMap<>())).getSecond().put(face, pull);
                    pipeGraph.computeIfAbsent(connectedPos, $ -> Pair.of(distance + 1, new IdentityHashMap<>())).getSecond().put(face.getOpposite(), !pull);
                    frontier.add(Pair.of(distance + 1, connectedPos));
                }
            }
        }

        Map<Integer, Set<BlockFace>> validFaces = new HashMap<>();
        searchForEndpointRecursively(pipeGraph, targets, validFaces, new BlockFace(start.getPos(), start.getOppositeFace()), pull);

        float pressure = Math.abs(getSpeed());
        for (Set<BlockFace> set : validFaces.values()) {
            int parallelBranches = Math.max(1, set.size() - 1);
            for (BlockFace face : set) {
                BlockPos pipePos = face.getPos();
                Direction pipeSide = face.getFace();

                if (pipePos.equals(worldPosition)) {
                    continue;
                }

                boolean inbound = pipeGraph.get(pipePos).getSecond().get(pipeSide);
                GasTransportBehaviour pipeBehaviour = GasPropagator.getPipe(level, pipePos);
                if (pipeBehaviour == null) {
                    continue;
                }

                pipeBehaviour.addPressure(pipeSide, inbound, pressure / parallelBranches);
            }
        }
    }

    protected boolean searchForEndpointRecursively(Map<BlockPos, Pair<Integer, Map<Direction, Boolean>>> pipeGraph, Set<BlockFace> targets, Map<Integer, Set<BlockFace>> validFaces, BlockFace currentFace, boolean pull) {
        BlockPos currentPos = currentFace.getPos();
        if (!pipeGraph.containsKey(currentPos)) {
            return false;
        }
        Pair<Integer, Map<Direction, Boolean>> pair = pipeGraph.get(currentPos);
        int distance = pair.getFirst();

        boolean atLeastOneBranchSuccessful = false;
        for (Direction nextFacing : Iterate.directions) {
            if (nextFacing == currentFace.getFace()) {
                continue;
            }
            Map<Direction, Boolean> map = pair.getSecond();
            if (!map.containsKey(nextFacing)) {
                continue;
            }

            BlockFace localTarget = new BlockFace(currentPos, nextFacing);
            if (targets.contains(localTarget)) {
                validFaces.computeIfAbsent(distance, $ -> new HashSet<>()).add(localTarget);
                atLeastOneBranchSuccessful = true;
                continue;
            }

            if (map.get(nextFacing) != pull) {
                continue;
            }
            if (!searchForEndpointRecursively(pipeGraph, targets, validFaces, new BlockFace(currentPos.relative(nextFacing), nextFacing.getOpposite()), pull)) {
                continue;
            }

            validFaces.computeIfAbsent(distance, $ -> new HashSet<>()).add(localTarget);
            atLeastOneBranchSuccessful = true;
        }

        if (atLeastOneBranchSuccessful) {
            validFaces.computeIfAbsent(distance, $ -> new HashSet<>()).add(currentFace);
        }

        return atLeastOneBranchSuccessful;
    }

    private boolean hasReachedValidEndpoint(LevelAccessor world, BlockFace blockFace, boolean pull) {
        BlockPos connectedPos = blockFace.getConnectedPos();
        BlockState connectedState = world.getBlockState(connectedPos);
        BlockEntity blockEntity = world.getBlockEntity(connectedPos);
        Direction face = blockFace.getFace();

        if (AirtightPumpBlock.isPump(connectedState) && connectedState.getValue(AirtightPumpBlock.FACING).getAxis() == face.getAxis() && blockEntity instanceof AirtightPumpBlockEntity pumpBE) {
            return pumpBE.isPullingOnSide(pumpBE.isFront(blockFace.getOppositeFace())) != pull;
        }

        GasTransportBehaviour pipe = GasPropagator.getPipe(world, connectedPos);
        if (pipe != null && pipe.canHaveFlowToward(connectedState, blockFace.getOppositeFace())) {
            return false;
        }

        if (blockEntity != null && blockEntity.getLevel() != null) {
            IGasHandler capability = blockEntity.getLevel().getCapability(GasCapabilities.GasHandler.BLOCK, blockEntity.getBlockPos(), face.getOpposite());
            if (capability != null) {
                return true;
            }
        }

        return GasPropagator.isOpenEnd(world, blockFace.getPos(), face);
    }

    protected boolean isFront(Direction direction) {
        BlockState blockState = getBlockState();
        if (!(blockState.getBlock() instanceof AirtightPumpBlock)) {
            return false;
        }
        Direction front = blockState.getValue(AirtightPumpBlock.FACING);
        return direction == front;
    }

    @Nullable
    protected Direction getFront() {
        BlockState blockState = getBlockState();
        if (!(blockState.getBlock() instanceof AirtightPumpBlock)) {
            return null;
        }
        return blockState.getValue(AirtightPumpBlock.FACING);
    }

    public void updatePipesOnSide(Direction direction) {
        if (!isSideAccessible(direction)) {
            return;
        }
        updatePipeNetwork(isFront(direction));
        transportBehaviour.wipePressure();
    }

    protected void updatePipeNetwork(boolean front) {
        sidesToUpdate.get(front).setTrue();
    }

    private void updatePressureChange() {
        Direction front = getFront();
        if (front == null || level == null) {
            return;
        }

        BlockPos frontPos = worldPosition.relative(front);
        BlockPos backPos = worldPosition.relative(front.getOpposite());
        GasPropagator.propagateChangedPipe(level, frontPos, level.getBlockState(frontPos));
        GasPropagator.propagateChangedPipe(level, backPos, level.getBlockState(backPos));

        if (transportBehaviour != null) {
            transportBehaviour.wipePressure();
        }

        sidesToUpdate.forEach(MutableBoolean::setTrue);
        pressureUpdate = false;
    }

    public boolean isPullingOnSide(boolean front) {
        return !front;
    }

    class GasPumpTransportBehaviour extends GasTransportBehaviour {
        public GasPumpTransportBehaviour(AirtightPumpBlockEntity be) {
            super(be);
        }

        @Override
        public boolean canHaveFlowToward(BlockState state, Direction direction) {
            return isSideAccessible(direction);
        }

        @Override
        public AttachmentTypes getRenderedRimAttachment(BlockAndTintGetter world, BlockPos pos, BlockState state, Direction direction) {
            return AttachmentTypes.NONE;
        }

        @Override
        public void tick() {
            super.tick();

            Level level = getWorld();
            if (level == null || level.isClientSide() || !level.isLoaded(getPos()) || isRemoved()) {
                return;
            }

            float absSpeed = Mth.abs(getSpeed());
            if (absSpeed < IRotate.SpeedLevel.MEDIUM.getSpeedValue()) {
                return;
            }

            for (Map.Entry<Direction, GasPipeConnection> entry : interfaces.entrySet()) {
                boolean pull = isPullingOnSide(isFront(entry.getKey()));
                Couple<Float> pressure = entry.getValue().getPressure();
                pressure.set(pull, absSpeed);
                pressure.set(!pull, 0f);
            }
        }
    }
}