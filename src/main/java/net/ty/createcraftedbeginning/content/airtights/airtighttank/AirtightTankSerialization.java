package net.ty.createcraftedbeginning.content.airtights.airtighttank;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.ty.createcraftedbeginning.api.gas.GasPressure;
import net.ty.createcraftedbeginning.api.gas.GasStack;
import net.ty.createcraftedbeginning.api.gasreleasehandlers.GasReleaseCause;
import net.ty.createcraftedbeginning.foundation.BoundedMath;
import net.ty.createcraftedbeginning.gas.release.GasReleaseRequest;
import net.ty.createcraftedbeginning.gas.storage.GasTank;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
final class AirtightTankSerialization {
    static final String COMPOUND_KEY_CORE = "Core";
    static final String COMPOUND_KEY_PENDING_OVERFLOW = "PendingOverflowReleases";
    private static final String COMPOUND_KEY_TANK_GAUGE = "TankGauge";
    private static final String COMPOUND_KEY_GAS = "Gas";
    private static final String COMPOUND_KEY_SOURCE_PRESSURE = "SourcePressurePa";

    private final AirtightTankBlockEntity owner;
    private final AirtightTankStorageController storage;

    AirtightTankSerialization(AirtightTankBlockEntity owner, AirtightTankStorageController storage) {
        this.owner = owner;
        this.storage = storage;
    }

    void write(CompoundTag compoundTag, Provider provider, boolean clientPacket) {
        AirtightTankSerializationSupport.writeMultiblock(owner, compoundTag, clientPacket);
        compoundTag.putBoolean(COMPOUND_KEY_TANK_GAUGE, owner.isLocalTankGaugeInstalled());
        compoundTag.remove(COMPOUND_KEY_PENDING_OVERFLOW);
        if (!clientPacket) {
            ListTag pendingOverflow = new ListTag();
            for (GasReleaseRequest release : storage.getPendingOverflowReleases()) {
                CompoundTag releaseTag = new CompoundTag();
                releaseTag.put(COMPOUND_KEY_GAS, release.gas().saveOptional(provider));
                releaseTag.putLong(COMPOUND_KEY_SOURCE_PRESSURE, release.sourcePressurePa().orElseThrow());
                pendingOverflow.add(releaseTag);
            }
            if (!pendingOverflow.isEmpty()) {
                compoundTag.put(COMPOUND_KEY_PENDING_OVERFLOW, pendingOverflow);
            }
        }
        if (!owner.isController()) {
            return;
        }

        compoundTag.put(COMPOUND_KEY_CORE, owner.getCore().write(provider, clientPacket));
        compoundTag.put(AirtightTankSerializationSupport.COMPOUND_KEY_TANK_CONTENT, owner.getTankInventory().write(provider, new CompoundTag()));
    }

    void writeSafe(CompoundTag compoundTag) {
        AirtightTankSerializationSupport.writeSafeMultiblock(owner, compoundTag);
        compoundTag.putBoolean(COMPOUND_KEY_TANK_GAUGE, owner.isLocalTankGaugeInstalled());
    }

    void read(CompoundTag compoundTag, Provider provider, boolean clientPacket) {
        boolean clientStructureChanged = AirtightTankSerializationSupport.readMultiblock(owner, compoundTag, clientPacket);
        owner.setLocalTankGaugeInstalled(compoundTag.getBoolean(COMPOUND_KEY_TANK_GAUGE));
        Level level = owner.getLevel();
        boolean recoverOverflow = !clientPacket && (level == null || !level.isClientSide);
        List<GasReleaseRequest> pendingOverflow = new ArrayList<>();
        GasReleaseRequest contentsOverflow = null;
        if (recoverOverflow) {
            pendingOverflow.addAll(readPendingOverflow(compoundTag, provider));
        }
        if (owner.isController()) {
            GasTank tank = owner.getTankInventory();
            if (compoundTag.contains(AirtightTankSerializationSupport.COMPOUND_KEY_TANK_CONTENT)) {
                GasTank stagedTank = new GasTank(storage.limitsForStructure());
                GasStack overflow = stagedTank.readClampedToLimits(provider, compoundTag.getCompound(AirtightTankSerializationSupport.COMPOUND_KEY_TANK_CONTENT));
                if (recoverOverflow && !overflow.isEmpty()) {
                    long loadedAmount = BoundedMath.saturatedAdd(stagedTank.getStoredAmount(), overflow.getAmount());
                    long sourcePressurePa = GasPressure.pressure(loadedAmount, stagedTank.getVolume());
                    contentsOverflow = GasReleaseRequest.radial(overflow, owner.getBlockPos(), GasReleaseCause.OVERFLOW, sourcePressurePa);
                    pendingOverflow.add(contentsOverflow);
                }
                tank.tryApplyState(stagedTank.snapshot()).requireAccepted();
            }
            else {
                tank.tryReconfigure(storage.limitsForStructure()).requireAccepted();
            }
        }
        if (!clientPacket) {
            storage.setPendingOverflowReleases(pendingOverflow, contentsOverflow);
        }

        if (compoundTag.contains(COMPOUND_KEY_CORE)) {
            owner.getCore().read(compoundTag.getCompound(COMPOUND_KEY_CORE), provider, clientPacket);
        }
        if (!clientStructureChanged) {
            return;
        }

        updateClientState();
    }

    private List<GasReleaseRequest> readPendingOverflow(CompoundTag compoundTag, Provider provider) {
        List<GasReleaseRequest> releases = new ArrayList<>();
        ListTag pendingOverflow = compoundTag.getList(COMPOUND_KEY_PENDING_OVERFLOW, Tag.TAG_COMPOUND);
        for (int index = 0; index < pendingOverflow.size(); index++) {
            CompoundTag releaseTag = pendingOverflow.getCompound(index);
            GasStack gas = GasStack.parseOptional(provider, releaseTag.getCompound(COMPOUND_KEY_GAS));
            if (gas.isEmpty()) {
                continue;
            }

            long sourcePressurePa = Math.max(GasPressure.VACUUM_PA, releaseTag.getLong(COMPOUND_KEY_SOURCE_PRESSURE));
            releases.add(GasReleaseRequest.radial(gas, owner.getBlockPos(), GasReleaseCause.OVERFLOW, sourcePressurePa));
        }
        return releases;
    }

    private void updateClientState() {
        Level level = owner.getLevel();
        if (level != null) {
            level.sendBlockUpdated(owner.getBlockPos(), owner.getBlockState(), owner.getBlockState(), Block.UPDATE_KNOWN_SHAPE);
        }
        if (owner.isController()) {
            storage.setVolumeForStructure();
        }
        owner.invalidateRenderBounds();
    }
}
