package net.ty.createcraftedbeginning.content.breezes.breezecooler;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.fluids.FluidStack;
import net.ty.createcraftedbeginning.recipe.CoolingRecipeLookup;
import net.ty.createcraftedbeginning.recipe.CoolingRecipeLookup.CoolingData;

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
        Level level = cooler.getLevel();
        if (level == null || fluidStack.isEmpty()) {
            return CoolingData.EMPTY;
        }

        long gameTime = level.getGameTime();
        boolean isSameFluid = !cachedFluid.isEmpty() && FluidStack.isSameFluidSameComponents(cachedFluid, fluidStack);
        if (isSameFluid && gameTime < expiry) {
            return cachedData;
        }

        cachedFluid = fluidStack.copyWithAmount(1);
        cachedData = CoolingRecipeLookup.findCoolingData(level, null, fluidStack);
        expiry = gameTime + CACHE_INTERVAL;
        return cachedData;
    }
}
