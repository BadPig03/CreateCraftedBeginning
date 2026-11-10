package net.ty.createcraftedbeginning.content.opticalpower.amethystloader;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.phys.Vec3;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
record AmethystLoaderBeamRequest(Vec3 position, int color, boolean mergeStaticColumn) {
}
