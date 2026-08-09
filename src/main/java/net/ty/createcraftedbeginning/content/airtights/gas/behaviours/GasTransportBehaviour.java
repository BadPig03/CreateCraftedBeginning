package net.ty.createcraftedbeginning.content.airtights.gas.behaviours;

import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BehaviourType;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.createmod.catnip.data.Iterate;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.ty.createcraftedbeginning.api.gas.gases.GasCapabilities;
import net.ty.createcraftedbeginning.api.gas.gases.GasStack;
import net.ty.createcraftedbeginning.api.gas.gases.collisions.GasCollisionEvent;
import net.ty.createcraftedbeginning.content.airtights.airtightpipe.AirtightPipeAttachmentTypes.AttachmentTypes;
import net.ty.createcraftedbeginning.content.airtights.airtightpump.AirtightPumpBlock;
import net.ty.createcraftedbeginning.content.airtights.gas.interfaces.IAirtightComponent;
import net.ty.createcraftedbeginning.content.airtights.gas.transport.GasPipeConnection;
import net.ty.createcraftedbeginning.content.airtights.gas.transport.GasPipeConnection.AirFlow;
import net.ty.createcraftedbeginning.registry.CCBTags.CCBBlockTags;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.List;
import java.util.function.Predicate;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@SuppressWarnings("unused")
public abstract class GasTransportBehaviour extends BlockEntityBehaviour {
    public static final BehaviourType<GasTransportBehaviour> TYPE = new BehaviourType<>();

    private static final int CONNECTION_REFRESH_INTERVAL = 20;

    private EnumMap<Direction, GasPipeConnection> interfaces;
    @Nullable
    private List<GasPipeConnection> retiredConnections;
    private UpdatePhase phase;
    private boolean connectionsDirty;
    private int connectionRefreshTicks;

    public GasTransportBehaviour(SmartBlockEntity be) {
        super(be);
        phase = UpdatePhase.WAIT_FOR_PUMPS;
        connectionsDirty = true;
    }

    public static boolean isValidAirtightComponents(@Nullable Level level, BlockPos pos, BlockState state, Direction direction) {
        return level != null && (state.getBlock() instanceof IAirtightComponent component && component.isAirtight(pos, state, direction) || state.getDestroySpeed(level, pos) != -1 && (state.canBeReplaced() || CCBBlockTags.GAS_SOURCES.matches(state)) || GasCapabilities.hasGasCapability(level, pos, direction.getOpposite()));
    }

    public abstract boolean canHaveFlowToward(BlockState state, Direction direction);

    public abstract boolean canHaveFlowTowardWithoutLevel(BlockState state, Direction direction);

    public boolean allowsInboundFlow(BlockState state, Direction direction) {
        return canHaveFlowToward(state, direction);
    }

    public boolean allowsOutboundFlow(BlockState state, Direction direction) {
        return canHaveFlowToward(state, direction);
    }

    public GasStack getProvidedOutwardGas(Direction side) {
        GasPipeConnection connection = getConnection(side);
        if (connection == null || !allowsOutboundFlow(blockEntity.getBlockState(), side)) {
            return GasStack.EMPTY;
        }
        return connection.provideOutboundFlow();
    }

    private void createConnectionData() {
        if (interfaces != null) {
            return;
        }

        interfaces = new EnumMap<>(Direction.class);
        BlockState state = blockEntity.getBlockState();
        Level level = getWorld();
        for (Direction direction : Iterate.directions) {
            boolean canConnect = level == null ? canHaveFlowTowardWithoutLevel(state, direction) : canHaveFlowToward(state, direction);
            if (!canConnect) {
                continue;
            }

            interfaces.put(direction, new GasPipeConnection(direction));
        }
    }

    private void refreshConnectionData() {
        Level level = getWorld();
        if (interfaces == null) {
            createConnectionData();
            return;
        }

        if (level == null) {
            return;
        }

        boolean flowRemoved = false;
        BlockState state = blockEntity.getBlockState();
        for (Direction direction : Iterate.directions) {
            if (canHaveFlowToward(state, direction)) {
                if (interfaces.containsKey(direction)) {
                    continue;
                }

                interfaces.put(direction, new GasPipeConnection(direction));
                continue;
            }

            GasPipeConnection connection = interfaces.remove(direction);
            if (connection == null) {
                continue;
            }

            flowRemoved |= connection.getFlow() != null;
            retireConnection(connection);
        }
        if (!flowRemoved || level.isClientSide && !blockEntity.isVirtual()) {
            return;
        }

        blockEntity.notifyUpdate();
    }

