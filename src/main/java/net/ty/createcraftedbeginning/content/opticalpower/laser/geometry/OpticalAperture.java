package net.ty.createcraftedbeginning.content.opticalpower.laser.geometry;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.ApiStatus.Internal;

import javax.annotation.ParametersAreNonnullByDefault;

@Internal
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public record OpticalAperture(Vec3 center, Vec3 horizontal, Vec3 vertical, double halfSize) {}
