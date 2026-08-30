package net.ty.createcraftedbeginning.content.airtights.airtighttank;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.ty.createcraftedbeginning.foundation.CCBNbtUtils;

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

    void write(CompoundTag compoundTag, Provider provider, boolean clientPacket) {
        AirtightTankSerializationSupport.writeMultiblock(owner, compoundTag, clientPacket);
        if (!owner.isController()) {
            return;
        }

        CCBNbtUtils.putTag(compoundTag, CORE, owner.getCore().write(provider, clientPacket));
        CCBNbtUtils.putTag(compoundTag, AirtightTankSerializationSupport.TANK_CONTENT, owner.getTankInventory().write(provider, new CompoundTag()));
    }

    void writeSafe(CompoundTag compoundTag) {
        AirtightTankSerializationSupport.writeSafeMultiblock(owner, compoundTag);
    }

    void read(CompoundTag compoundTag, Provider provider, boolean clientPacket) {
        boolean clientStructureChanged = AirtightTankSerializationSupport.readMultiblock(owner, compoundTag, clientPacket);
        if (owner.isController()) {
            storage.setCapacityForStructure();
            if (CCBNbtUtils.contains(compoundTag, AirtightTankSerializationSupport.TANK_CONTENT)) {
                owner.getTankInventory().read(provider, CCBNbtUtils.getCompound(compoundTag, AirtightTankSerializationSupport.TANK_CONTENT));
                storage.drainOverflow();
            }
        }

        if (CCBNbtUtils.contains(compoundTag, CORE)) {
            owner.getCore().read(CCBNbtUtils.getCompound(compoundTag, CORE), provider, clientPacket);
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
            storage.setCapacityForStructure();
        }
        owner.invalidateRenderBounds();
    }
}
