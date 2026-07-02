package net.ty.createcraftedbeginning.ponder;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Contract;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class PonderHelpers {
    private static final double DISTRIBUTION_DEVIATION = 0.11485000171139836;

    @Contract("_ -> new")
    public static Vec3 generateItemDropVelocity(RandomSource random) {
        return new Vec3(random.triangle(0, DISTRIBUTION_DEVIATION), random.triangle(0.2, DISTRIBUTION_DEVIATION), random.triangle(0, DISTRIBUTION_DEVIATION));
    }
}