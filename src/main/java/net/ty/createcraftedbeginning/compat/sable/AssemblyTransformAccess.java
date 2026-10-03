package net.ty.createcraftedbeginning.compat.sable;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public interface AssemblyTransformAccess {
    BlockPos ccb$transform(BlockPos pos);

    ServerLevel ccb$getResultingLevel();
}
