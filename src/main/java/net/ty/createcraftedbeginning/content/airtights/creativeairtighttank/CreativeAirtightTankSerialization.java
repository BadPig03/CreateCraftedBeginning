package net.ty.createcraftedbeginning.content.airtights.creativeairtighttank;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.ty.createcraftedbeginning.content.airtights.airtighttank.AirtightTankSerializationSupport;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
final class CreativeAirtightTankSerialization {
    private static final String COMPOUND_KEY_FIXED_PRESSURE = "FixedPressure";

    private final CreativeAirtightTankBlockEntity owner;
    private final CreativeAirtightTankStorageController storage;

    CreativeAirtightTankSerialization(CreativeAirtightTankBlockEntity owner, CreativeAirtightTankStorageController storage) {
        this.owner = owner;
        this.storage = storage;
    }

    void write(CompoundTag compoundTag, Provider provider, boolean clientPacket) {
        AirtightTankSerializationSupport.writeMultiblock(owner, compoundTag, clientPacket);
        compoundTag.putLong(COMPOUND_KEY_FIXED_PRESSURE, owner.getFixedPressurePa());
        if (!owner.isController()) {
            return;
        }

        compoundTag.put(AirtightTankSerializationSupport.TANK_CONTENT, owner.getTankInventory().write(provider, new CompoundTag()));
    }

    void writeSafe(CompoundTag compoundTag) {
        AirtightTankSerializationSupport.writeSafeMultiblock(owner, compoundTag);
        compoundTag.putLong(COMPOUND_KEY_FIXED_PRESSURE, owner.getFixedPressurePa());
    }

    void read(CompoundTag compoundTag, Provider provider, boolean clientPacket) {
        boolean clientStructureChanged = AirtightTankSerializationSupport.readMultiblock(owner, compoundTag, clientPacket);
        if (owner.isController()) {
            storage.resetReservoirLimits();
            if (compoundTag.contains(AirtightTankSerializationSupport.TANK_CONTENT)) {
                owner.getTankInventory().read(provider, compoundTag.getCompound(AirtightTankSerializationSupport.TANK_CONTENT));
            }
        }

        if (compoundTag.contains(COMPOUND_KEY_FIXED_PRESSURE)) {
            owner.loadLocalFixedPressurePa(compoundTag.getLong(COMPOUND_KEY_FIXED_PRESSURE));
        }
        owner.syncPressureBehaviour();
        if (!clientStructureChanged) {
            return;
        }

        owner.updateClientStructureState();
    }
}
