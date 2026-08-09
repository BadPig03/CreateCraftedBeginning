package net.ty.createcraftedbeginning.content.airtights.airtighttank;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.Block;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
final class AirtightTankSerialization {
    private static final String CORE = "Core";

    private final AirtightTankBlockEntity owner;
    private final AirtightTankStorageController storage;

    AirtightTankSerialization(AirtightTankBlockEntity owner, AirtightTankStorageController storage) {
        this.owner = owner;
        this.storage = storage;
    }

    void write(CompoundTag tag, Provider provider, boolean clientPacket) {
        AirtightTankSerializationSupport.writeMultiblock(owner, tag, clientPacket);
        if (!owner.isController()) {
            return;
        }

        tag.put(CORE, owner.getCore().write(provider, clientPacket));
        tag.put(AirtightTankSerializationSupport.TANK_CONTENT, owner.getTankInventory().write(provider, new CompoundTag()));
    }

    void writeSafe(CompoundTag tag) {
        AirtightTankSerializationSupport.writeSafeMultiblock(owner, tag);
    }

    void read(CompoundTag tag, Provider provider, boolean clientPacket) {
        boolean clientStructureChanged = AirtightTankSerializationSupport.readMultiblock(owner, tag, clientPacket);
        if (owner.isController()) {
            storage.setCapacityForStructure();
            if (tag.contains(AirtightTankSerializationSupport.TANK_CONTENT)) {
                owner.getTankInventory().read(provider, tag.getCompound(AirtightTankSerializationSupport.TANK_CONTENT));
                storage.drainOverflow();
            }
        }

        if (tag.contains(CORE)) {
            owner.getCore().read(tag.getCompound(CORE), provider, clientPacket);
        }
        if (!clientStructureChanged) {
            return;
        }

        updateClientState();
    }

    private void updateClientState() {
        if (owner.getLevel() != null) {
            owner.getLevel().sendBlockUpdated(owner.getBlockPos(), owner.getBlockState(), owner.getBlockState(), Block.UPDATE_KNOWN_SHAPE);
        }
        if (owner.isController()) {
            storage.setCapacityForStructure();
        }
        owner.invalidateRenderBounds();
    }
}
