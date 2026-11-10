package net.ty.createcraftedbeginning.content.opticalpower.experiencecrystal;

import net.minecraft.MethodsReturnNonnullByDefault;
import org.jetbrains.annotations.ApiStatus.Internal;

import javax.annotation.ParametersAreNonnullByDefault;

@FunctionalInterface
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@Internal
public interface ExperienceCrystalSectionAccess {
    ExperienceCrystalSectionIndex ccb$getCrystalIndex();
}
