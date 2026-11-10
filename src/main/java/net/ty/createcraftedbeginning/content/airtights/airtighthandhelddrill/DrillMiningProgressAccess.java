package net.ty.createcraftedbeginning.content.airtights.airtighthandhelddrill;

import net.minecraft.MethodsReturnNonnullByDefault;
import org.jetbrains.annotations.ApiStatus.Internal;

import javax.annotation.ParametersAreNonnullByDefault;

@FunctionalInterface
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@Internal
public interface DrillMiningProgressAccess {
    void ccb$resetDrillMiningProgress();
}
