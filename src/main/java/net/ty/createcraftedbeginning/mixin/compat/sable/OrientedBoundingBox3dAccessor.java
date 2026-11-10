package net.ty.createcraftedbeginning.mixin.compat.sable;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.ty.createcraftedbeginning.compat.sable.SableOrientedBoxAccess;
import org.joml.Quaterniond;
import org.joml.Vector3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.gen.Accessor;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@Pseudo
@Mixin(targets = "dev.ryanhcode.sable.api.math.OrientedBoundingBox3d", remap = false)
public interface OrientedBoundingBox3dAccessor extends SableOrientedBoxAccess {
    @Override
    @Accessor("orientation")
    Quaterniond ccb$getOrientation();

    @Override
    @Accessor("dimensions")
    Vector3d ccb$getDimensions();
}
