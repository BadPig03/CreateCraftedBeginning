package net.ty.createcraftedbeginning.content.opticalpower.laser.beam;

import net.minecraft.MethodsReturnNonnullByDefault;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;

@Internal
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public record BeamFootprint(double horizontalScale, double verticalScale, double horizontalOffset, double verticalOffset, @Nullable BeamRegion region) {
    public static final BeamFootprint FULL = new BeamFootprint(1, 1);

    public BeamFootprint(double horizontalScale, double verticalScale) {
        this(horizontalScale, verticalScale, 0, 0);
    }

    public BeamFootprint(double horizontalScale, double verticalScale, double horizontalOffset, double verticalOffset) {
        this(horizontalScale, verticalScale, horizontalOffset, verticalOffset, null);
    }

    public BeamFootprint {
        if (!Double.isFinite(horizontalScale) || horizontalScale <= 0 || horizontalScale > 1 || !Double.isFinite(verticalScale) || verticalScale <= 0 || verticalScale > 1 || horizontalScale * verticalScale == 0) {
            throw new IllegalArgumentException("Beam footprint scales must be finite and in (0, 1] with positive area; got horizontal=" + horizontalScale + ", vertical=" + verticalScale + '.');
        }

        if (!Double.isFinite(horizontalOffset) || !Double.isFinite(verticalOffset)) {
            throw new IllegalArgumentException("Beam footprint offsets must be finite; got horizontal=" + horizontalOffset + ", vertical=" + verticalOffset + '.');
        }

        horizontalOffset += 0.0;
        verticalOffset += 0.0;
    }

    public double areaFraction() {
        if (region != null) {
            return region.area() / 4;
        }

        return horizontalScale * verticalScale;
    }

    public BeamRegion domain() {
        if (region != null) {
            return region;
        }

        return BeamRegion.rectangle(horizontalOffset - horizontalScale, verticalOffset - verticalScale, horizontalOffset + horizontalScale, verticalOffset + verticalScale);
    }
}
