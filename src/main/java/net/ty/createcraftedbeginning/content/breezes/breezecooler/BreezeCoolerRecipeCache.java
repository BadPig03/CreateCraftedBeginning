package net.ty.createcraftedbeginning.content.breezes.breezecooler;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.neoforged.neoforge.fluids.FluidStack;
import net.ty.createcraftedbeginning.recipe.CoolingRecipe;
import net.ty.createcraftedbeginning.recipe.CoolingRecipe.CoolingData;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
final class BreezeCoolerRecipeCache {
    private static final int CACHE_INTERVAL = 100;
    private final BreezeCoolerBlockEntity cooler;
    private FluidStack cachedFluid = FluidStack.EMPTY;
    private CoolingData cachedData = CoolingData.EMPTY;
    private long expiry = Long.MIN_VALUE;

    BreezeCoolerRecipeCache(BreezeCoolerBlockEntity cooler) {
        this.cooler = cooler;
    }

    CoolingData getFluidCoolingData(FluidStack fluidStack) {
        if (cooler.getLevel() == null || fluidStack.isEmpty()) {
            return CoolingData.EMPTY;
        }

        long gameTime = cooler.getLevel().getGameTime();
        boolean sameFluid = !cachedFluid.isEmpty() && FluidStack.isSameFluidSameComponents(cachedFluid, fluidStack);
        if (sameFluid && gameTime < expiry) {
            return cachedData;
        }

        cachedFluid = fluidStack.copyWithAmount(1);
        cachedData = CoolingRecipe.getCoolingTime(cooler.getLevel(), null, fluidStack);
        expiry = gameTime + CACHE_INTERVAL;
        return cachedData;
    }
}
