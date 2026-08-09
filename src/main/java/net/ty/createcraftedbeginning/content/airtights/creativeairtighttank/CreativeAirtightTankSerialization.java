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

    void write(CompoundTag tag, Provider provider, boolean clientPacket) {
        AirtightTankSerializationSupport.writeMultiblock(owner, tag, clientPacket);
        if (!owner.isController()) {
            return;
        }

        tag.put(AirtightTankSerializationSupport.TANK_CONTENT, owner.getTankInventory().write(provider, new CompoundTag()));
    }

    void writeSafe(CompoundTag tag) {
        AirtightTankSerializationSupport.writeSafeMultiblock(owner, tag);
    }

    void read(CompoundTag tag, Provider provider, boolean clientPacket) {
        boolean clientStructureChanged = AirtightTankSerializationSupport.readMultiblock(owner, tag, clientPacket);
        if (owner.isController()) {
            storage.resetCapacity();
            if (tag.contains(AirtightTankSerializationSupport.TANK_CONTENT)) {
                owner.getTankInventory().read(provider, tag.getCompound(AirtightTankSerializationSupport.TANK_CONTENT));
            }
        }
        if (!clientStructureChanged) {
            return;
        }

        owner.updateClientStructureState();
    }
}
