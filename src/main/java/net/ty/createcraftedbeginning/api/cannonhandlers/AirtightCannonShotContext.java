package net.ty.createcraftedbeginning.api.cannonhandlers;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.Entity;
import net.ty.createcraftedbeginning.api.gas.gases.Gas;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public record AirtightCannonShotContext(Entity projectile, @Nullable Entity owner, Holder<Gas> gasHolder, float effectMultiplier, float knockbackMultiplier, boolean flame) {
    /**
     * Creates a cannon-shot context for an externally supplied source.
     *
     * @param source the source associated with the operation
     * @param gasHolder the gas holder to use
     * @param effectMultiplier the effect multiplier value to use
     * @return this instance
     */
    public static AirtightCannonShotContext external(Entity source, Holder<Gas> gasHolder, float effectMultiplier) {
        return new AirtightCannonShotContext(source, source, gasHolder, effectMultiplier, 1.0f, false);
    }
}
