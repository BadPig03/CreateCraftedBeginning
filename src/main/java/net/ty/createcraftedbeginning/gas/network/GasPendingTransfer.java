package net.ty.createcraftedbeginning.gas.network;

import com.simibubi.create.foundation.ICapabilityProvider;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.ty.createcraftedbeginning.api.gas.GasAction;
import net.ty.createcraftedbeginning.api.gas.GasStack;
import net.ty.createcraftedbeginning.api.gas.handler.GasHandler;
import net.ty.createcraftedbeginning.api.gas.pressure.GasPressureCompartment;
import net.ty.createcraftedbeginning.foundation.NbtValues;
import net.ty.createcraftedbeginning.gas.network.endpoint.AtmosphericGasEndpoint;
import net.ty.createcraftedbeginning.gas.network.endpoint.ExternalGasEndpoint;
import net.ty.createcraftedbeginning.gas.network.endpoint.GasConnectionEndpoint;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
final class GasPendingTransfer {
    private static final String COMPOUND_KEY_PENDING_TRANSFER = "PendingTransfer";
    private static final String COMPOUND_KEY_PENDING_TRANSFER_ORIGIN = "PendingTransferOrigin";
    private static final int RETIRED_RECOVERY_INITIAL_DELAY_TICKS = 20;
    private static final int RETIRED_RECOVERY_MAX_DELAY_TICKS = 640;

    private GasStack transfer = GasStack.EMPTY;
    private RecoveryOrigin origin = RecoveryOrigin.UNSPECIFIED;
    private int retiredRecoveryDelayTicks = RETIRED_RECOVERY_INITIAL_DELAY_TICKS;
    private int retiredRecoveryCooldownTicks;

    boolean isEmpty() {
        return transfer.isEmpty();
    }

    boolean retain(GasStack additionalTransfer, @Nullable GasConnectionEndpoint endpoint, @Nullable GasConnectionEndpoint previousEndpoint) {
        if (additionalTransfer.isEmpty()) {
            return true;
        }

        if (transfer.isEmpty()) {
            transfer = additionalTransfer.copy();
            origin = RecoveryOrigin.EXTERNAL_HANDLER;
            return true;
        }

        if (!GasStack.isSameGasSameComponents(transfer, additionalTransfer)) {
            return false;
        }

        GasStack combinedTransfer = transfer.copy();
        combinedTransfer.grow(additionalTransfer.getAmount());
        setTransfer(combinedTransfer, endpoint, previousEndpoint);
        return true;
    }

    void captureOrigin(@Nullable GasConnectionEndpoint endpoint, @Nullable GasConnectionEndpoint previousEndpoint) {
        if (transfer.isEmpty() || origin != RecoveryOrigin.UNSPECIFIED) {
            return;
        }

        GasConnectionEndpoint recoveryEndpoint = endpoint != null ? endpoint : previousEndpoint;
        if (recoveryEndpoint instanceof AtmosphericGasEndpoint) {
            origin = RecoveryOrigin.ATMOSPHERE;
            return;
        }

        if (!(recoveryEndpoint instanceof ExternalGasEndpoint)) {
            return;
        }

        origin = RecoveryOrigin.EXTERNAL_HANDLER;
    }

    boolean recover(Level level, BlockPos ownerPos, @Nullable GasConnectionEndpoint endpoint, @Nullable GasConnectionEndpoint previousEndpoint) {
        if (transfer.isEmpty()) {
            return true;
        }

        if (endpoint == null || !origin.accepts(endpoint)) {
            return false;
        }

        BlockEntity blockEntity = level.getBlockEntity(ownerPos);
        if (blockEntity == null) {
            return false;
        }

        endpoint.bind(level, blockEntity);
        ICapabilityProvider<GasHandler> endpointProvider = endpoint.getGasHandlerProvider();
        if (endpointProvider == null) {
            return false;
        }

        GasHandler endpointHandler = endpointProvider.getCapability();
        if (endpointHandler == null) {
            return false;
        }

        long returnedAmount = endpointHandler instanceof GasPressureCompartment compartment && compartment.supportsExactDrainRecovery() ? compartment.restoreDrainedGas(transfer.copy(), GasAction.EXECUTE) : endpointHandler.fill(transfer.copy(), GasAction.EXECUTE);
        returnedAmount = Mth.clamp(returnedAmount, 0L, transfer.getAmount());
        if (returnedAmount <= 0) {
            return false;
        }

        GasStack remainingTransfer = transfer.copy();
        remainingTransfer.shrink(returnedAmount);
        setTransfer(remainingTransfer, endpoint, previousEndpoint);
        blockEntity.setChanged();
        return transfer.isEmpty();
    }

