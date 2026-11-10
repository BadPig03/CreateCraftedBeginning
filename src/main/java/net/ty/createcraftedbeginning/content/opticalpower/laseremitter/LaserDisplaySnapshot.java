package net.ty.createcraftedbeginning.content.opticalpower.laseremitter;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.phys.Vec3;
import net.ty.createcraftedbeginning.content.opticalpower.laser.display.LaserBeamDisplayInput;
import net.ty.createcraftedbeginning.content.opticalpower.laser.display.LaserBeamPathSignature.Input;
import net.ty.createcraftedbeginning.content.opticalpower.laser.trace.LaserRigidDisplay;
import net.ty.createcraftedbeginning.content.opticalpower.laser.trace.LaserStaticDependencies;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;

@Internal
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public record LaserDisplaySnapshot(Vec3 origin, Vec3 direction, double fluxLm, int range, LaserStaticDependencies dependencies, LaserBeamDisplayInput input, Input clipping, @Nullable LaserRigidDisplay rigid) {
    LaserDisplaySnapshot(Vec3 origin, Vec3 direction, double fluxLm, int range, LaserStaticDependencies dependencies, LaserBeamDisplayInput input, Input clipping) {
        this(origin, direction, fluxLm, range, dependencies, input, clipping, null);
    }
}
