package net.ty.createcraftedbeginning.content.opticalpower.laser.trace;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.level.block.AirBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunkSection;
import org.jetbrains.annotations.ApiStatus.Internal;

import javax.annotation.ParametersAreNonnullByDefault;

@Internal
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class LaserSectionCandidates {
    private final long[] occupied = new long[64];
    private final long[] unbounded = new long[64];
    private int unknown;

    public LaserSectionCandidates(LevelChunkSection section) {
        if (!section.getStates().maybeHas(state -> state.getBlock().getClass() != AirBlock.class)) {
            return;
        }

        for (int index = 0; index < 4096; index++) {
            int x = index & 15;
            int y = index >> 8;
            int z = index >> 4 & 15;
            changed(x, y, z, section.getBlockState(x, y, z));
        }
    }

    public void changed(int x, int y, int z, BlockState state) {
        int index = y << 8 | z << 4 | x;
        int word = index >> 6;
        long bit = 1L << (index & 63);
        boolean before = (unbounded[word] & bit) != 0;
        boolean air = state.getBlock().getClass() == AirBlock.class;
        boolean after = !LaserWorldSections.stable(state);
        occupied[word] = air ? occupied[word] & ~bit : occupied[word] | bit;
        unbounded[word] = after ? unbounded[word] | bit : unbounded[word] & ~bit;
        if (before == after) {
            return;
        }

        unknown += after ? 1 : -1;
    }

    boolean hasUnbounded() {
        return unknown > 0;
    }

    boolean isUnbounded(int index) {
        return (unbounded[index >> 6] & 1L << (index & 63)) != 0;
    }

    int next(int from) {
        if (from >= 4096) {
            return -1;
        }

        int word = from >> 6;
        long bits = occupied[word] & -1L << (from & 63);
        while (bits == 0) {
            if (++word == occupied.length) {
                return -1;
            }

            bits = occupied[word];
        }
        return word * 64 + Long.numberOfTrailingZeros(bits);
    }
}
