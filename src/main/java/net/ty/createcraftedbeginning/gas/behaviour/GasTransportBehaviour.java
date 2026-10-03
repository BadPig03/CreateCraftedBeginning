package net.ty.createcraftedbeginning.gas.behaviour;

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
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.ty.createcraftedbeginning.api.gas.GasCapabilities;
import net.ty.createcraftedbeginning.api.gas.GasStack;
import net.ty.createcraftedbeginning.api.gasreleasehandlers.GasReleaseCause;
import net.ty.createcraftedbeginning.foundation.BoundedMath;
import net.ty.createcraftedbeginning.gas.network.GasConnectable;
import net.ty.createcraftedbeginning.gas.network.GasFlowResistance;
import net.ty.createcraftedbeginning.gas.network.GasPipeConnection;
import net.ty.createcraftedbeginning.gas.network.GasPipeConnection.FlowDirection;
import net.ty.createcraftedbeginning.gas.network.GasPipeConnection.FlowState;
import net.ty.createcraftedbeginning.gas.network.GasTransportEdgeProperties;
import net.ty.createcraftedbeginning.gas.network.solver.GasNetworkSimulator;
import net.ty.createcraftedbeginning.gas.network.solver.GasNetworkTopology;
import net.ty.createcraftedbeginning.gas.release.GasReleaseRequest;
import net.ty.createcraftedbeginning.gas.release.GasReleaseService;
import net.ty.createcraftedbeginning.gas.telemetry.GasFlowTelemetryTarget;
import net.ty.createcraftedbeginning.registry.CCBTags.CCBBlockTags;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public abstract class GasTransportBehaviour extends BlockEntityBehaviour {
    public static final BehaviourType<GasTransportBehaviour> TYPE = new BehaviourType<>();

    private static final int CONNECTION_REFRESH_INTERVAL = 20;
    private static final long PIPE_RESISTANCE_UNITS = GasFlowResistance.fromFactor(0.958);
    private static final String COMPOUND_KEY_RETIRED_CONNECTIONS = "RetiredGasConnections";

    private EnumMap<Direction, GasPipeConnection> connections;
    private @Nullable List<GasPipeConnection> retiredConnections;
    private boolean connectionsDirty;
    private int connectionRefreshTicks;
    private boolean periodicTopologyRefreshPending;

    protected GasTransportBehaviour(SmartBlockEntity blockEntity) {
        super(blockEntity);
        connectionsDirty = true;
    }

    public static boolean isValidConnectionTarget(@Nullable Level level, BlockPos pos, BlockState state, Direction directionFromPipe) {
        if (level == null) {
            return false;
        }

        Direction localFace = directionFromPipe.getOpposite();
        return state.getBlock() instanceof GasConnectable component && component.canConnectOnFace(pos, state, localFace) || state.getDestroySpeed(level, pos) != -1 && (state.canBeReplaced() || CCBBlockTags.GAS_SOURCES.matches(state)) || GasCapabilities.hasBlockHandler(level, pos, localFace);
    }

    private static void finalizePendingTransfer(Level level, BlockPos pos, GasPipeConnection connection) {
        if (!connection.hasPendingTransfer()) {
            return;
        }

        GasStack unrecoveredTransfer = connection.finalizePendingTransferForOwnerRemoval(level, pos);
        if (unrecoveredTransfer.isEmpty()) {
            return;
        }

        GasReleaseService.release(level, GasReleaseRequest.radial(unrecoveredTransfer, pos, GasReleaseCause.RUPTURE));
    }

    @Override
    public BehaviourType<?> getType() {
        return TYPE;
    }

    @Override
    public void initialize() {
        super.initialize();
        Level level = getWorld();
        if (level != null) {
            GasNetworkTopology.invalidate(level, getPos());
        }

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
        refreshConnectionsIfNeeded();
        if (!level.isClientSide && periodicTopologyRefreshPending) {
            periodicTopologyRefreshPending = false;
            GasNetworkTopology.invalidate(level, pos);
            GasNetworkSimulator.simulateTick(level, pos);
            return;
        }

        if (level.isClientSide) {
            return;
        }

        GasNetworkSimulator.simulateTick(level, pos);
    }

    @Override
    public void read(CompoundTag compoundTag, Provider provider, boolean clientPacket) {
        super.read(compoundTag, provider, clientPacket);
        if (clientPacket) {
            return;
        }

        refreshConnections();
        BlockPos pos = blockEntity.getBlockPos();
        for (GasPipeConnection connection : connections.values()) {
            connection.read(compoundTag, provider, pos, false);
        }

        readRetiredConnections(compoundTag, provider);
    }

    @Override
    public void write(CompoundTag compoundTag, Provider provider, boolean clientPacket) {
        super.write(compoundTag, provider, clientPacket);
        if (clientPacket) {
            return;
        }

        createConnectionData();
        for (GasPipeConnection connection : connections.values()) {
            connection.write(compoundTag, provider, false);
        }

        writeRetiredConnections(compoundTag, provider);
    }

    @Override
    public void unload() {
        Level level = getWorld();
        if (level != null) {
            GasNetworkTopology.invalidate(level, getPos());
        }

        super.unload();
    }

    public void finalizePendingTransfersBeforeBlockRemoval() {
        Level level = getWorld();
        if (level == null || level.isClientSide) {
            return;
        }

        createConnectionData();
        BlockPos pos = getPos();
        for (GasPipeConnection connection : connections.values()) {
            finalizePendingTransfer(level, pos, connection);
        }

        if (retiredConnections == null) {
            return;
        }

        for (GasPipeConnection connection : retiredConnections) {
            finalizePendingTransfer(level, pos, connection);
        }

        retiredConnections = null;
    }

    public abstract boolean canConnectOnFace(BlockState state, Direction direction);

    public abstract boolean isConnectionFaceEnabled(BlockState state, Direction direction);

    public boolean canObservePressureOnFace(BlockState state, Direction direction) {
        return canConnectOnFace(state, direction);
    }

    public boolean allowsInboundFlow(BlockState state, Direction direction) {
        return canConnectOnFace(state, direction);
    }

    public boolean acceptsInboundGas(BlockState state, Direction direction, GasStack gasStack) {
        return allowsInboundFlow(state, direction) && acceptsGas(gasStack, state, direction);
    }

    public boolean allowsOutboundFlow(BlockState state, Direction direction) {
        return canConnectOnFace(state, direction);
    }

    public boolean allowsOutboundGas(BlockState state, Direction direction, GasStack gasStack) {
        return allowsOutboundFlow(state, direction) && acceptsGas(gasStack, state, direction);
    }

    public GasTransportEdgeProperties getTransportEdgeProperties(BlockState state, Direction entryFace, Direction exitFace, GasStack gasStack) {
        return GasTransportEdgeProperties.passive(getFlowResistanceUnits(state, entryFace, exitFace));
    }

    public long getManifoldSpokeResistanceUnits(BlockState state, Direction face) {
        long segmentResistance = Math.max(GasFlowResistance.MIN_RESISTANCE_UNITS, getFlowResistanceUnits(state, face, face.getOpposite()));
        long halfResistance = segmentResistance / 2 + segmentResistance % 2;
        return Math.max(GasFlowResistance.MIN_RESISTANCE_UNITS, halfResistance);
    }

    @Nullable
    public GasPipeConnection getConnection(Direction side) {
        if (connectionsDirty) {
            refreshConnections();
        }
        else {
            createConnectionData();
        }

        return connections.get(side);
    }

    @Nullable
    public FlowState getFlowState(Direction side) {
        GasPipeConnection connection = getConnection(side);
        if (connection == null) {
            return null;
        }

        return connection.getFlowState();
    }

    public long getThroughputFlowRate() {
        long inboundFlowRate = 0;
        long outboundFlowRate = 0;
        for (Direction side : Iterate.directions) {
            FlowState flowState = getFlowState(side);
            if (flowState == null) {
                continue;
            }

            if (flowState.direction() == FlowDirection.INBOUND) {
                inboundFlowRate = BoundedMath.saturatedAdd(inboundFlowRate, flowState.flowRate());
                continue;
            }

            outboundFlowRate = BoundedMath.saturatedAdd(outboundFlowRate, flowState.flowRate());
        }

        return Math.max(inboundFlowRate, outboundFlowRate);
    }

    public boolean publishFlowTelemetry() {
        if (!(blockEntity instanceof GasFlowTelemetryTarget telemetryTarget)) {
            return false;
        }

        telemetryTarget.acceptFlowTelemetry(getThroughputFlowRate());
        return true;
    }

    public void markConnectionsDirty() {
        Level level = getWorld();
        if (level != null) {
            GasNetworkTopology.invalidate(level, getPos());
        }

        connectionsDirty = true;
    }

    @SuppressWarnings("unused")
    protected long getFlowResistanceUnits(BlockState state, Direction entryFace, Direction exitFace) {
        return PIPE_RESISTANCE_UNITS;
    }

    protected boolean acceptsGas(GasStack gas, BlockState state, Direction direction) {
        return true;
    }

    private void createConnectionData() {
        if (connections != null) {
            return;
        }

        connections = new EnumMap<>(Direction.class);
        BlockState state = blockEntity.getBlockState();
        Level level = getWorld();
        for (Direction direction : Iterate.directions) {
            boolean canConnect = level == null ? isConnectionFaceEnabled(state, direction) : canConnectOnFace(state, direction);
            if (!canConnect) {
                continue;
            }

            connections.put(direction, new GasPipeConnection(direction));
        }
    }

    private void refreshConnectionData() {
        if (connections == null) {
            createConnectionData();
            return;
        }

        Level level = getWorld();
        if (level == null) {
            return;
        }

        boolean topologyChanged = false;
        boolean flowRemoved = false;
        BlockState state = blockEntity.getBlockState();
        for (Direction direction : Iterate.directions) {
            if (canConnectOnFace(state, direction)) {
                if (!connections.containsKey(direction)) {
                    connections.put(direction, new GasPipeConnection(direction));
                    topologyChanged = true;
                }

                continue;
            }

            GasPipeConnection connection = connections.remove(direction);
            if (connection == null) {
                continue;
            }

            topologyChanged = true;
            flowRemoved |= connection.getFlowState() != null;
            retireConnection(connection);
        }

        if (topologyChanged && !connectionsDirty) {
            GasNetworkTopology.invalidate(level, getPos());
            if (!level.isClientSide) {
                periodicTopologyRefreshPending = true;
            }
        }

        if (!(flowRemoved && !level.isClientSide)) {
            return;
        }

        publishFlowTelemetry();
    }

    private void retireConnection(GasPipeConnection connection) {
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

    private void recoverRetiredConnections() {
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

    private void refreshConnections() {
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

    private void readRetiredConnections(CompoundTag compoundTag, Provider provider) {
        retiredConnections = null;
        if (!compoundTag.contains(COMPOUND_KEY_RETIRED_CONNECTIONS, Tag.TAG_LIST)) {
            return;
        }

        ListTag retiredData = compoundTag.getList(COMPOUND_KEY_RETIRED_CONNECTIONS, Tag.TAG_COMPOUND);
        for (int connectionIndex = 0; connectionIndex < retiredData.size(); connectionIndex++) {
            GasPipeConnection connection = GasPipeConnection.readRetiredData(retiredData.getCompound(connectionIndex), provider);
            if (connection == null) {
                continue;
            }

            if (retiredConnections == null) {
                retiredConnections = new ArrayList<>();
            }

            retiredConnections.add(connection);
        }
    }

    private void writeRetiredConnections(CompoundTag compoundTag, Provider provider) {
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

}
