package net.ty.createcraftedbeginning.compat.sable;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;

import javax.annotation.ParametersAreNonnullByDefault;

@FunctionalInterface
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public interface AssemblyTransformAccess {
    BlockPos ccb$transform(BlockPos pos);
}
