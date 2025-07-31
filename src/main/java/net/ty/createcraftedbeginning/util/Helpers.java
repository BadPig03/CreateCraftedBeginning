package net.ty.createcraftedbeginning.util;


import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;

public class Helpers {
    private static final double DISTRIBUTION_DEVIATION = 0.11485000171139836;

    public static Vec3 generateItemDropVelocity(RandomSource random) {
        return new Vec3(
            random.triangle(0.0, DISTRIBUTION_DEVIATION),
            random.triangle(0.2, DISTRIBUTION_DEVIATION),
            random.triangle(0.0, DISTRIBUTION_DEVIATION));
    }
}
