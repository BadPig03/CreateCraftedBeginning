package net.ty.createcraftedbeginning.content.opticalpower.amethystcollectorpanel;

import net.minecraft.MethodsReturnNonnullByDefault;
import org.jetbrains.annotations.ApiStatus.Internal;

import javax.annotation.ParametersAreNonnullByDefault;

@Internal
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public record AmethystCollectorPanelOutput(int powerLp, Limitation limitation) {
    public enum Limitation {
        NONE,
        OVERSIZED,
        NO_SKYLIGHT,
        NIGHT,
        OBSTRUCTED,
        RAIN
    }
}
