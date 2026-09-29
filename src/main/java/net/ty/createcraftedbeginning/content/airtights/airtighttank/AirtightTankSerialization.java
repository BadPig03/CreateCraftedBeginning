package net.ty.createcraftedbeginning.content.airtights.airtighttank;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.ty.createcraftedbeginning.gas.storage.GasTank;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
final class AirtightTankSerialization {
    private static final String CORE = "Core";
    private static final String TANK_GAUGE = "TankGauge";

    private final AirtightTankBlockEntity owner;
    private final AirtightTankStorageController storage;

    AirtightTankSerialization(AirtightTankBlockEntity owner, AirtightTankStorageController storage) {
        this.owner = owner;
        this.storage = storage;
    }

    void write(CompoundTag compoundTag, Provider provider, boolean clientPacket) {
        AirtightTankSerializationSupport.writeMultiblock(owner, compoundTag, clientPacket);
        compoundTag.putBoolean(TANK_GAUGE, owner.isLocalTankGaugeInstalled());
        if (!owner.isController()) {
            return;
        }

        compoundTag.put(CORE, owner.getCore().write(provider, clientPacket));
        compoundTag.put(AirtightTankSerializationSupport.TANK_CONTENT, owner.getTankInventory().write(provider, new CompoundTag()));
    }

    void writeSafe(CompoundTag compoundTag) {
        AirtightTankSerializationSupport.writeSafeMultiblock(owner, compoundTag);
        compoundTag.putBoolean(TANK_GAUGE, owner.isLocalTankGaugeInstalled());
    }

    void read(CompoundTag compoundTag, Provider provider, boolean clientPacket) {
        boolean clientStructureChanged = AirtightTankSerializationSupport.readMultiblock(owner, compoundTag, clientPacket);
        owner.setLocalTankGaugeInstalled(compoundTag.getBoolean(TANK_GAUGE));
        if (owner.isController()) {
            GasTank tank = owner.getTankInventory();
            if (compoundTag.contains(AirtightTankSerializationSupport.TANK_CONTENT)) {
                GasTank stagedTank = new GasTank(storage.limitsForStructure());
                stagedTank.read(provider, compoundTag.getCompound(AirtightTankSerializationSupport.TANK_CONTENT));
                tank.tryApplyState(stagedTank.snapshot()).requireAccepted();
            }
            else {
                tank.tryReconfigure(storage.limitsForStructure()).requireAccepted();
            }
        }

        if (compoundTag.contains(CORE)) {
            owner.getCore().read(compoundTag.getCompound(CORE), provider, clientPacket);
        }
        if (!clientStructureChanged) {
            return;
        }

        updateClientState();
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