    private void retireConnection(GasPipeConnection connection) {
        if (connection.prepareForRemoval()) {
            return;
        }

        if (retiredConnections == null) {
            retiredConnections = new ArrayList<>();
        }
        retiredConnections.add(connection);
    }

    private void recoverRetiredConnections() {
        if (retiredConnections == null) {
            return;
        }

        retiredConnections.removeIf(GasPipeConnection::prepareForRemoval);
        if (!retiredConnections.isEmpty()) {
            return;
        }

        retiredConnections = null;
    }

    protected final void refreshConnections() {
        refreshConnectionData();
        recoverRetiredConnections();
        connectionsDirty = false;
        connectionRefreshTicks = CONNECTION_REFRESH_INTERVAL;
    }

    private void refreshConnectionsIfNeeded() {
        if (!connectionsDirty && --connectionRefreshTicks > 0) {
            recoverRetiredConnections();
            return;
        }

        refreshConnections();
    }

    public void markConnectionsDirty() {
        connectionsDirty = true;
    }

    protected void beforeFlowUpdate(Level level, BlockPos pos, Collection<GasPipeConnection> connections) {
    }

    @Nullable
    public GasPipeConnection getConnection(Direction side) {
        if (connectionsDirty) {
            refreshConnections();
        }
        else {
            createConnectionData();
        }
        return interfaces.get(side);
    }

    public boolean hasAnyPressure() {
        if (connectionsDirty) {
            refreshConnections();
        }
        else {
            createConnectionData();
        }
        for (GasPipeConnection pipeConnection : interfaces.values()) {
            if (!pipeConnection.hasPressure()) {
                continue;
            }

            return true;
        }
        return false;
    }

    public boolean hasAnyPressureContribution() {
        if (connectionsDirty) {
            refreshConnections();
        }
        else {
            createConnectionData();
        }
        for (GasPipeConnection pipeConnection : interfaces.values()) {
            if (!pipeConnection.hasPressureContribution()) {
                continue;
            }

            return true;
        }
        return false;
    }

    @Nullable
    public AirFlow getFlow(Direction side) {
        GasPipeConnection connection = getConnection(side);
        return connection == null ? null : connection.getFlow();
    }

    public void addPressure(Direction side, boolean inbound, float pressure) {
        GasPipeConnection connection = getConnection(side);
        BlockState state = blockEntity.getBlockState();
        boolean flowAllowed = inbound ? allowsInboundFlow(state, side) : allowsOutboundFlow(state, side);
        if (connection == null || !flowAllowed) {
            return;
        }

        connection.addPressure(inbound, pressure);
    }

    public void wipePressure() {
        refreshConnectionsIfNeeded();
        phase = UpdatePhase.WAIT_FOR_PUMPS;
        for (GasPipeConnection connection : interfaces.values()) {
            connection.wipePressure();
        }
    }

    public boolean isIncorrectAxis(BlockState state, Direction direction) {
        return state.getValue(BlockStateProperties.AXIS) != direction.getAxis();
    }

