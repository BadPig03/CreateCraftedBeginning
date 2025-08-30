package net.ty.createcraftedbeginning.content.airtightpump;

import com.simibubi.create.content.fluids.FluidPropagator;
import com.simibubi.create.content.fluids.FluidTransportBehaviour;
import com.simibubi.create.content.fluids.PipeConnection;
import com.simibubi.create.content.fluids.pump.PumpBlock;
import com.simibubi.create.content.fluids.pump.PumpBlockEntity;
import com.simibubi.create.content.fluids.tank.FluidTankBlock;
import com.simibubi.create.content.kinetics.base.IRotate;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.createmod.catnip.data.Couple;
import net.createmod.catnip.data.Pair;
import net.createmod.catnip.math.BlockFace;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.ty.createcraftedbeginning.content.aircompressor.AirCompressorBlock;
import net.ty.createcraftedbeginning.content.airtightencasedpipe.AirtightEncasedPipeBlock;
import net.ty.createcraftedbeginning.content.airtightintakeport.AirtightIntakePortBlock;
import net.ty.createcraftedbeginning.content.airtightpipe.AirtightPipeBlock;
import net.ty.createcraftedbeginning.content.airtighttank.AirtightTankBlock;
import net.ty.createcraftedbeginning.content.gasinjectionchamber.GasInjectionChamberBlock;
import net.ty.createcraftedbeginning.data.CCBTags;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class AirtightPumpBlockEntity extends PumpBlockEntity {
    public AirtightPumpBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        behaviours.add(new CompressedAirTransferBehaviour(this));
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
            FluidPropagator.resetAffectedFluidNetworks(level, worldPosition, side.getOpposite());
        }

        if (!hasReachedValidEndpoint(level, start, pull)) {
            pipeGraph.computeIfAbsent(worldPosition, $ -> Pair.of(0, new IdentityHashMap<>())).getSecond().put(side, pull);
            pipeGraph.computeIfAbsent(start.getConnectedPos(), $ -> Pair.of(1, new IdentityHashMap<>())).getSecond().put(side.getOpposite(), !pull);

            List<Pair<Integer, BlockPos>> frontier = new ArrayList<>();
            Set<BlockPos> visited = new HashSet<>();
            int maxDistance = FluidPropagator.getPumpRange() * 2;
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
                FluidTransportBehaviour pipe = FluidPropagator.getPipe(level, currentPos);
                if (pipe == null) {
                    continue;
                }

                for (Direction face : FluidPropagator.getPipeConnections(currentState, pipe)) {
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

                    FluidTransportBehaviour pipeBehaviour = FluidPropagator.getPipe(level, connectedPos);
                    if (pipeBehaviour == null) {
                        continue;
                    }
                    if (pipeBehaviour instanceof CompressedAirTransferBehaviour) {
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
                FluidTransportBehaviour pipeBehaviour = FluidPropagator.getPipe(level, pipePos);
                if (pipeBehaviour == null) {
                    continue;
                }

                pipeBehaviour.addPressure(pipeSide, inbound, pressure / parallelBranches);
            }
        }
    }

    private boolean hasReachedValidEndpoint(LevelAccessor world, BlockFace blockFace, boolean pull) {
        BlockPos connectedPos = blockFace.getConnectedPos();
        BlockState connectedState = world.getBlockState(connectedPos);
        BlockEntity blockEntity = world.getBlockEntity(connectedPos);
        Direction face = blockFace.getFace();

        if (PumpBlock.isPump(connectedState) && connectedState.getValue(PumpBlock.FACING).getAxis() == face.getAxis() && blockEntity instanceof AirtightPumpBlockEntity pumpBE) {
            return pumpBE.isPullingOnSide(pumpBE.isFront(blockFace.getOppositeFace())) != pull;
        }

        FluidTransportBehaviour pipe = FluidPropagator.getPipe(world, connectedPos);
        if (pipe != null && pipe.canHaveFlowToward(connectedState, blockFace.getOppositeFace())) {
            return false;
        }

        if (blockEntity != null && blockEntity.getLevel() != null) {
            IFluidHandler capability = blockEntity.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, blockEntity.getBlockPos(), face.getOpposite());
            if (capability != null) {
                return true;
            }
        }

        return FluidPropagator.isOpenEnd(world, blockFace.getPos(), face);
    }


    @Override
    public void lazyTick() {
        super.lazyTick();
        updatePressureChange();
    }

    class CompressedAirTransferBehaviour extends FluidTransportBehaviour {
        public CompressedAirTransferBehaviour(AirtightPumpBlockEntity be) {
            super(be);
        }

        @Override
        public boolean canPullFluidFrom(FluidStack fluid, BlockState state, Direction direction) {
            if (!fluid.is(CCBTags.commonFluidTag("compressed_air"))) {
                return false;
            }

            BlockState otherState = getWorld().getBlockState(getPos().relative(direction));
            Block otherBlock = otherState.getBlock();
            Direction.Axis axis = state.getValue(AirtightPumpBlock.FACING).getAxis();

            return switch (otherBlock) {
                case AirtightPipeBlock ignored when otherState.getValue(AirtightPipeBlock.AXIS) == axis -> true;
                case AirtightPumpBlock ignored when otherState.getValue(AirtightPumpBlock.FACING).getAxis() == axis -> true;
                case AirCompressorBlock ignored when otherState.getValue(AirCompressorBlock.HORIZONTAL_FACING).getClockWise().getAxis() == axis -> true;
                case AirtightIntakePortBlock ignored -> AirtightIntakePortBlock.isValidDirection(axis, direction, otherState);
                case GasInjectionChamberBlock ignored -> true;
                case AirtightTankBlock ignored -> true;
                case AirtightEncasedPipeBlock ignored -> true;
                case FluidTankBlock ignored -> true;
                default -> false;
            };
        }

        @Override
        public boolean canHaveFlowToward(BlockState state, Direction direction) {
            return state.getBlock() instanceof AirtightPumpBlock && state.getValue(AirtightPumpBlock.FACING).getAxis() == direction.getAxis();
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

            for (Map.Entry<Direction, PipeConnection> entry : interfaces.entrySet()) {
                boolean pull = isPullingOnSide(isFront(entry.getKey()));
                Couple<Float> pressure = entry.getValue().getPressure();
                pressure.set(pull, absSpeed * 2f);
                pressure.set(!pull, 0f);
            }
        }

        @Override
        public AttachmentTypes getRenderedRimAttachment(BlockAndTintGetter world, BlockPos pos, BlockState state, Direction direction) {
            return AttachmentTypes.NONE;
        }
    }
}
