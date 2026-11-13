package net.ty.createcraftedbeginning.content.opticalpower.chunkstabilizer;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.phys.Vec3;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
record ChunkStabilizerBeamRequest(Vec3 position, int color, boolean mergeStaticColumn) {
}
