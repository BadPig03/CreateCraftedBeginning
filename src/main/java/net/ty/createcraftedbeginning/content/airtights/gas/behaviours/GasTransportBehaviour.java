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
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
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
import net.ty.createcraftedbeginning.content.airtights.gas.transport.GasPipeConnection.GasFlow;
import net.ty.createcraftedbeginning.content.airtights.gas.transport.GasPropagator;
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
public abstract class GasTransportBehaviour extends BlockEntityBehaviour {
    public static final BehaviourType<GasTransportBehaviour> TYPE = new BehaviourType<>();

    private static final int CONNECTION_REFRESH_INTERVAL = 20;
    private static final String COMPOUND_KEY_RETIRED_CONNECTIONS = "RetiredGasConnections";

    protected EnumMap<Direction, GasPipeConnection> interfaces;
    @Nullable
    protected List<GasPipeConnection> retiredConnections;
    protected UpdatePhase phase;
    protected boolean connectionsDirty;
    protected int connectionRefreshTicks;
    private boolean periodicTopologyRefreshPending;

    public GasTransportBehaviour(SmartBlockEntity be) {
        super(be);
        phase = UpdatePhase.WAIT_FOR_PUMPS;
        connectionsDirty = true;
    }

    public static boolean isValidAirtightComponents(@Nullable Level level, BlockPos pos, BlockState state, Direction directionToTarget) {
        if (level == null) {
            return false;
        }

        Direction localFace = directionToTarget.getOpposite();
        return state.getBlock() instanceof IAirtightComponent component && component.canConnectOnFace(pos, state, localFace) || state.getDestroySpeed(level, pos) != -1 && (state.canBeReplaced() || CCBBlockTags.GAS_SOURCES.matches(state)) || GasCapabilities.hasGasCapability(level, pos, localFace);
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

    protected void createConnectionData() {
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

    protected void refreshConnectionData() {
        Level level = getWorld();
        if (interfaces == null) {
            createConnectionData();
            return;
        }

        if (level == null) {
            return;
        }

        boolean topologyChanged = false;
        boolean flowRemoved = false;
        BlockState state = blockEntity.getBlockState();
        for (Direction direction : Iterate.directions) {
            if (canHaveFlowToward(state, direction)) {
                if (interfaces.containsKey(direction)) {
                    continue;
                }

                interfaces.put(direction, new GasPipeConnection(direction));
                topologyChanged = true;
                continue;
            }

            GasPipeConnection connection = interfaces.remove(direction);
            if (connection == null) {
                continue;
            }

            topologyChanged = true;
            flowRemoved |= connection.getFlow() != null;
            retireConnection(connection);
        }
        if (topologyChanged && !connectionsDirty && !level.isClientSide) {
            periodicTopologyRefreshPending = true;
        }
        if (!flowRemoved || level.isClientSide && !blockEntity.isVirtual()) {
            return;
        }

        blockEntity.notifyUpdate();
    }

    protected void retireConnection(GasPipeConnection connection) {
        Level level = getWorld();
        if (level != null && connection.prepareForRemoval(level, getPos())) {
            return;
        }

        connection.beginRetiredRecoveryBackoff();
        if (retiredConnections == null) {
            retiredConnections = new ArrayList<>();
        }
        retiredConnections.add(connection);
        blockEntity.setChanged();
    }

    protected void recoverRetiredConnections() {
        if (retiredConnections == null) {
            return;
        }

        Level level = getWorld();
        if (level == null) {
            return;
        }

        BlockPos pos = getPos();
        retiredConnections.removeIf(connection -> connection.tryRecoverRetired(level, pos));
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

    protected void refreshConnectionsIfNeeded() {
        if (!connectionsDirty && --connectionRefreshTicks > 0) {
            recoverRetiredConnections();
            return;
        }

        refreshConnections();
    }

    public void markConnectionsDirty() {
        connectionsDirty = true;
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
    public GasFlow getFlow(Direction side) {
        GasPipeConnection connection = getConnection(side);
        if (connection == null) {
            return null;
        }
        return connection.getFlow();
    }

    public void addPressureUnits(Direction side, boolean inbound, long pressureUnits) {
        GasPipeConnection connection = getConnection(side);
        BlockState state = blockEntity.getBlockState();
        boolean flowAllowed = inbound ? allowsInboundFlow(state, side) : allowsOutboundFlow(state, side);
        if (connection == null || !flowAllowed) {
            return;
        }

        connection.addPressureUnits(inbound, pressureUnits);
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
        if (!level.isClientSide && periodicTopologyRefreshPending) {
            periodicTopologyRefreshPending = false;
            GasPropagator.propagatePipe(level, pos);
            return;
        }

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
        if (clientPacket) {
            return;
        }

        readRetiredConnections(compoundTag, provider);
    }

    @Override
    public void write(CompoundTag compoundTag, Provider provider, boolean clientPacket) {
        super.write(compoundTag, provider, clientPacket);
        createConnectionData();
        for (GasPipeConnection connection : interfaces.values()) {
            connection.write(compoundTag, provider, clientPacket);
        }
        if (clientPacket) {
            return;
        }

        writeRetiredConnections(compoundTag, provider);
    }

    protected void readRetiredConnections(CompoundTag compoundTag, Provider provider) {
        retiredConnections = null;
        if (!compoundTag.contains(COMPOUND_KEY_RETIRED_CONNECTIONS, Tag.TAG_LIST)) {
            return;
        }

        ListTag retiredData = compoundTag.getList(COMPOUND_KEY_RETIRED_CONNECTIONS, Tag.TAG_COMPOUND);
        for (int i = 0; i < retiredData.size(); i++) {
            GasPipeConnection connection = GasPipeConnection.readRetiredData(retiredData.getCompound(i), provider);
            if (connection == null) {
                continue;
            }

            if (retiredConnections == null) {
                retiredConnections = new ArrayList<>();
            }
            retiredConnections.add(connection);
        }
    }

    protected void writeRetiredConnections(CompoundTag compoundTag, Provider provider) {
        if (retiredConnections == null || retiredConnections.isEmpty()) {
            return;
        }

        ListTag retiredData = new ListTag();
        for (GasPipeConnection connection : retiredConnections) {
            CompoundTag connectionData = connection.writeRetiredData(provider);
            if (connectionData.isEmpty()) {
                continue;
            }

            retiredData.add(connectionData);
        }
        if (retiredData.isEmpty()) {
            return;
        }

        compoundTag.put(COMPOUND_KEY_RETIRED_CONNECTIONS, retiredData);
    }

    protected void updateSources(Level level, BlockPos pos, Collection<GasPipeConnection> connections) {
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

    protected boolean updateFlows(Level level, BlockPos pos, Collection<GasPipeConnection> connections) {
        BlockState state = blockEntity.getBlockState();
        boolean sendUpdate = false;
        for (GasPipeConnection connection : connections) {
            Direction side = connection.getSide();
            boolean allowsInbound = allowsInboundFlow(state, side);
            Predicate<GasStack> extractionPredicate = extracted -> allowsInbound && canPullGasFrom(extracted, state, side);
            sendUpdate |= connection.validateExistingInboundFlow(level, pos, extractionPredicate);
        }

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

        for (GasPipeConnection connection : connections) {
            Direction side = connection.getSide();
            boolean allowsInbound = allowsInboundFlow(state, side);
            boolean allowsOutbound = allowsOutboundFlow(state, side);
            GasStack internalGas = singleSource == connection || !allowsOutbound ? GasStack.EMPTY : availableFlow;
            Predicate<GasStack> extractionPredicate = extracted -> allowsInbound && canPullGasFrom(extracted, state, side);
            sendUpdate |= connection.manageFlows(level, pos, internalGas, extractionPredicate);
        }

        GasStack updatedFlow = GasStack.EMPTY;
        for (GasPipeConnection connection : connections) {
            Direction side = connection.getSide();
            GasStack gasInFlow = connection.getProvidedGas();
            if (gasInFlow.isEmpty() || !allowsInboundFlow(state, side) || !canPullGasFrom(gasInFlow, state, side)) {
                continue;
            }

            if (updatedFlow.isEmpty()) {
                updatedFlow = gasInFlow;
                continue;
            }

            if (GasStack.isSameGasSameComponents(updatedFlow, gasInFlow)) {
                continue;
            }

            GasCollisionEvent.handleCollision(level, pos, updatedFlow, gasInFlow);
            return true;
        }

        for (GasPipeConnection connection : connections) {
            connection.tickNetwork();
        }

        if (!sendUpdate) {
            return false;
        }

        blockEntity.notifyUpdate();
        return false;
    }

    protected void beforeFlowUpdate(Level level, BlockPos pos, Collection<GasPipeConnection> connections) {
    }

    public boolean canPullGasFrom(GasStack gas, BlockState state, Direction direction) {
        return true;
    }

    protected enum UpdatePhase {
        WAIT_FOR_PUMPS,
        FLIP_FLOWS,
        IDLE
    }
}
