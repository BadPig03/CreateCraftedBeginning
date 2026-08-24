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
    private static final String COMPOUND_KEY_PENDING_TRANSFER_ORIGIN = "PendingTransferOrigin";
    private static final String COMPOUND_KEY_SIDE = "Side";
    private static final int RETIRED_RECOVERY_INITIAL_DELAY_TICKS = 20;
    private static final int RETIRED_RECOVERY_MAX_DELAY_TICKS = 640;

    private final Direction side;
    private long inboundPressureUnits;
    private long outwardPressureUnits;
    private boolean pressureContributed;
    private GasStack pendingTransfer;
    private PendingTransferOrigin pendingTransferOrigin;
    private int retiredRecoveryDelayTicks;
    private int retiredRecoveryCooldownTicks;

    @Nullable
    private GasFlowSource source;
    @Nullable
    private GasFlow flow;
    @Nullable
    private GasNetwork network;
    @Nullable
    private GasFlowSource previousSource;

    public GasPipeConnection(Direction side) {
        this.side = side;
        pendingTransfer = GasStack.EMPTY;
        pendingTransferOrigin = PendingTransferOrigin.UNSPECIFIED;
        retiredRecoveryDelayTicks = RETIRED_RECOVERY_INITIAL_DELAY_TICKS;
    }

    @Nullable
    public static GasPipeConnection readRetiredData(CompoundTag retiredData, Provider provider) {
        if (!retiredData.contains(COMPOUND_KEY_SIDE, Tag.TAG_INT) || !retiredData.contains(COMPOUND_KEY_PENDING_TRANSFER, Tag.TAG_COMPOUND)) {
            return null;
        }

        GasStack retiredTransfer = GasStack.parseOptional(provider, retiredData.getCompound(COMPOUND_KEY_PENDING_TRANSFER));
        if (retiredTransfer.isEmpty()) {
            return null;
        }

        int retiredSideId = retiredData.getInt(COMPOUND_KEY_SIDE);
        Direction retiredSide = null;
        for (Direction direction : Direction.values()) {
            if (direction.get3DDataValue() != retiredSideId) {
                continue;
            }

            retiredSide = direction;
            break;
        }
        if (retiredSide == null) {
            return null;
        }

        GasPipeConnection retiredConnection = new GasPipeConnection(retiredSide);
        retiredConnection.pendingTransfer = retiredTransfer;
        retiredConnection.pendingTransferOrigin = readPendingTransferOrigin(retiredData);
        retiredConnection.beginRetiredRecoveryBackoff();
        return retiredConnection;
    }

    private static PendingTransferOrigin readPendingTransferOrigin(CompoundTag connectionData) {
        if (!connectionData.contains(COMPOUND_KEY_PENDING_TRANSFER_ORIGIN, Tag.TAG_STRING)) {
            return PendingTransferOrigin.ANY_ENDPOINT;
        }

        return PendingTransferOrigin.fromSerializedName(connectionData.getString(COMPOUND_KEY_PENDING_TRANSFER_ORIGIN));
    }

    public Direction getSide() {
        return side;
    }

    @Nullable
    public GasFlow getFlow() {
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
        boolean hasSinglePressureDirection = pressureDirection != 0 && (inboundPressureUnits == 0 || outwardPressureUnits == 0);
        if (!hasSinglePressureDirection || pressureDirection < 0 == flow.inbound) {
            return false;
        }

        flow.inbound = !flow.inbound;
        return true;
    }

    public int getPressureDirection() {
        return Long.compare(outwardPressureUnits, inboundPressureUnits);
    }

    public long getOutwardPressureUnits() {
        return outwardPressureUnits;
    }

    public long getInboundPressureUnits() {
        return inboundPressureUnits;
    }

    public void setPumpPressure(boolean inbound, float amount) {
        long pressureUnits = GasPressure.toUnits(amount);
        pressureContributed = pressureUnits > 0;
        inboundPressureUnits = inbound ? pressureUnits : 0;
        outwardPressureUnits = inbound ? 0 : pressureUnits;
    }

    public void manageSource(Level level, BlockPos pos, BlockEntity blockEntity) {
        if (source == null && !determineSource(level, pos)) {
            return;
        }

        source.manageSource(level, blockEntity);
    }

    public boolean determineSource(Level level, BlockPos pos) {
        BlockPos adjacentPos = pos.relative(side);
        if (level.getChunk(adjacentPos.getX() >> 4, adjacentPos.getZ() >> 4, ChunkStatus.FULL, false) == null) {
            return false;
        }

        BlockFace sourceLocation = new BlockFace(pos, side);
        AdjacentTarget adjacentTarget = GasPropagator.resolveAdjacentTarget(level, pos, side);
        if (adjacentTarget.isOpenEnded()) {
            source = previousSource instanceof OpenEndedSource ? previousSource : new OpenEndedSource(sourceLocation);
            return true;
        }

        if (adjacentTarget.hasGasCapability()) {
            source = new ExternalHandlerSource(sourceLocation);
            return true;
        }

        source = adjacentTarget.behaviour() == null ? new BlockedSource(sourceLocation) : new AdjacentPipeSource(sourceLocation);
        return true;
    }

    public boolean validateExistingInboundFlow(Level level, BlockPos pos, Predicate<GasStack> extractionPredicate) {
        if (flow == null || !flow.inbound) {
            return false;
        }

        if (source == null && !determineSource(level, pos)) {
            flow = null;
            retireNetwork();
            return true;
        }

        GasFlowSource currentSource = source;
        GasStack providedGas = currentSource.provideGas(extractionPredicate);
        if (!hasPressure() || getPressureDirection() >= 0 || providedGas.isEmpty() || !GasStack.isSameGasSameComponents(providedGas, flow.gas)) {
            flow = null;
            retireNetwork();
            return true;
        }
        return false;
    }

    public boolean manageFlows(Level level, BlockPos pos, GasStack internalGas, Predicate<GasStack> extractionPredicate) {
        recoverRetiredNetwork();
        if (source == null && !determineSource(level, pos)) {
            retireNetwork();
            return false;
        }

        GasFlowSource currentSource = source;
        if (flow == null) {
            return startFlow(currentSource, internalGas, extractionPredicate);
        }

        GasStack flowGas = flow.inbound ? flow.gas : internalGas;
        if (!hasPressure() || flowGas.isEmpty() || !GasStack.isSameGasSameComponents(flowGas, flow.gas)) {
            flow = null;
            retireNetwork();
            return true;
        }

        if (flow.inbound != getPressureDirection() < 0) {
            boolean newFlowInbound = !flow.inbound;
            if (newFlowInbound && !flowGas.isEmpty() || !newFlowInbound && !internalGas.isEmpty()) {
                GasPropagator.resetAffectedNetworks(level, pos, side);
                retireNetwork();
                tryStartingNewFlow(newFlowInbound, newFlowInbound ? currentSource.provideGas(extractionPredicate) : internalGas);
                return true;
            }
        }

        if (!currentSource.isEndpoint() || !flow.inbound) {
            retireNetwork();
            return false;
        }

        if (network == null) {
            network = new GasNetwork(level, new BlockFace(pos, side), this::getCurrentGasHandlerProvider, pendingTransfer, this::setPendingTransfer);
        }
        else if (!network.isActive()) {
            network.reset();
        }
        return false;
    }

    public void tickNetwork() {
        if (network == null || !network.isActive()) {
            return;
        }

        network.tick();
    }

    private boolean startFlow(GasFlowSource flowSource, GasStack internalGas, Predicate<GasStack> extractionPredicate) {
        retireNetwork();
        if (!hasPressure()) {
            return false;
        }

        boolean prioritizeInbound = getPressureDirection() < 0;
        for (boolean matchesPriority : Iterate.trueAndFalse) {
            boolean isInbound = prioritizeInbound == matchesPriority;
            long pressureUnits = isInbound ? inboundPressureUnits : outwardPressureUnits;
            if (pressureUnits == 0) {
                continue;
            }

            GasStack candidateGas = isInbound ? flowSource.provideGas(extractionPredicate) : internalGas;
            if (tryStartingNewFlow(isInbound, candidateGas)) {
                return true;
            }
        }
        return false;
    }

    private boolean tryStartingNewFlow(boolean isInbound, GasStack flowGas) {
        if (flowGas.isEmpty()) {
            return false;
        }

        flow = new GasFlow(isInbound, flowGas);
        return true;
    }

    @Nullable
    private ICapabilityProvider<IGasHandler> getCurrentGasHandlerProvider() {
        GasFlowSource currentSource = source;
        if (currentSource == null || !currentSource.isEndpoint() || !pendingTransfer.isEmpty() && !pendingTransferOrigin.accepts(currentSource)) {
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

        CompoundTag flowTag = new CompoundTag();
        flowTag.put(COMPOUND_KEY_GAS, flow.gas.saveOptional(provider));
        flowTag.putBoolean(COMPOUND_KEY_INBOUND, flow.inbound);
        connectionData.put(COMPOUND_KEY_AIR_FLOW, flowTag);
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
            CompoundTag openEndTag = openEndedSource.write(provider);
            if (!openEndTag.isEmpty()) {
                connectionData.put(COMPOUND_KEY_OPEN_END, openEndTag);
            }
        }

        writePendingTransferData(connectionData, provider);
    }

    private void writePendingTransferData(CompoundTag connectionData, Provider provider) {
        if (pendingTransfer.isEmpty()) {
            return;
        }

        capturePendingTransferOrigin();
        connectionData.put(COMPOUND_KEY_PENDING_TRANSFER, pendingTransfer.saveOptional(provider));
        if (pendingTransferOrigin != PendingTransferOrigin.UNSPECIFIED && pendingTransferOrigin != PendingTransferOrigin.ANY_ENDPOINT) {
            connectionData.putString(COMPOUND_KEY_PENDING_TRANSFER_ORIGIN, pendingTransferOrigin.serializedName);
        }
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

        CompoundTag flowTag = connectionData.getCompound(COMPOUND_KEY_AIR_FLOW);
        GasStack flowGas = GasStack.parseOptional(provider, flowTag.getCompound(COMPOUND_KEY_GAS));
        if (flowGas.isEmpty()) {
            flow = null;
            return;
        }

        boolean isInbound = flowTag.getBoolean(COMPOUND_KEY_INBOUND);
        if (flow == null) {
            flow = new GasFlow(isInbound, flowGas);
            return;
        }

        flow.gas = flowGas;
        flow.inbound = isInbound;
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
        pendingTransferOrigin = pendingTransfer.isEmpty() ? PendingTransferOrigin.UNSPECIFIED : readPendingTransferOrigin(connectionData);
        if (!connectionData.contains(COMPOUND_KEY_OPEN_END, Tag.TAG_COMPOUND)) {
            return;
        }

        OpenEndedSource openEndedSource = OpenEndedSource.read(connectionData.getCompound(COMPOUND_KEY_OPEN_END), provider, new BlockFace(blockPos, side));
        source = openEndedSource;
        previousSource = openEndedSource;
    }

    public boolean prepareForRemoval(Level level, BlockPos pos) {
        capturePendingTransferOrigin();
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

    public void beginRetiredRecoveryBackoff() {
        retiredRecoveryDelayTicks = RETIRED_RECOVERY_INITIAL_DELAY_TICKS;
        retiredRecoveryCooldownTicks = RETIRED_RECOVERY_INITIAL_DELAY_TICKS;
    }

    public boolean tryRecoverRetired(Level level, BlockPos pos) {
        if (retiredRecoveryCooldownTicks > 0) {
            retiredRecoveryCooldownTicks--;
            return false;
        }

        if (network == null) {
            source = null;
        }

        if (prepareForRemoval(level, pos)) {
            return true;
        }

        retiredRecoveryDelayTicks = Math.min(retiredRecoveryDelayTicks * 2, RETIRED_RECOVERY_MAX_DELAY_TICKS);
        retiredRecoveryCooldownTicks = retiredRecoveryDelayTicks;
        return false;
    }

    private boolean recoverPendingTransferWithoutNetwork(Level level, BlockPos pos) {
        if (pendingTransfer.isEmpty()) {
            return true;
        }

        if (source == null && !determineSource(level, pos)) {
            return false;
        }
        GasFlowSource currentSource = source;
        if (currentSource == null || !currentSource.isEndpoint() || !pendingTransferOrigin.accepts(currentSource)) {
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

        IGasHandler sourceHandler = sourceProvider.getCapability();
        if (sourceHandler == null) {
            return false;
        }

        if (sourceHandler instanceof IVentingGasSource) {
            setPendingTransfer(GasStack.EMPTY);
            blockEntity.setChanged();
            return true;
        }

        long returnedAmount = sourceHandler.fill(pendingTransfer.copy(), GasAction.EXECUTE);
        returnedAmount = Math.clamp(returnedAmount, 0, pendingTransfer.getAmount());
        if (returnedAmount <= 0) {
            return false;
        }

        GasStack remainingTransfer = pendingTransfer.copy();
        remainingTransfer.shrink(returnedAmount);
        setPendingTransfer(remainingTransfer);
        blockEntity.setChanged();
        return pendingTransfer.isEmpty();
    }

    private void setPendingTransfer(GasStack transfer) {
        pendingTransfer = transfer.isEmpty() ? GasStack.EMPTY : transfer.copy();
        if (pendingTransfer.isEmpty()) {
            pendingTransferOrigin = PendingTransferOrigin.UNSPECIFIED;
            return;
        }
        capturePendingTransferOrigin();
    }

    private void capturePendingTransferOrigin() {
        if (pendingTransfer.isEmpty() || pendingTransferOrigin != PendingTransferOrigin.UNSPECIFIED) {
            return;
        }

        GasFlowSource recoverySource = source != null ? source : previousSource;
        if (recoverySource instanceof OpenEndedSource) {
            pendingTransferOrigin = PendingTransferOrigin.OPEN_END;
        }
        else if (recoverySource instanceof ExternalHandlerSource) {
            pendingTransferOrigin = PendingTransferOrigin.EXTERNAL;
        }
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
        long canceledPressureUnits = Math.min(inboundPressureUnits, outwardPressureUnits);
        inboundPressureUnits -= canceledPressureUnits;
        outwardPressureUnits -= canceledPressureUnits;
        if (GasPressure.isZero(inboundPressureUnits)) {
            inboundPressureUnits = 0;
        }
        if (GasPressure.isZero(outwardPressureUnits)) {
            outwardPressureUnits = 0;
        }
    }

    private enum PendingTransferOrigin {
        UNSPECIFIED(""),
        ANY_ENDPOINT("any"),
        OPEN_END("open_end"),
        EXTERNAL("external");

        private final String serializedName;

        PendingTransferOrigin(String serializedName) {
            this.serializedName = serializedName;
        }

        private static PendingTransferOrigin fromSerializedName(String name) {
            for (PendingTransferOrigin origin : values()) {
                if (origin.serializedName.equals(name)) {
                    return origin;
                }
            }
            return ANY_ENDPOINT;
        }

        private boolean accepts(GasFlowSource flowSource) {
            return switch (this) {
                case OPEN_END -> flowSource instanceof OpenEndedSource;
                case EXTERNAL -> flowSource instanceof ExternalHandlerSource;
                case UNSPECIFIED, ANY_ENDPOINT -> true;
            };
        }
    }

    public static final class GasFlow {
        public boolean inbound;
        public GasStack gas;

        private GasFlow(boolean inbound, GasStack gas) {
            this.inbound = inbound;
            this.gas = gas;
        }
    }
}
