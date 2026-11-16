package net.ty.createcraftedbeginning.content.opticalpower.laser.trace;

import net.minecraft.MethodsReturnNonnullByDefault;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.function.IntPredicate;

@Internal
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class LaserSectionChanges {
    private static final int CAPACITY = 64;
    private final short[] positions = new short[CAPACITY];
    private long revision;

    public void changed(int x, int y, int z) {
        positions[(int) (revision % CAPACITY)] = (short) (y << 8 | z << 4 | x);
        revision++;
    }

    long revision() {
        return revision;
    }

    boolean intersects(long previous, int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        return intersects(previous, minX, minY, minZ, maxX, maxY, maxZ, null);
    }

    boolean intersects(long previous, int minX, int minY, int minZ, int maxX, int maxY, int maxZ, @Nullable IntPredicate observed) {
        if (previous > revision || revision - previous > CAPACITY) {
            return true;
        }

        for (long change = previous; change < revision; change++) {
            int position = positions[(int) (change % CAPACITY)];
            int x = position & 15;
            int y = position >> 8 & 15;
            int z = position >> 4 & 15;
            if (x >= minX && x <= maxX && y >= minY && y <= maxY && z >= minZ && z <= maxZ && (observed == null || observed.test(position))) {
                return true;
            }
        }
        return false;
    }
}
