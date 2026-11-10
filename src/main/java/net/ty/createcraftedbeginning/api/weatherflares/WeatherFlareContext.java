package net.ty.createcraftedbeginning.api.weatherflares;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public record WeatherFlareContext(ServerLevel level, Vec3 pos, WeatherFlareEffect effect, ItemStack flareStack, double durationRatio) {
    public WeatherFlareContext {
        flareStack = flareStack.copyWithCount(1);
    }

    @Override
    public ItemStack flareStack() {
        return flareStack.copy();
    }

    public int durationTicks() {
        return Mth.ceil(WeatherFlare.DEFAULT_DURATION * durationRatio);
    }
}
