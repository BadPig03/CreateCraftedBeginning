package net.ty.createcraftedbeginning.content.opticalpower.loadingnetwork;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderGetter;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureBlockInfo;
import net.ty.createcraftedbeginning.content.opticalpower.loadingnetwork.LoadingNetworkSavedData.Node;
import net.ty.createcraftedbeginning.registry.CCBBlocks;
import org.jetbrains.annotations.Unmodifiable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
final class LoadingNetworkTrainSerialization {
    private static final String COMPOUND_KEY_CONTRAPTION = "Contraption";
    private static final String COMPOUND_KEY_BLOCKS = "Blocks";
    private static final String COMPOUND_KEY_PALETTE = "Palette";
    private static final String COMPOUND_KEY_BLOCK_LIST = "BlockList";
    private static final String COMPOUND_KEY_STATE = "State";
    private static final String COMPOUND_KEY_POS = "Pos";
    private static final String COMPOUND_KEY_DATA = "Data";

    private LoadingNetworkTrainSerialization() {
    }

    static @Unmodifiable List<Node> read(CompoundTag source, HolderGetter<Block> lookup) {
        CompoundTag blocks = source.getCompound(COMPOUND_KEY_CONTRAPTION).getCompound(COMPOUND_KEY_BLOCKS);
        ListTag palette = blocks.getList(COMPOUND_KEY_PALETTE, Tag.TAG_COMPOUND);
        Map<Integer, BlockState> states = new HashMap<>();
        for (int index = 0; index < palette.size(); index++) {
            BlockState state = NbtUtils.readBlockState(lookup, palette.getCompound(index));
            if (!state.is(CCBBlocks.CHUNK_STABILIZER_BLOCK.get()) && !state.is(CCBBlocks.AMETHYST_ANTENNA_BLOCK.get())) {
                continue;
            }

            states.put(index, state);
        }
        Map<BlockPos, StructureBlockInfo> captured = new HashMap<>();
        for (Tag entry : blocks.getList(COMPOUND_KEY_BLOCK_LIST, Tag.TAG_COMPOUND)) {
            CompoundTag tag = (CompoundTag) entry;
            BlockState state = states.get(tag.getInt(COMPOUND_KEY_STATE));
            if (state == null) {
                continue;
            }

            BlockPos pos = BlockPos.of(tag.getLong(COMPOUND_KEY_POS));
            captured.put(pos, new StructureBlockInfo(pos, state, tag.getCompound(COMPOUND_KEY_DATA)));
        }
        return List.copyOf(LoadingNetworkContraptions.members(captured).values());
    }
}
