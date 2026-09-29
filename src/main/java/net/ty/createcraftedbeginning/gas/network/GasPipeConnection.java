package net.ty.createcraftedbeginning.gas.network;

import net.createmod.catnip.data.Iterate;
import net.createmod.catnip.math.BlockFace;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.Level;
import net.ty.createcraftedbeginning.api.gas.GasStack;
import net.ty.createcraftedbeginning.foundation.NbtValues;
import net.ty.createcraftedbeginning.gas.network.GasConnectionResolver.AdjacentConnection;
import net.ty.createcraftedbeginning.gas.network.endpoint.AtmosphericGasEndpoint;
import net.ty.createcraftedbeginning.gas.network.endpoint.ExternalGasEndpoint;
import net.ty.createcraftedbeginning.gas.network.endpoint.GasConnectionEndpoint;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class GasPipeConnection {
    private static final String COMPOUND_KEY_ATMOSPHERIC_ENDPOINT = "AtmosphericEndpoint";
    private static final String COMPOUND_KEY_SIDE = "Side";

    private final Direction side;
    private final GasPendingTransfer pendingTransfer;

    @Nullable
    private GasConnectionEndpoint endpoint;
    @Nullable
    private GasConnectionEndpoint previousEndpoint;
    @Nullable
    private FlowState flowState;

    public GasPipeConnection(Direction side) {
        this.side = side;
        pendingTransfer = new GasPendingTransfer();
    }

    @Nullable
    public static GasPipeConnection readRetiredData(CompoundTag retiredData, Provider provider) {
        if (!retiredData.contains(COMPOUND_KEY_SIDE, Tag.TAG_INT)) {
            return null;
        }

        int retiredSideId = retiredData.getInt(COMPOUND_KEY_SIDE);
        Direction retiredSide = null;
        for (Direction direction : Iterate.directions) {
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
        retiredConnection.pendingTransfer.read(retiredData, provider);
        if (retiredConnection.pendingTransfer.isEmpty()) {
            return null;
        }

        retiredConnection.beginRetiredRecoveryBackoff();
        return retiredConnection;
    }

    public Direction getSide() {
        return side;
    }

    @Nullable
    public FlowState getFlowState() {
        return flowState;
    }

    public boolean setFlowState(GasStack gas, boolean inbound, long flowRate) {
        if (gas.isEmpty() || flowRate <= 0) {
            return clearFlowState();
        }

        FlowState nextState = new FlowState(gas, inbound ? FlowDirection.INBOUND : FlowDirection.OUTBOUND, flowRate);
        if (nextState.equals(flowState)) {
            return false;
        }

        flowState = nextState;
        return true;
    }

    public boolean clearFlowState() {
        if (flowState == null) {
            return false;
        }

        flowState = null;
        return true;
    }

    public boolean hasPendingTransfer() {
        return !pendingTransfer.isEmpty();
    }

    public boolean tryRecoverPendingTransfer(Level level, BlockPos pos) {
        return recoverPendingTransfer(level, pos);
    }

    public boolean retainPendingTransfer(GasStack transfer) {
        return pendingTransfer.retain(transfer, endpoint, previousEndpoint);
    }

    public boolean prepareForRemoval(Level level, BlockPos pos) {
        pendingTransfer.captureOrigin(endpoint, previousEndpoint);
        clearFlowState();
        if (!recoverPendingTransfer(level, pos)) {
            return false;
        }

        endpoint = null;
        previousEndpoint = null;
        return true;
    }

    public GasStack finalizePendingTransferForOwnerRemoval(Level level, BlockPos pos) {
        pendingTransfer.captureOrigin(endpoint, previousEndpoint);
        clearFlowState();
        recoverPendingTransfer(level, pos);

        GasStack unrecoveredTransfer = pendingTransfer.releaseCustody();
        endpoint = null;
        previousEndpoint = null;
        return unrecoveredTransfer;
    }

    public void beginRetiredRecoveryBackoff() {
        pendingTransfer.beginRetiredRecoveryBackoff();
    }

    public boolean tryRecoverRetired(Level level, BlockPos pos) {
        if (!pendingTransfer.isRetiredRecoveryReady()) {
            return false;
        }

        endpoint = null;
        if (prepareForRemoval(level, pos)) {
            return true;
        }

        pendingTransfer.backoffRetiredRecovery();
        return false;
    }

    public void write(CompoundTag compoundTag, Provider provider, boolean clientPacket) {
        if (clientPacket) {
            return;
        }

        CompoundTag connectionData = new CompoundTag();
        writePersistentData(connectionData, provider);
        if (connectionData.isEmpty()) {
            return;
        }

        compoundTag.put(side.getName(), connectionData);
    }

    public void read(CompoundTag compoundTag, Provider provider, BlockPos blockPos, boolean clientPacket) {
        if (clientPacket) {
            flowState = null;
            return;
        }

        CompoundTag connectionData = NbtValues.getCompoundOrEmpty(compoundTag, side.getName());
        readPersistentData(connectionData, provider, blockPos);
    }

    public CompoundTag writeRetiredData(Provider provider) {
        if (pendingTransfer.isEmpty()) {
            return new CompoundTag();
        }

        CompoundTag retiredData = new CompoundTag();
        retiredData.putInt(COMPOUND_KEY_SIDE, side.get3DDataValue());
        pendingTransfer.write(retiredData, provider, endpoint, previousEndpoint);
        return retiredData;
    }

    public @Nullable GasConnectionEndpoint getEndpoint() {
        return endpoint;
    }

    public boolean resolveEndpoint(Level level, BlockPos pos) {
        BlockFace endpointLocation = new BlockFace(pos, side);
        AdjacentConnection adjacentConnection = GasConnectionResolver.resolveAdjacentConnection(level, pos, side);
        GasConnectionEndpoint currentEndpoint = endpoint;
        if (adjacentConnection.isAtmospheric()) {
            if (currentEndpoint instanceof AtmosphericGasEndpoint) {
                return true;
            }

            endpoint = previousEndpoint instanceof AtmosphericGasEndpoint ? previousEndpoint : new AtmosphericGasEndpoint(endpointLocation);
            return true;
        }

        if (adjacentConnection.hasGasHandler()) {
            if (currentEndpoint instanceof ExternalGasEndpoint) {
                return true;
            }

            if (currentEndpoint instanceof AtmosphericGasEndpoint) {
                previousEndpoint = currentEndpoint;
            }
            endpoint = new ExternalGasEndpoint(endpointLocation);
            return true;
        }

        if (currentEndpoint instanceof AtmosphericGasEndpoint) {
            previousEndpoint = currentEndpoint;
        }
        endpoint = null;
        return false;
    }

    private void writePersistentData(CompoundTag compoundTag, Provider provider) {
        AtmosphericGasEndpoint atmosphericEndpoint = null;
        if (endpoint instanceof AtmosphericGasEndpoint currentAtmosphericEndpoint) {
            atmosphericEndpoint = currentAtmosphericEndpoint;
        }
        else if (endpoint == null && previousEndpoint instanceof AtmosphericGasEndpoint previousAtmosphericEndpoint) {
            atmosphericEndpoint = previousAtmosphericEndpoint;
        }
        if (atmosphericEndpoint != null) {
            CompoundTag atmosphericEndpointTag = atmosphericEndpoint.write(provider);
            if (!atmosphericEndpointTag.isEmpty()) {
                compoundTag.put(COMPOUND_KEY_ATMOSPHERIC_ENDPOINT, atmosphericEndpointTag);
            }
        }
        pendingTransfer.write(compoundTag, provider, endpoint, previousEndpoint);
    }

    private void readPersistentData(CompoundTag connectionData, Provider provider, BlockPos blockPos) {
        flowState = null;
        endpoint = null;
        previousEndpoint = null;
        pendingTransfer.read(connectionData, provider);
        if (!connectionData.contains(COMPOUND_KEY_ATMOSPHERIC_ENDPOINT, Tag.TAG_COMPOUND)) {
            return;
        }

        AtmosphericGasEndpoint atmosphericEndpoint = AtmosphericGasEndpoint.read(connectionData.getCompound(COMPOUND_KEY_ATMOSPHERIC_ENDPOINT), provider, new BlockFace(blockPos, side));
        endpoint = atmosphericEndpoint;
        previousEndpoint = atmosphericEndpoint;
    }

    private boolean recoverPendingTransfer(Level level, BlockPos pos) {
        return pendingTransfer.isEmpty() || resolveEndpoint(level, pos) && pendingTransfer.recover(level, pos, endpoint, previousEndpoint);
    }

    public enum FlowDirection {
        INBOUND,
        OUTBOUND
    }

    public record FlowState(GasStack gas, FlowDirection direction, long flowRate) {
        public FlowState {
            gas = gas.isEmpty() ? GasStack.EMPTY : gas.copyWithAmount(1);
            flowRate = Math.max(0, flowRate);
        }

        @Override
        public GasStack gas() {
            return gas.copy();
        }
    }
}
