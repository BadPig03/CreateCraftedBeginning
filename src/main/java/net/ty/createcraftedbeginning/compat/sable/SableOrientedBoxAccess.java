package net.ty.createcraftedbeginning.compat.sable;

import net.minecraft.MethodsReturnNonnullByDefault;
import org.joml.Quaterniond;
import org.joml.Vector3d;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public interface SableOrientedBoxAccess {
    Quaterniond ccb$getOrientation();

    Vector3d ccb$getDimensions();
}
