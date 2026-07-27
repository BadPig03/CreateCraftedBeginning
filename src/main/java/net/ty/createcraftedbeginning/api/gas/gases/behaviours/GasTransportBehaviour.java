package net.ty.createcraftedbeginning.api.gas.gases.behaviours;

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
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.ty.createcraftedbeginning.api.gas.gases.GasCapabilities;
import net.ty.createcraftedbeginning.api.gas.gases.GasPipeConnection;
import net.ty.createcraftedbeginning.api.gas.gases.GasPipeConnection.AirFlow;
import net.ty.createcraftedbeginning.api.gas.gases.GasStack;
import net.ty.createcraftedbeginning.api.gas.gases.collisions.GasCollisionEvent;
import net.ty.createcraftedbeginning.api.gas.gases.interfaces.IAirtightComponent;
import net.ty.createcraftedbeginning.content.airtights.airtightpipe.AirtightPipeAttachmentTypes.AttachmentTypes;
import net.ty.createcraftedbeginning.content.airtights.airtightpipe.AxisGasPipeBlock;
import net.ty.createcraftedbeginning.content.airtights.airtightpump.AirtightPumpBlock;
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

    private EnumMap<Direction, GasPipeConnection> interfaces;
    @Nullable
    private List<GasPipeConnection> retiredConnections;
    private UpdatePhase phase;
    private boolean clientModelRefreshPending;

    /**
     * Creates a new {@code GasTransportBehaviour} instance.
     *
     * @param be the block entity that participates in the operation
     */
    public GasTransportBehaviour(SmartBlockEntity be) {
        super(be);
        phase = UpdatePhase.WAIT_FOR_PUMPS;
        clientModelRefreshPending = true;
    }

    /**
     * Checks whether this value is valid airtight components.
     *
     * @param level     the level in which the operation is performed
     * @param pos       the target block position
     * @param state     the block state to inspect or process
     * @param direction the direction associated with the operation
     * @return {@code true} if this value is valid airtight components; otherwise {@code false}
     */
    public static boolean isValidAirtightComponents(@Nullable Level level, BlockPos pos, BlockState state, Direction direction) {
        if (level == null) {
            return false;
        }

        boolean openEnded = state.getDestroySpeed(level, pos) != -1 && (state.canBeReplaced() || CCBBlockTags.GAS_SOURCES.matches(state));
        boolean hasGasCapability = GasCapabilities.hasGasCapability(level, pos, direction.getOpposite());
        boolean isAirtight = state.getBlock() instanceof IAirtightComponent component && component.isAirtight(pos, state, direction);
        return openEnded || hasGasCapability || isAirtight;
    }

    /**
     * Checks whether the requested operation can have flow toward.
     *
     * @param state     the block state to inspect or process
     * @param direction the direction associated with the operation
     * @return {@code true} if the requested operation can have flow toward; otherwise {@code false}
     */
    public abstract boolean canHaveFlowToward(BlockState state, Direction direction);

    /**
     * Checks whether a connection may exist while the block entity is being
     * deserialized and has not received its level yet.
     * <p>
     * This method must only inspect the block state. Neighbor and capability
     * checks are deferred until {@link #initialize()} or the next tick.
     */
    public abstract boolean canHaveFlowTowardWithoutLevel(BlockState state, Direction direction);

    /**
     * Checks whether inbound flow is allowed.
     *
     * @param state     the block state to inspect or process
     * @param direction the direction associated with the operation
     * @return {@code true} if inbound flow is allowed; otherwise {@code false}
     */
    public boolean allowsInboundFlow(BlockState state, Direction direction) {
        return canHaveFlowToward(state, direction);
    }

    /**
     * Checks whether outbound flow is allowed.
     *
     * @param state     the block state to inspect or process
     * @param direction the direction associated with the operation
     * @return {@code true} if outbound flow is allowed; otherwise {@code false}
     */
    public boolean allowsOutboundFlow(BlockState state, Direction direction) {
        return canHaveFlowToward(state, direction);
    }

    /**
     * Returns the provided outward gas.
     *
     * @param side the side from which the target is accessed
     * @return the provided outward gas
     */
    public GasStack getProvidedOutwardGas(Direction side) {
        GasPipeConnection connection = getConnection(side);
        if (connection == null || !allowsOutboundFlow(blockEntity.getBlockState(), side)) {
            return GasStack.EMPTY;
        }
        return connection.provideOutboundFlow();
    }

    private void refreshClientModelIfNeeded(@Nullable Level level) {
        if (!clientModelRefreshPending || level == null) {
            return;
        }

        clientModelRefreshPending = false;
        if (!level.isClientSide || blockEntity.isVirtual()) {
            return;
        }

        BlockState state = blockEntity.getBlockState();
        Block block = state.getBlock();
        if (!(block instanceof AxisGasPipeBlock) && !(block instanceof AirtightPumpBlock)) {
            return;
        }

        blockEntity.requestModelDataUpdate();
        level.sendBlockUpdated(getPos(), state, state, Block.UPDATE_CLIENTS);
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
    }

    /**
     * Returns the connection.
     *
     * @param side the side from which the target is accessed
     * @return the connection
     */
    @Nullable
    public GasPipeConnection getConnection(Direction side) {
        createConnectionData();
        return interfaces.get(side);
    }

    /**
     * Checks whether at least one connection has pressure.
     *
     * @return {@code true} if at least one connection has pressure; otherwise {@code false}
     */
    public boolean hasAnyPressure() {
        createConnectionData();
        for (GasPipeConnection pipeConnection : interfaces.values()) {
            if (!pipeConnection.hasPressure()) {
                continue;
            }

            return true;
        }
        return false;
    }

    /**
     * Returns the flow.
     *
     * @param side the side from which the target is accessed
     * @return the flow
     */
    @Nullable
    public AirFlow getFlow(Direction side) {
        GasPipeConnection connection = getConnection(side);
        return connection == null ? null : connection.getFlow();
    }

    /**
     * Adds the supplied pressure.
     *
     * @param side     the side from which the target is accessed
     * @param inbound  whether inbound is enabled
     * @param pressure the pressure value to use
     */
    public void addPressure(Direction side, boolean inbound, float pressure) {
        GasPipeConnection connection = getConnection(side);
        BlockState state = blockEntity.getBlockState();
        boolean flowAllowed = inbound ? allowsInboundFlow(state, side) : allowsOutboundFlow(state, side);
        if (connection == null || !flowAllowed) {
            return;
        }

        connection.addPressure(inbound, pressure);
    }

    /**
     * Clears the pressure stored by this connection.
     */
    public void wipePressure() {
        refreshConnectionData();
        phase = UpdatePhase.WAIT_FOR_PUMPS;
        for (GasPipeConnection connection : interfaces.values()) {
            connection.wipePressure();
        }
    }

    /**
     * Checks whether this value is incorrect axis.
     *
     * @param state     the block state to inspect or process
     * @param direction the direction associated with the operation
     * @return {@code true} if this value is incorrect axis; otherwise {@code false}
     */
    public boolean isIncorrectAxis(BlockState state, Direction direction) {
        return state.getValue(BlockStateProperties.AXIS) != direction.getAxis();
    }

    /**
     * Returns the rendered rim attachment.
     *
     * @param level     the level in which the operation is performed
     * @param pos       the target block position
     * @param state     the block state to inspect or process
     * @param direction the direction associated with the operation
     * @return the rendered rim attachment
     */
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

    /**
     * {@inheritDoc}
     */
    @Override
    public BehaviourType<?> getType() {
        return TYPE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void initialize() {
        super.initialize();
        refreshConnections();
        clientModelRefreshPending = true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void tick() {
        Level level = getWorld();
        if (level == null) {
            return;
        }

        super.tick();
        BlockPos pos = getPos();
        boolean isClientSide = level.isClientSide && !blockEntity.isVirtual();
        refreshConnections();
        Collection<GasPipeConnection> connections = interfaces.values();
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

    /**
     * {@inheritDoc}
     */
    @Override
    public void read(CompoundTag compoundTag, Provider provider, boolean clientPacket) {
        super.read(compoundTag, provider, clientPacket);
        refreshConnectionData();
        if (!clientPacket) {
            phase = UpdatePhase.WAIT_FOR_PUMPS;
        }
        for (GasPipeConnection connection : interfaces.values()) {
            connection.read(compoundTag, provider, blockEntity.getBlockPos(), clientPacket);
        }
        if (clientPacket) {
            clientModelRefreshPending = true;
        }
    }

    /**
     * {@inheritDoc}
     */
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
        if (sendUpdate) {
            blockEntity.notifyUpdate();
        }
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
        if (sendUpdate) {
            blockEntity.notifyUpdate();
        }
        return false;
    }

    /**
     * Checks whether the requested operation can pull gas from.
     *
     * @param gas       the gas to inspect or process
     * @param state     the block state to inspect or process
     * @param direction the direction associated with the operation
     * @return {@code true} if the requested operation can pull gas from; otherwise {@code false}
     */
    public boolean canPullGasFrom(GasStack gas, BlockState state, Direction direction) {
        return true;
    }

    private enum UpdatePhase {
        WAIT_FOR_PUMPS,
        FLIP_FLOWS,
        IDLE
    }
}
