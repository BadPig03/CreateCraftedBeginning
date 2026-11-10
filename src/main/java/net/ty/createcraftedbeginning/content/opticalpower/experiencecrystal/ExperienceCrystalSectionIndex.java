package net.ty.createcraftedbeginning.content.opticalpower.experiencecrystal;

import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunkSection;
import org.jetbrains.annotations.ApiStatus.Internal;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.function.Consumer;

@Internal
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class ExperienceCrystalSectionIndex {
    private static final int HISTORY_SIZE = 64;
    private static final int CELL_Y_SHIFT = 2 * SectionPos.SECTION_BITS;

    private final IntSet relevant = new IntOpenHashSet();
    private final int[] changes = new int[HISTORY_SIZE];
    private long revision;

    public ExperienceCrystalSectionIndex(LevelChunkSection section) {
        if (!section.maybeHas(ExperienceCrystalSectionIndex::isRelevant)) {
            return;
        }

        for (int cell = 0; cell < LevelChunkSection.SECTION_SIZE; cell++) {
            update(cell, section.getBlockState(cellX(cell), cellY(cell), cellZ(cell)));
        }
    }

    private static boolean isRelevant(BlockState state) {
        return state.is(Blocks.SCULK) || state.getBlock() instanceof ExperienceCrystalBlock crystal && !crystal.isMature();
    }

    private static int cellX(int cell) {
        return cell & SectionPos.SECTION_MASK;
    }

    private static int cellY(int cell) {
        return cell >> CELL_Y_SHIFT;
    }

    private static int cellZ(int cell) {
        return cell >> SectionPos.SECTION_BITS & SectionPos.SECTION_MASK;
    }

    private static BlockPos position(BlockPos origin, int cell) {
        return origin.offset(cellX(cell), cellY(cell), cellZ(cell));
    }

    public void changed(int x, int y, int z, BlockState state) {
        int cell = y << CELL_Y_SHIFT | z << SectionPos.SECTION_BITS | x;
        update(cell, state);
        changes[(int) (revision++ % HISTORY_SIZE)] = cell;
    }

    public long revision() {
        return revision;
    }

    public boolean collectChanges(long previous, BlockPos origin, Consumer<BlockPos> consumer) {
        if (previous > revision || revision - previous > HISTORY_SIZE) {
            return false;
        }

        for (long cursor = previous; cursor < revision; cursor++) {
            consumer.accept(position(origin, changes[(int) (cursor % HISTORY_SIZE)]));
        }
        return true;
    }

    public void collectRelevant(BlockPos origin, Consumer<BlockPos> consumer) {
        relevant.forEach((int cell) -> consumer.accept(position(origin, cell)));
    }

    private void update(int cell, BlockState state) {
        if (isRelevant(state)) {
            relevant.add(cell);
            return;
        }

        relevant.remove(cell);
    }
}