    public AttachmentTypes getRenderedRimAttachment(BlockAndTintGetter level, BlockPos pos, BlockState state, Direction direction) {
        if (!canHaveFlowToward(state, direction)) {
            return AttachmentTypes.NONE;
        }

        BlockPos offsetPos = pos.relative(direction);
        BlockState facingState = level.getBlockState(offsetPos);
        if (facingState.getBlock() instanceof AirtightPumpBlock && direction.getOpposite() == facingState.getValue(AirtightPumpBlock.FACING)) {
            return AttachmentTypes.NONE;
        }

        if (GasCapabilities.hasGasCapability(level, offsetPos, direction.getOpposite())) {
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
        refreshConnections();
    }

    @Override
    public void tick() {
        Level level = getWorld();
        if (level == null) {
            return;
        }

        super.tick();
        BlockPos pos = getPos();
        boolean isClientSide = level.isClientSide && !blockEntity.isVirtual();
        refreshConnectionsIfNeeded();
        Collection<GasPipeConnection> connections = interfaces.values();
        beforeFlowUpdate(level, pos, connections);
        if (phase == UpdatePhase.WAIT_FOR_PUMPS) {
            phase = UpdatePhase.FLIP_FLOWS;
            return;
        }

        if (!isClientSide) {
            updateSources(level, pos, connections);
        }

        if (phase == UpdatePhase.FLIP_FLOWS) {
            phase = UpdatePhase.IDLE;
            return;
        }

        if (!isClientSide && updateFlows(level, pos, connections)) {
            return;
        }

        for (GasPipeConnection connection : connections) {
            connection.tickFlowProgress(level, pos);
        }
    }

    @Override
    public void read(CompoundTag compoundTag, Provider provider, boolean clientPacket) {
        super.read(compoundTag, provider, clientPacket);
        refreshConnections();
        if (!clientPacket) {
            phase = UpdatePhase.WAIT_FOR_PUMPS;
        }
        for (GasPipeConnection connection : interfaces.values()) {
            connection.read(compoundTag, provider, blockEntity.getBlockPos(), clientPacket);
        }
    }

    @Override
    public void write(CompoundTag compoundTag, Provider provider, boolean clientPacket) {
        super.write(compoundTag, provider, clientPacket);
        createConnectionData();
        for (GasPipeConnection connection : interfaces.values()) {
            connection.write(compoundTag, provider, clientPacket);
        }
    }

    private void updateSources(Level level, BlockPos pos, Collection<GasPipeConnection> connections) {
        boolean sendUpdate = false;
        for (GasPipeConnection connection : connections) {
            sendUpdate |= connection.flipFlowsIfPressureReversed();
            connection.manageSource(level, pos, blockEntity);
        }
        if (!sendUpdate) {
            return;
        }

        blockEntity.notifyUpdate();
    }

    private boolean updateFlows(Level level, BlockPos pos, Collection<GasPipeConnection> connections) {
        BlockState state = blockEntity.getBlockState();
        GasPipeConnection singleSource = null;
        GasStack availableFlow = GasStack.EMPTY;
        GasStack collidingFlow = GasStack.EMPTY;
        for (GasPipeConnection connection : connections) {
            Direction side = connection.getSide();
            GasStack gasInFlow = connection.getProvidedGas();
            if (gasInFlow.isEmpty() || !allowsInboundFlow(state, side) || !canPullGasFrom(gasInFlow, state, side)) {
                continue;
            }

            if (availableFlow.isEmpty()) {
                singleSource = connection;
                availableFlow = gasInFlow;
                continue;
            }

            if (GasStack.isSameGasSameComponents(availableFlow, gasInFlow)) {
                singleSource = null;
                availableFlow = gasInFlow;
                continue;
            }

            collidingFlow = gasInFlow;
            break;
        }

        if (!collidingFlow.isEmpty()) {
            GasCollisionEvent.handleCollision(level, pos, availableFlow, collidingFlow);
            return true;
        }

        boolean sendUpdate = false;
        for (GasPipeConnection connection : connections) {
            Direction side = connection.getSide();
            boolean allowsInbound = allowsInboundFlow(state, side);
            boolean allowsOutbound = allowsOutboundFlow(state, side);
            GasStack internalGas = singleSource == connection || !allowsOutbound ? GasStack.EMPTY : availableFlow;
            Predicate<GasStack> extractionPredicate = extracted -> allowsInbound && canPullGasFrom(extracted, state, side);
            sendUpdate |= connection.manageFlows(level, pos, internalGas, extractionPredicate);
        }
        if (!sendUpdate) {
            return false;
        }

        blockEntity.notifyUpdate();
        return false;
    }

    public boolean canPullGasFrom(GasStack gas, BlockState state, Direction direction) {
        return true;
    }

    private enum UpdatePhase {
        WAIT_FOR_PUMPS,
        FLIP_FLOWS,
        IDLE
    }
}
