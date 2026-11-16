package net.ty.createcraftedbeginning.content.opticalpower.laser.trace;

import net.minecraft.MethodsReturnNonnullByDefault;
import org.jetbrains.annotations.ApiStatus.Internal;

import javax.annotation.ParametersAreNonnullByDefault;

@Internal
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public interface LaserSectionAccess {
    LaserSectionChanges ccb$getLaserChanges();

    LaserSectionCandidates ccb$getLaserCandidates();
}
