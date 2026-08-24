package net.ty.createcraftedbeginning.content.airtights.creativeairtighttank;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.ty.createcraftedbeginning.content.airtights.airtighttank.AirtightTankSerializationSupport;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
final class CreativeAirtightTankSerialization {
    private final CreativeAirtightTankBlockEntity owner;
    private final CreativeAirtightTankStorageController storage;

    CreativeAirtightTankSerialization(CreativeAirtightTankBlockEntity owner, CreativeAirtightTankStorageController storage) {
        this.owner = owner;
        this.storage = storage;
    }

    void write(CompoundTag compoundTag, Provider provider, boolean clientPacket) {
        AirtightTankSerializationSupport.writeMultiblock(owner, compoundTag, clientPacket);
        if (!owner.isController()) {
            return;
        }

        compoundTag.put(AirtightTankSerializationSupport.TANK_CONTENT, owner.getTankInventory().write(provider, new CompoundTag()));
    }

    void writeSafe(CompoundTag compoundTag) {
        AirtightTankSerializationSupport.writeSafeMultiblock(owner, compoundTag);
    }

    void read(CompoundTag compoundTag, Provider provider, boolean clientPacket) {
        boolean clientStructureChanged = AirtightTankSerializationSupport.readMultiblock(owner, compoundTag, clientPacket);
        if (owner.isController()) {
            storage.resetCapacity();
            if (compoundTag.contains(AirtightTankSerializationSupport.TANK_CONTENT)) {
                owner.getTankInventory().read(provider, compoundTag.getCompound(AirtightTankSerializationSupport.TANK_CONTENT));
            }
        }
        if (!clientStructureChanged) {
            return;
        }

        owner.updateClientStructureState();
    }
}
