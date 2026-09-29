package net.ty.createcraftedbeginning.mixin.compat.sable;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.ty.createcraftedbeginning.compat.sable.AssemblyTransformAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@Pseudo
@Mixin(targets = "dev.ryanhcode.sable.api.SubLevelAssemblyHelper$AssemblyTransform", remap = false)
public abstract class AssemblyTransformMixin implements AssemblyTransformAccess {
    @Override
    public BlockPos ccb$transform(BlockPos pos) {
        return apply(pos);
    }

    @Shadow
    public abstract BlockPos apply(BlockPos pos);
}
