package net.ty.createcraftedbeginning.compat.sable;

import dev.ryanhcode.sable.companion.SableCompanion;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Position;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class SableSubLevelCompat {
    private SableSubLevelCompat() {
    }

    public static Projection resolve(Level level, Position position) {
        boolean inSubLevel = SableCompanion.INSTANCE.getContaining(level, position) != null;
        Vec3 projectedPosition = SableCompanion.INSTANCE.projectOutOfSubLevel(level, position);
        return new Projection(projectedPosition, inSubLevel);
    }

    public static Projection resolve(Level level, BlockPos blockPos) {
        return resolve(level, Vec3.atCenterOf(blockPos));
    }

    public record Projection(Vec3 worldPosition, boolean inSubLevel) {
        public BlockPos blockPos() {
            return BlockPos.containing(worldPosition);
        }
    }
}
