package net.ty.createcraftedbeginning.api.gas;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BehaviourType;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.createmod.catnip.data.Iterate;
import net.createmod.catnip.data.WorldAttached;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.ty.createcraftedbeginning.content.airtights.airtightpump.AirtightPumpBlock;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.function.Predicate;

@SuppressWarnings("unused")
public abstract class GasTransportBehaviour extends BlockEntityBehaviour {
    public static final BehaviourType<GasTransportBehaviour> TYPE = new BehaviourType<>();
    public static final WorldAttached<Map<BlockPos, Map<Direction, GasPipeConnection>>> interfaceTransfer = new WorldAttached<>($ -> new HashMap<>());
    public Map<Direction, GasPipeConnection> interfaces;
    public UpdatePhase phase;

    public GasTransportBehaviour(SmartBlockEntity be) {
        super(be);
        phase = UpdatePhase.WAIT_FOR_PUMPS;
    }

    public static void cacheFlows(LevelAccessor world, BlockPos pos) {
        GasTransportBehaviour pipe = BlockEntityBehaviour.get(world, pos, GasTransportBehaviour.TYPE);
        if (pipe != null) {
            interfaceTransfer.get(world).put(pos, pipe.interfaces);
        }
    }

    public static void loadFlows(LevelAccessor world, BlockPos pos) {
        GasTransportBehaviour newPipe = BlockEntityBehaviour.get(world, pos, GasTransportBehaviour.TYPE);
        if (newPipe != null) {
            newPipe.interfaces = interfaceTransfer.get(world).remove(pos);
        }
    }

    public boolean canPullGasFrom(GasStack gas, BlockState state, Direction direction) {
        return true;
    }

    public abstract boolean canHaveFlowToward(BlockState state, Direction direction);

    public GasStack getProvidedOutwardGas(Direction side) {
        createConnectionData();
        if (!interfaces.containsKey(side)) {
            return GasStack.EMPTY;
        }
        return interfaces.get(side).provideOutboundFlow();
    }

    @Nullable
    public GasPipeConnection getConnection(Direction side) {
        createConnectionData();
        return interfaces.get(side);
    }

    public boolean hasAnyPressure() {
        createConnectionData();
        for (GasPipeConnection pipeConnection : interfaces.values()) {
            if (pipeConnection.hasPressure()) {
                return true;
            }
        }
        return false;
    }

    @Nullable
    public GasPipeConnection.AirFlow getFlow(Direction side) {
        createConnectionData();
        if (!interfaces.containsKey(side)) {
            return null;
        }
        return interfaces.get(side).flow.orElse(null);
    }

    public void addPressure(Direction side, boolean inbound, float pressure) {
        createConnectionData();
        if (!interfaces.containsKey(side)) {
            return;
        }
        interfaces.get(side).addPressure(inbound, pressure);
        blockEntity.sendData();
    }

    public void wipePressure() {
        if (interfaces != null) {
            for (Direction d : Iterate.directions) {
                if (!canHaveFlowToward(blockEntity.getBlockState(), d)) {
                    interfaces.remove(d);
                } else {
                    interfaces.computeIfAbsent(d, GasPipeConnection::new);
                }
            }
        }
        phase = UpdatePhase.WAIT_FOR_PUMPS;
        createConnectionData();
        interfaces.values().forEach(GasPipeConnection::wipePressure);
        blockEntity.sendData();
    }

    private void createConnectionData() {
        if (interfaces != null) {
            return;
        }
        interfaces = new IdentityHashMap<>();
        for (Direction d : Iterate.directions) {
            if (canHaveFlowToward(blockEntity.getBlockState(), d)) {
                interfaces.put(d, new GasPipeConnection(d));
            }
        }
    }

    public AttachmentTypes getRenderedRimAttachment(BlockAndTintGetter world, BlockPos pos, BlockState state, Direction direction) {
        if (!canHaveFlowToward(state, direction)) {
            return AttachmentTypes.NONE;
        }

        BlockPos offsetPos = pos.relative(direction);
        BlockState facingState = world.getBlockState(offsetPos);
        if (facingState.getBlock() instanceof AirtightPumpBlock && facingState.getValue(AirtightPumpBlock.FACING) == direction.getOpposite()) {
            return AttachmentTypes.NONE;
        }

        if (GasPropagator.hasGasCapability(world, offsetPos, direction.getOpposite()) && !AllBlocks.HOSE_PULLEY.has(facingState)) {
            return AttachmentTypes.DRAIN;
        }

        return AttachmentTypes.RIM;
    }

    @Override
    public BehaviourType<?> getType() {
        return TYPE;
    }