    GasStack releaseCustody() {
        GasStack releasedTransfer = transfer.copy();
        setTransfer(GasStack.EMPTY, null, null);
        return releasedTransfer;
    }

    void beginRetiredRecoveryBackoff() {
        retiredRecoveryDelayTicks = RETIRED_RECOVERY_INITIAL_DELAY_TICKS;
        retiredRecoveryCooldownTicks = RETIRED_RECOVERY_INITIAL_DELAY_TICKS;
    }

    boolean isRetiredRecoveryReady() {
        if (retiredRecoveryCooldownTicks <= 0) {
            return true;
        }

        retiredRecoveryCooldownTicks--;
        return false;
    }

    void backoffRetiredRecovery() {
        retiredRecoveryDelayTicks = Math.min(retiredRecoveryDelayTicks * 2, RETIRED_RECOVERY_MAX_DELAY_TICKS);
        retiredRecoveryCooldownTicks = retiredRecoveryDelayTicks;
    }

    void write(CompoundTag compoundTag, Provider provider, @Nullable GasConnectionEndpoint endpoint, @Nullable GasConnectionEndpoint previousEndpoint) {
        if (transfer.isEmpty()) {
            return;
        }

        captureOrigin(endpoint, previousEndpoint);
        compoundTag.put(COMPOUND_KEY_PENDING_TRANSFER, transfer.saveOptional(provider));
        if (origin == RecoveryOrigin.UNSPECIFIED || origin == RecoveryOrigin.ANY_ENDPOINT) {
            return;
        }

        compoundTag.putString(COMPOUND_KEY_PENDING_TRANSFER_ORIGIN, origin.serializedName);
    }

    void read(CompoundTag compoundTag, Provider provider) {
        transfer = compoundTag.contains(COMPOUND_KEY_PENDING_TRANSFER, Tag.TAG_COMPOUND) ? GasStack.parseOptional(provider, compoundTag.getCompound(COMPOUND_KEY_PENDING_TRANSFER)) : GasStack.EMPTY;
        origin = transfer.isEmpty() ? RecoveryOrigin.UNSPECIFIED : readOrigin(compoundTag);
        retiredRecoveryDelayTicks = RETIRED_RECOVERY_INITIAL_DELAY_TICKS;
        retiredRecoveryCooldownTicks = 0;
    }

    private static RecoveryOrigin readOrigin(CompoundTag compoundTag) {
        String serializedOrigin = NbtValues.getStringOrDefault(compoundTag, COMPOUND_KEY_PENDING_TRANSFER_ORIGIN, RecoveryOrigin.ANY_ENDPOINT.serializedName);
        return RecoveryOrigin.fromSerializedName(serializedOrigin);
    }

    private void setTransfer(GasStack nextTransfer, @Nullable GasConnectionEndpoint endpoint, @Nullable GasConnectionEndpoint previousEndpoint) {
        transfer = nextTransfer.isEmpty() ? GasStack.EMPTY : nextTransfer.copy();
        if (transfer.isEmpty()) {
            origin = RecoveryOrigin.UNSPECIFIED;
            return;
        }

        captureOrigin(endpoint, previousEndpoint);
    }

    private enum RecoveryOrigin {
        UNSPECIFIED(""),
        ANY_ENDPOINT("any_endpoint"),
        ATMOSPHERE("atmosphere"),
        EXTERNAL_HANDLER("external_handler");

        private final String serializedName;

        RecoveryOrigin(String serializedName) {
            this.serializedName = serializedName;
        }

        private static RecoveryOrigin fromSerializedName(String name) {
            for (RecoveryOrigin origin : values()) {
                if (!origin.serializedName.equals(name)) {
                    continue;
                }

                return origin;
            }
            return ANY_ENDPOINT;
        }

        private boolean accepts(GasConnectionEndpoint endpoint) {
            return switch (this) {
                case ATMOSPHERE -> endpoint instanceof AtmosphericGasEndpoint;
                case EXTERNAL_HANDLER -> endpoint instanceof ExternalGasEndpoint;
                case UNSPECIFIED, ANY_ENDPOINT -> true;
            };
        }
    }
}
