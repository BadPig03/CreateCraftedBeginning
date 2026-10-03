package net.ty.createcraftedbeginning.content.airtights.airtightfractionationtower;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.ty.createcraftedbeginning.foundation.NbtValues;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Objects;
import java.util.function.UnaryOperator;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
final class AirtightFractionationTowerSerialization {
    private static final String COMPOUND_KEY_ORIGIN = "TowerOrigin";
    private static final String COMPOUND_KEY_HEIGHT = "TowerHeight";
    private static final String COMPOUND_KEY_STRUCTURE_MANAGER = "StructureManager";
    private static final String COMPOUND_KEY_CRAFTING = "Crafting";

    private final AirtightFractionationTowerBlockEntity owner;

    AirtightFractionationTowerSerialization(AirtightFractionationTowerBlockEntity owner) {
        this.owner = owner;
    }

    static void transformStructureNbt(CompoundTag tag, UnaryOperator<BlockPos> transform) {
        if (!tag.contains(COMPOUND_KEY_ORIGIN, Tag.TAG_LONG)) {
            return;
        }

        BlockPos controller = BlockPos.of(tag.getLong(COMPOUND_KEY_ORIGIN)).offset(1, 0, 1);
        tag.putLong(COMPOUND_KEY_ORIGIN, transform.apply(controller).offset(-1, 0, -1).asLong());
    }

    void write(CompoundTag compoundTag, Provider provider) {
        BlockPos origin = owner.getOrigin();
        if (origin == null) {
            return;
        }

        compoundTag.putLong(COMPOUND_KEY_ORIGIN, origin.asLong());
        compoundTag.putInt(COMPOUND_KEY_HEIGHT, owner.getHeight());
        if (!owner.isTowerController()) {
            return;
        }

        compoundTag.put(COMPOUND_KEY_STRUCTURE_MANAGER, owner.getStructureManager().write());
        compoundTag.put(COMPOUND_KEY_CRAFTING, owner.getCrafting().write(provider));
    }

    void read(CompoundTag compoundTag, Provider provider, boolean clientPacket) {
        BlockPos previousOrigin = owner.getOrigin();
        int previousHeight = owner.getHeight();
        BlockPos origin = null;
        if (compoundTag.contains(COMPOUND_KEY_ORIGIN, Tag.TAG_ANY_NUMERIC)) {
            origin = BlockPos.of(compoundTag.getLong(COMPOUND_KEY_ORIGIN));
        }
        int height = NbtValues.getIntOrDefault(compoundTag, COMPOUND_KEY_HEIGHT, 0);
        owner.loadStructure(origin, height);
        owner.getStructureManager().read(compoundTag.getCompound(COMPOUND_KEY_STRUCTURE_MANAGER));
        owner.getCrafting().read(compoundTag.getCompound(COMPOUND_KEY_CRAFTING), provider);
        if (!clientPacket || Objects.equals(previousOrigin, owner.getOrigin()) && previousHeight == owner.getHeight()) {
            return;
        }

        Level level = owner.getLevel();
        if (level == null || !level.isClientSide) {
            return;
        }

        owner.requestModelDataUpdate();
        BlockState state = owner.getBlockState();
        level.sendBlockUpdated(owner.getBlockPos(), state, state, Block.UPDATE_ALL);
    }
}
