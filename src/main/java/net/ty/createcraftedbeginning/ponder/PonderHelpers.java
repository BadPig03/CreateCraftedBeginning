package net.ty.createcraftedbeginning.ponder;

import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

public class PonderHelpers {
    private static final double DISTRIBUTION_DEVIATION = 0.11485000171139836;

    @Contract("_ -> new")
    public static @NotNull Vec3 generateItemDropVelocity(@NotNull RandomSource random) {
        return new Vec3(random.triangle(0, DISTRIBUTION_DEVIATION), random.triangle(0.2, DISTRIBUTION_DEVIATION), random.triangle(0, DISTRIBUTION_DEVIATION));
    }
}