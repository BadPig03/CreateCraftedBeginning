package net.ty.createcraftedbeginning.content.airtights.gas.transport;

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
import net.ty.createcraftedbeginning.api.gas.gases.GasAction;
import net.ty.createcraftedbeginning.api.gas.gases.GasStack;
import net.ty.createcraftedbeginning.api.gas.gases.interfaces.IGasHandler;
import net.ty.createcraftedbeginning.content.airtights.gas.flowsources.AdjacentPipeSource;
import net.ty.createcraftedbeginning.content.airtights.gas.flowsources.BlockedSource;
import net.ty.createcraftedbeginning.content.airtights.gas.flowsources.ExternalHandlerSource;
import net.ty.createcraftedbeginning.content.airtights.gas.flowsources.GasFlowSource;
import net.ty.createcraftedbeginning.content.airtights.gas.flowsources.OpenEndedSource;
import net.ty.createcraftedbeginning.content.airtights.gas.interfaces.IVentingGasSource;
import net.ty.createcraftedbeginning.content.airtights.gas.transport.GasPropagator.AdjacentTarget;
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
    private static final String COMPOUND_KEY_PENDING_TRANSFER = "PendingTransfer";
    private static final String COMPOUND_KEY_SIDE = "Side";

    private final Direction side;
    private long inboundPressureUnits;
    private long outwardPressureUnits;
    private boolean pressureContributed;
    private GasStack pendingTransfer;

    @Nullable
    private GasFlowSource source;
    @Nullable
    private AirFlow flow;
    @Nullable
    private GasNetwork network;
    @Nullable
    private GasFlowSource previousSource;

    public GasPipeConnection(Direction side) {
        this.side = side;
        pendingTransfer = GasStack.EMPTY;
    }

    @Nullable
    public static GasPipeConnection readRetiredData(CompoundTag retiredData, Provider provider) {
        if (!retiredData.contains(COMPOUND_KEY_SIDE, Tag.TAG_INT) || !retiredData.contains(COMPOUND_KEY_PENDING_TRANSFER, Tag.TAG_COMPOUND)) {
            return null;
        }

        GasStack pending = GasStack.parseOptional(provider, retiredData.getCompound(COMPOUND_KEY_PENDING_TRANSFER));
        if (pending.isEmpty()) {
            return null;
        }

        int sideId = retiredData.getInt(COMPOUND_KEY_SIDE);
        Direction retiredSide = null;
        for (Direction direction : Direction.values()) {
            if (direction.get3DDataValue() != sideId) {
                continue;
            }

            retiredSide = direction;
            break;
        }
        if (retiredSide == null) {
            return null;
        }

        GasPipeConnection connection = new GasPipeConnection(retiredSide);
        connection.pendingTransfer = pending;
        return connection;
    }

    public Direction getSide() {
        return side;
    }

    @Nullable
    public AirFlow getFlow() {
        return flow;
    }

    @Nullable
    public GasFlowSource getSource() {
        return source;
    }

    public GasStack getProvidedGas() {
        if (!hasPressure() || flow == null || !flow.inbound) {
            return GasStack.EMPTY;
        }
        return flow.gas;
    }

    public boolean flipFlowsIfPressureReversed() {
        if (flow == null) {
            return false;
        }

        int pressureDirection = getPressureDirection();
        boolean singlePressure = pressureDirection != 0 && (inboundPressureUnits == 0 || outwardPressureUnits == 0);
        if (!singlePressure || pressureDirection < 0 == flow.inbound) {
            return false;
        }

        flow.inbound = !flow.inbound;
        return true;
    }

    @SuppressWarnings("unused")
    private float comparePressure() {
        return GasPressure.toPressure(outwardPressureUnits) - GasPressure.toPressure(inboundPressureUnits);
    }

    public int getPressureDirection() {
        return Long.compare(outwardPressureUnits, inboundPressureUnits);
    }

    @SuppressWarnings("unused")
    private float getOutwardPressure() {
        return GasPressure.toPressure(outwardPressureUnits);
    }

    public long getOutwardPressureUnits() {
        return outwardPressureUnits;
    }

    @SuppressWarnings("unused")
    private float getInboundPressure() {
        return GasPressure.toPressure(inboundPressureUnits);
    }

    public long getInboundPressureUnits() {
        return inboundPressureUnits;
    }

    public void setPumpPressure(boolean inbound, float amount) {
        long units = GasPressure.toUnits(amount);
        pressureContributed = units > 0;
        inboundPressureUnits = inbound ? units : 0;
        outwardPressureUnits = inbound ? 0 : units;
    }

    public void manageSource(Level level, BlockPos pos, BlockEntity blockEntity) {
        if (source == null && !determineSource(level, pos)) {
            return;
        }

        source.manageSource(level, blockEntity);
    }

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

        if (flow.inbound != getPressureDirection() < 0) {
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
            network = new GasNetwork(level, new BlockFace(pos, side), this::getCurrentGasHandlerProvider, pendingTransfer, this::setPendingTransfer);
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

        boolean prioritizeInbound = getPressureDirection() < 0;
        for (boolean matchesPriority : Iterate.trueAndFalse) {
            boolean inbound = prioritizeInbound == matchesPriority;
            long pressureUnits = inbound ? inboundPressureUnits : outwardPressureUnits;
            if (pressureUnits == 0) {
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

    public boolean hasPressure() {
        return getPressureDirection() != 0;
    }

    public boolean hasPressureContribution() {
        return pressureContributed;
    }

    public void tickFlowProgress(Level level, BlockPos pos) {
        if (flow == null || flow.gas.isEmpty() || !level.isClientSide || source != null) {
            return;
        }

        determineSource(level, pos);
    }

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
        if (openEndedSource != null) {
            CompoundTag openEndData = openEndedSource.write(provider);
            if (!openEndData.isEmpty()) {
                connectionData.put(COMPOUND_KEY_OPEN_END, openEndData);
            }
        }

        writePendingTransferData(connectionData, provider);
    }

    private void writePendingTransferData(CompoundTag connectionData, Provider provider) {
        if (pendingTransfer.isEmpty()) {
            return;
        }

        connectionData.put(COMPOUND_KEY_PENDING_TRANSFER, pendingTransfer.saveOptional(provider));
    }

    public CompoundTag writeRetiredData(Provider provider) {
        if (pendingTransfer.isEmpty()) {
            return new CompoundTag();
        }

        CompoundTag retiredData = new CompoundTag();
        retiredData.putInt(COMPOUND_KEY_SIDE, side.get3DDataValue());
        writePendingTransferData(retiredData, provider);
        return retiredData;
    }

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
        inboundPressureUnits = 0;
        outwardPressureUnits = 0;
        pressureContributed = false;
        flow = null;
        network = null;
        source = null;
        previousSource = null;
        pendingTransfer = connectionData.contains(COMPOUND_KEY_PENDING_TRANSFER, Tag.TAG_COMPOUND) ? GasStack.parseOptional(provider, connectionData.getCompound(COMPOUND_KEY_PENDING_TRANSFER)) : GasStack.EMPTY;
        if (!connectionData.contains(COMPOUND_KEY_OPEN_END, Tag.TAG_COMPOUND)) {
            return;
        }

        OpenEndedSource openEndedSource = OpenEndedSource.read(connectionData.getCompound(COMPOUND_KEY_OPEN_END), provider, new BlockFace(blockPos, side));
        source = openEndedSource;
        previousSource = openEndedSource;
    }

    public boolean prepareForRemoval(Level level, BlockPos pos) {
        inboundPressureUnits = 0;
        outwardPressureUnits = 0;
        pressureContributed = false;
        flow = null;
        retireNetwork();
        if (network != null || !recoverPendingTransferWithoutNetwork(level, pos)) {
            return false;
        }

        source = null;
        previousSource = null;
        return true;
    }

    private boolean recoverPendingTransferWithoutNetwork(Level level, BlockPos pos) {
        if (pendingTransfer.isEmpty()) {
            return true;
        }

        if (source == null && !determineSource(level, pos)) {
            return false;
        }
        GasFlowSource currentSource = source;
        if (currentSource == null || !currentSource.isEndpoint()) {
            return false;
        }

        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity == null) {
            return false;
        }
        currentSource.manageSource(level, blockEntity);

        ICapabilityProvider<IGasHandler> sourceProvider = currentSource.getGasHandlerProvider();
        if (sourceProvider == null) {
            return false;
        }

        IGasHandler sourceCap = sourceProvider.getCapability();
        if (sourceCap == null) {
            return false;
        }

        if (sourceCap instanceof IVentingGasSource) {
            setPendingTransfer(GasStack.EMPTY);
            blockEntity.setChanged();
            return true;
        }

        long returned = sourceCap.fill(pendingTransfer.copy(), GasAction.EXECUTE);
        returned = Math.clamp(returned, 0, pendingTransfer.getAmount());
        if (returned <= 0) {
            return false;
        }

        GasStack remainder = pendingTransfer.copy();
        remainder.shrink(returned);
        setPendingTransfer(remainder);
        blockEntity.setChanged();
        return pendingTransfer.isEmpty();
    }

    private void setPendingTransfer(GasStack stack) {
        pendingTransfer = stack.isEmpty() ? GasStack.EMPTY : stack.copy();
    }

    public void wipePressure() {
        inboundPressureUnits = 0;
        outwardPressureUnits = 0;
        pressureContributed = false;
        if (source != null) {
            previousSource = source;
        }
        source = null;
        resetNetwork();
    }

    public void resetNetwork() {
        if (network == null) {
            return;
        }

        network.reset();
    }

    public GasStack provideOutboundFlow() {
        if (!hasPressure() || flow == null || flow.inbound) {
            return GasStack.EMPTY;
        }
        return flow.gas;
    }

    public void addPressureUnits(boolean inbound, long newAmountUnits) {
        if (newAmountUnits <= 0) {
            return;
        }

        pressureContributed = true;
        if (inbound) {
            inboundPressureUnits = GasPressure.addSaturated(inboundPressureUnits, newAmountUnits);
        }
        else {
            outwardPressureUnits = GasPressure.addSaturated(outwardPressureUnits, newAmountUnits);
        }
        normalizeOpposingPressure();
    }

    private void normalizeOpposingPressure() {
        long cancellation = Math.min(inboundPressureUnits, outwardPressureUnits);
        inboundPressureUnits -= cancellation;
        outwardPressureUnits -= cancellation;
        if (GasPressure.isZero(inboundPressureUnits)) {
            inboundPressureUnits = 0;
        }
        if (GasPressure.isZero(outwardPressureUnits)) {
            outwardPressureUnits = 0;
        }
    }

    public static final class AirFlow {
        public boolean inbound;
        public GasStack gas;

        private AirFlow(boolean inbound, GasStack gas) {
            this.inbound = inbound;
            this.gas = gas;
        }
    }
}
