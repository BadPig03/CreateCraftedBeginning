package net.ty.createcraftedbeginning.gas.flow;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class GasIntakeArea {
    private static final double SIDE_LENGTH = 3;

    private GasIntakeArea() {
    }

    public static AABB bounds(Vec3 sourceHalfSize, Vec3 direction) {
        Vec3 center = direction.multiply(sourceHalfSize).scale(1 + SIDE_LENGTH);
        Vec3 halfSize = sourceHalfSize.scale(SIDE_LENGTH);
        return new AABB(center.subtract(halfSize), center.add(halfSize));
    }
}