    @Override
    public void initialize() {
        super.initialize();
        createConnectionData();
    }

    @Override
    public void tick() {
        super.tick();

        Level world = getWorld();
        BlockPos pos = getPos();
        boolean onServer = !world.isClientSide || blockEntity.isVirtual();

        if (interfaces == null) {
            return;
        }
        Collection<GasPipeConnection> connections = interfaces.values();
        GasPipeConnection singleSource = null;

        if (phase == UpdatePhase.WAIT_FOR_PUMPS) {
            phase = UpdatePhase.FLIP_FLOWS;
            return;
        }

        if (onServer) {
            boolean sendUpdate = false;
            for (GasPipeConnection connection : connections) {
                sendUpdate |= connection.flipFlowsIfPressureReversed();
                connection.manageSource(world, pos, blockEntity);
            }
            if (sendUpdate) {
                blockEntity.notifyUpdate();
            }
        }

        if (phase == UpdatePhase.FLIP_FLOWS) {
            phase = UpdatePhase.IDLE;
            return;
        }

        if (onServer) {
            GasStack availableFlow = GasStack.EMPTY;
            GasStack collidingFlow = GasStack.EMPTY;

            for (GasPipeConnection connection : connections) {
                GasStack gasInFlow = connection.getProvidedGas();
                if (gasInFlow.isEmpty()) {
                    continue;
                }
                if (availableFlow.isEmpty()) {
                    singleSource = connection;
                    availableFlow = gasInFlow;
                    continue;
                }
                if (GasStack.isSameGas(availableFlow, gasInFlow)) {
                    singleSource = null;
                    availableFlow = gasInFlow;
                    continue;
                }
                collidingFlow = gasInFlow;
                break;
            }

            if (!collidingFlow.isEmpty()) {
                GasReactions.handlePipeFlowCollision(world, pos, availableFlow, collidingFlow);
                return;
            }

            boolean sendUpdate = false;
            for (GasPipeConnection connection : connections) {
                GasStack internalGas = singleSource != connection ? availableFlow : GasStack.EMPTY;
                Predicate<GasStack> extractionPredicate = extracted -> canPullGasFrom(extracted, blockEntity.getBlockState(), connection.side);
                sendUpdate |= connection.manageFlows(world, pos, internalGas, extractionPredicate);
            }

            if (sendUpdate) {
                blockEntity.notifyUpdate();
            }
        }

        for (GasPipeConnection connection : connections) {
            connection.tickFlowProgress(world, pos);
        }
    }

    @Override
    public void read(CompoundTag nbt, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(nbt, registries, clientPacket);
        if (interfaces == null) {
            interfaces = new IdentityHashMap<>();
        }
        for (Direction face : Iterate.directions) {
            if (nbt.contains(face.getName())) {
                interfaces.computeIfAbsent(face, GasPipeConnection::new);
            }
        }

        if (interfaces.isEmpty()) {
            interfaces = null;
            return;
        }

        interfaces.values().forEach(connection -> connection.deserializeNBT(nbt, registries, blockEntity.getBlockPos(), clientPacket));
    }

    @Override
    public void write(CompoundTag nbt, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(nbt, registries, clientPacket);
        if (clientPacket) {
            createConnectionData();
        }
        if (interfaces == null) {
            return;
        }

        interfaces.values().forEach(connection -> connection.serializeNBT(nbt, registries, clientPacket));
    }

    public enum UpdatePhase {
        WAIT_FOR_PUMPS,
        FLIP_FLOWS,
        IDLE
    }

    public enum AttachmentTypes {
        NONE,
        CONNECTION(ComponentPartials.CONNECTION),
        DETAILED_CONNECTION(ComponentPartials.RIM_CONNECTOR),
        RIM(ComponentPartials.RIM_CONNECTOR, ComponentPartials.RIM),
        PARTIAL_RIM(ComponentPartials.RIM),
        DRAIN(ComponentPartials.RIM_CONNECTOR, ComponentPartials.DRAIN),
        PARTIAL_DRAIN(ComponentPartials.DRAIN);

        public final ComponentPartials[] partials;

        AttachmentTypes(ComponentPartials... partials) {
            this.partials = partials;
        }

        public AttachmentTypes withoutConnector() {
            if (this == AttachmentTypes.RIM) {
                return AttachmentTypes.PARTIAL_RIM;
            }
            if (this == AttachmentTypes.DRAIN) {
                return AttachmentTypes.PARTIAL_DRAIN;
            }
            return this;
        }

        public enum ComponentPartials {
            CONNECTION,
            RIM_CONNECTOR,
            RIM,
            DRAIN
        }
    }
}
