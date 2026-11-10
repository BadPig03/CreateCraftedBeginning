package net.ty.createcraftedbeginning.content.opticalpower.laser;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Objects;

@Internal
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public record OpticalHit(OpticalTarget target, BlockHitResult localHit, Vec3 position, OpticalSurface surface, @Nullable Vec3 localPlaneNormal) {
    public OpticalHit {
        Objects.requireNonNull(target, "Optical hit target must not be null.");
        Objects.requireNonNull(localHit, "Optical hit must include a local block hit result.");
        localHit = localHit.withPosition(localHit.getBlockPos().immutable());
        Objects.requireNonNull(surface, "Optical hit surface must not be null.");
        if (!Double.isFinite(position.x) || !Double.isFinite(position.y) || !Double.isFinite(position.z)) {
            throw new IllegalArgumentException("Optical hit position must have finite coordinates; got " + position + '.');
        }
    }
}
