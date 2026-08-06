package net.ty.createcraftedbeginning.api.gas.gases;

import com.simibubi.create.foundation.ICapabilityProvider;
import net.createmod.catnip.data.Iterate;
import net.createmod.catnip.math.BlockFace;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.ty.createcraftedbeginning.api.gas.gases.GasPropagator.AdjacentTarget;
import net.ty.createcraftedbeginning.api.gas.gases.flowsources.AdjacentPipeSource;
import net.ty.createcraftedbeginning.api.gas.gases.flowsources.BlockedSource;
import net.ty.createcraftedbeginning.api.gas.gases.flowsources.ExternalHandlerSource;
import net.ty.createcraftedbeginning.api.gas.gases.flowsources.GasFlowSource;
import net.ty.createcraftedbeginning.api.gas.gases.flowsources.OpenEndedSource;
import net.ty.createcraftedbeginning.api.gas.gases.interfaces.IGasHandler;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.function.Predicate;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class GasPipeConnection {
    private static final String COMPOUND_KEY_OPEN_END = "OpenEnd";
    private static final String COMPOUND_KEY_AIR_FLOW = "AirFlow";
    private static final String COMPOUND_KEY_INBOUND = "Inbound";
    private static final String COMPOUND_KEY_GAS = "Gas";

    private final Direction side;
    private float inboundPressure;
    private float outwardPressure;
    private boolean pressureContributed;

    @Nullable
    private GasFlowSource source;
    @Nullable
    private AirFlow flow;
    @Nullable
    private GasNetwork network;
    @Nullable
    private GasFlowSource previousSource;

    /**
     * Creates a new {@code GasPipeConnection} instance.
     *
     * @param side the side from which the target is accessed
     */
    public GasPipeConnection(Direction side) {
        this.side = side;
    }

    /**
     * Returns the side.
     *
     * @return the side
     */
    public Direction getSide() {
        return side;
    }

    /**
     * Returns the flow.
     *
     * @return the flow
     */
    @Nullable
    public AirFlow getFlow() {
        return flow;
    }

    /**
     * Returns the source.
     *
     * @return the source
     */
    @Nullable
    public GasFlowSource getSource() {
        return source;
    }

    /**
     * Returns the provided gas.
     *
     * @return the provided gas
     */
    public GasStack getProvidedGas() {
        if (!hasPressure() || flow == null || !flow.inbound) {
            return GasStack.EMPTY;
        }
        return flow.gas;
    }

    /**
     * Computes and returns the flip flows if pressure reversed result.
     *
     * @return {@code true} if the condition is satisfied; otherwise {@code false}
     */
    public boolean flipFlowsIfPressureReversed() {
        if (flow == null) {
            return false;
        }

        boolean singlePressure = comparePressure() != 0 && (inboundPressure == 0 || outwardPressure == 0);
        if (!singlePressure || comparePressure() < 0 == flow.inbound) {
            return false;
        }

        flow.inbound = !flow.inbound;
        return true;
    }

    /**
     * Compares inbound and outward pressure for this connection.
     *
     * @return the compare pressure value
     */
    public float comparePressure() {
        return outwardPressure - inboundPressure;
    }

    /**
     * Returns the outward pressure.
     *
     * @return the outward pressure
     */
    public float getOutwardPressure() {
        return outwardPressure;
    }

    /**
     * Returns the inbound pressure.
     *
     * @return the inbound pressure
     */
    public float getInboundPressure() {
        return inboundPressure;
    }

    /**
     * Sets the pump pressure.
     *
     * @param inbound whether inbound is enabled
     * @param amount  the amount to use
     */
    public void setPumpPressure(boolean inbound, float amount) {
        amount = amount > 0 && Float.isFinite(amount) ? amount : 0;
        pressureContributed = amount > 0;
        inboundPressure = inbound ? amount : 0;
        outwardPressure = inbound ? 0 : amount;
    }

    /**
     * Updates and manages the source.
     *
     * @param level       the level in which the operation is performed
     * @param pos         the target block position
     * @param blockEntity the block entity associated with the operation
     */
    public void manageSource(Level level, BlockPos pos, BlockEntity blockEntity) {
        if (source == null && !determineSource(level, pos)) {
            return;
        }

        source.manageSource(level, blockEntity);
    }

    /**
     * Determines and stores the flow source for this connection.
     *
     * @param level the level in which the operation is performed
     * @param pos   the target block position
     * @return {@code true} if the condition is satisfied; otherwise {@code false}
     */
    public boolean determineSource(Level level, BlockPos pos) {
        BlockPos relativePos = pos.relative(side);
        if (level.getChunk(relativePos.getX() >> 4, relativePos.getZ() >> 4, ChunkStatus.FULL, false) == null) {
            return false;
        }

        BlockFace location = new BlockFace(pos, side);
        AdjacentTarget target = GasPropagator.resolveAdjacentTarget(level, pos, side);
        if (target.isOpenEnded()) {
            source = previousSource instanceof OpenEndedSource ? previousSource : new OpenEndedSource(location);
            return true;
        }

        if (target.hasGasCapability()) {
            source = new ExternalHandlerSource(location);
            return true;
        }

        source = target.behaviour() == null ? new BlockedSource(location) : new AdjacentPipeSource(location);
        return true;
    }

    /**
     * Updates and manages the flows.
     *
     * @param level               the level in which the operation is performed
     * @param pos                 the target block position
     * @param internalGas         the internal gas handler used by the connection
     * @param extractionPredicate the predicate used to test the extraction
     * @return {@code true} if the condition is satisfied; otherwise {@code false}
     */
    public boolean manageFlows(Level level, BlockPos pos, GasStack internalGas, Predicate<GasStack> extractionPredicate) {
        recoverRetiredNetwork();
        if (source == null && !determineSource(level, pos)) {
            retireNetwork();
            return false;
        }

        GasFlowSource flowSource = source;
        if (flow == null) {
            return startFlow(flowSource, internalGas, extractionPredicate);
        }

        GasStack provided = flow.inbound ? flowSource.provideGas(extractionPredicate) : internalGas;
        if (!hasPressure() || provided.isEmpty() || !GasStack.isSameGasSameComponents(provided, flow.gas)) {
            flow = null;
            retireNetwork();
            return true;
        }

        if (flow.inbound != comparePressure() < 0) {
            boolean inbound = !flow.inbound;
            if (inbound && !provided.isEmpty() || !inbound && !internalGas.isEmpty()) {
                GasPropagator.resetAffectedNetworks(level, pos, side);
                retireNetwork();
                tryStartingNewFlow(inbound, inbound ? flowSource.provideGas(extractionPredicate) : internalGas);
                return true;
            }
        }

        if (!flowSource.isEndpoint() || !flow.inbound) {
            retireNetwork();
            return false;
        }

        if (network == null) {
            network = new GasNetwork(level, new BlockFace(pos, side), this::getCurrentGasHandlerProvider);
        }
        else if (!network.isActive()) {
            network.reset();
        }
        network.tick();
        return false;
    }

    private boolean startFlow(GasFlowSource flowSource, GasStack internalGas, Predicate<GasStack> extractionPredicate) {
        retireNetwork();
        if (!hasPressure()) {
            return false;
        }

        boolean prioritizeInbound = comparePressure() < 0;
        for (boolean matchesPriority : Iterate.trueAndFalse) {
            boolean inbound = prioritizeInbound == matchesPriority;
            float pressure = inbound ? inboundPressure : outwardPressure;
            if (pressure == 0) {
                continue;
            }

            GasStack gas = inbound ? flowSource.provideGas(extractionPredicate) : internalGas;
            if (tryStartingNewFlow(inbound, gas)) {
                return true;
            }
        }
        return false;
    }

    private boolean tryStartingNewFlow(boolean inbound, GasStack stack) {
        if (stack.isEmpty()) {
            return false;
        }

        flow = new AirFlow(inbound, stack);
        return true;
    }

    @Nullable
    private ICapabilityProvider<IGasHandler> getCurrentGasHandlerProvider() {
        GasFlowSource currentSource = source;
        if (currentSource == null || !currentSource.isEndpoint()) {
            return null;
        }
        return currentSource.getGasHandlerProvider();
    }

    private void recoverRetiredNetwork() {
        if (network == null || network.isActive() || !network.recoverPendingTransfer()) {
            return;
        }

        network = null;
    }

    private void retireNetwork() {
        if (network == null) {
            return;
        }

        network.stop();
        if (!network.recoverPendingTransfer()) {
            return;
        }

        network = null;
    }

    /**
     * Checks whether this connection has non-zero pressure.
     *
     * @return {@code true} if this connection has non-zero pressure; otherwise {@code false}
     */
    public boolean hasPressure() {
        return Float.compare(inboundPressure, outwardPressure) != 0;
    }

    /**
     * Checks whether this connection received any pressure contribution since
     * the last pressure wipe, even if opposing contributions cancelled out.
     *
     * @return {@code true} if pressure was contributed; otherwise {@code false}
     */
    public boolean hasPressureContribution() {
        return pressureContributed;
    }

    /**
     * Advances the visual flow progress for one game tick.
     *
     * @param level the level in which the operation is performed
     * @param pos   the target block position
     */
    public void tickFlowProgress(Level level, BlockPos pos) {
        if (flow == null || flow.gas.isEmpty() || !level.isClientSide || source != null) {
            return;
        }

        determineSource(level, pos);
    }

    /**
     * Writes this object's state to the supplied serialized data.
     *
     * @param compoundTag  the NBT compound to read from or write to
     * @param provider     the provider used to resolve the requested value
     * @param clientPacket the client synchronization packet
     */
    public void write(CompoundTag compoundTag, Provider provider, boolean clientPacket) {
        CompoundTag connectionData = new CompoundTag();
        if (clientPacket) {
            writeClientData(connectionData, provider);
        }
        else {
            writePersistentData(connectionData, provider);
        }
        if (connectionData.isEmpty()) {
            return;
        }

        compoundTag.put(side.getName(), connectionData);
    }

    private void writeClientData(CompoundTag connectionData, Provider provider) {
        if (flow == null || flow.gas.isEmpty()) {
            return;
        }

        CompoundTag flowData = new CompoundTag();
        flowData.put(COMPOUND_KEY_GAS, flow.gas.saveOptional(provider));
        flowData.putBoolean(COMPOUND_KEY_INBOUND, flow.inbound);
        connectionData.put(COMPOUND_KEY_AIR_FLOW, flowData);
    }

    private void writePersistentData(CompoundTag connectionData, Provider provider) {
        OpenEndedSource openEndedSource = null;
        if (source instanceof OpenEndedSource currentOpenEnd) {
            openEndedSource = currentOpenEnd;
        }
        else if (source == null && previousSource instanceof OpenEndedSource previousOpenEnd) {
            openEndedSource = previousOpenEnd;
        }
        if (openEndedSource == null) {
            return;
        }

        CompoundTag openEndData = openEndedSource.write(provider);
        if (openEndData.isEmpty()) {
            return;
        }

        connectionData.put(COMPOUND_KEY_OPEN_END, openEndData);
    }

    /**
     * Reads this object's state from the supplied serialized data.
     *
     * @param compoundTag  the NBT compound to read from or write to
     * @param provider     the provider used to resolve the requested value
     * @param blockPos     the target block position
     * @param clientPacket the client synchronization packet
     */
    public void read(CompoundTag compoundTag, Provider provider, BlockPos blockPos, boolean clientPacket) {
        CompoundTag connectionData = compoundTag.contains(side.getName(), Tag.TAG_COMPOUND) ? compoundTag.getCompound(side.getName()) : new CompoundTag();
        if (clientPacket) {
            readClientData(connectionData, provider);
            return;
        }

        readPersistentData(connectionData, provider, blockPos);
    }

    private void readClientData(CompoundTag connectionData, Provider provider) {
        source = null;
        if (!connectionData.contains(COMPOUND_KEY_AIR_FLOW, Tag.TAG_COMPOUND)) {
            flow = null;
            return;
        }

        CompoundTag flowData = connectionData.getCompound(COMPOUND_KEY_AIR_FLOW);
        GasStack gas = GasStack.parseOptional(provider, flowData.getCompound(COMPOUND_KEY_GAS));
        if (gas.isEmpty()) {
            flow = null;
            return;
        }

        boolean inbound = flowData.getBoolean(COMPOUND_KEY_INBOUND);
        if (flow == null) {
            flow = new AirFlow(inbound, gas);
            return;
        }

        flow.gas = gas;
        flow.inbound = inbound;
    }

    private void readPersistentData(CompoundTag connectionData, Provider provider, BlockPos blockPos) {
        inboundPressure = 0;
        outwardPressure = 0;
        pressureContributed = false;
        flow = null;
        retireNetwork();
        source = null;
        previousSource = null;
        if (!connectionData.contains(COMPOUND_KEY_OPEN_END, Tag.TAG_COMPOUND)) {
            return;
        }

        OpenEndedSource openEndedSource = OpenEndedSource.read(connectionData.getCompound(COMPOUND_KEY_OPEN_END), provider, new BlockFace(blockPos, side));
        source = openEndedSource;
        previousSource = openEndedSource;
    }

    /**
     * Prepares this object for removal.
     *
     * @return {@code true} if the condition is satisfied; otherwise {@code false}
     */
    public boolean prepareForRemoval() {
        inboundPressure = 0;
        outwardPressure = 0;
        pressureContributed = false;
        flow = null;
        retireNetwork();
        if (network != null) {
            return false;
        }

        source = null;
        previousSource = null;
        return true;
    }

    /**
     * Clears the pressure stored by this connection.
     */
    public void wipePressure() {
        inboundPressure = 0;
        outwardPressure = 0;
        pressureContributed = false;
        if (source != null) {
            previousSource = source;
        }
        source = null;
        resetNetwork();
    }

    /**
     * Resets the network.
     */
    public void resetNetwork() {
        if (network == null) {
            return;
        }

        network.reset();
    }

    /**
     * Provides the outbound flow.
     *
     * @return the resulting gas stack
     */
    public GasStack provideOutboundFlow() {
        if (!hasPressure() || flow == null || flow.inbound) {
            return GasStack.EMPTY;
        }
        return flow.gas;
    }

    /**
     * Adds the supplied pressure.
     *
     * @param inbound   whether inbound is enabled
     * @param newAmount the replacement gas amount
     */
    public void addPressure(boolean inbound, float newAmount) {
        if (newAmount <= 0 || !Float.isFinite(newAmount)) {
            return;
        }

        pressureContributed = true;
        if (inbound) {
            inboundPressure += newAmount;
        }
        else {
            outwardPressure += newAmount;
        }
        normalizeOpposingPressure();
    }

    private void normalizeOpposingPressure() {
        inboundPressure = inboundPressure > 0 && Float.isFinite(inboundPressure) ? inboundPressure : 0;
        outwardPressure = outwardPressure > 0 && Float.isFinite(outwardPressure) ? outwardPressure : 0;
        float cancellation = Math.min(inboundPressure, outwardPressure);
        inboundPressure -= cancellation;
        outwardPressure -= cancellation;
    }

    public static final class AirFlow {
        public boolean inbound;
        public GasStack gas;

        /**
         * Creates a new {@code AirFlow} instance.
         *
         * @param inbound whether inbound is enabled
         * @param gas     the gas to inspect or process
         */
        public AirFlow(boolean inbound, GasStack gas) {
            this.inbound = inbound;
            this.gas = gas;
        }
    }
}
