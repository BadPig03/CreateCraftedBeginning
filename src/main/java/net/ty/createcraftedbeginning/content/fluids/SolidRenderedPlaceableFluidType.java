package net.ty.createcraftedbeginning.content.fluids;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.material.FluidState;
import net.neoforged.neoforge.fluids.FluidStack;
import org.joml.Vector3f;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.function.Supplier;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class SolidRenderedPlaceableFluidType extends TintedFluidType {
    private final Vector3f fogColor;
    private final Supplier<Float> fogDistance;

    protected SolidRenderedPlaceableFluidType(Properties properties, ResourceLocation stillTexture, ResourceLocation flowingTexture, Vector3f fogColor, Supplier<Float> fogDistance) {
        super(properties, stillTexture, flowingTexture);
        this.fogColor = fogColor;
        this.fogDistance = fogDistance;
    }

    @Override
    int getTintColor(FluidStack stack) {
        return 0xFFFFFFFF;
    }

    @Override
    int getTintColor(FluidState state, BlockAndTintGetter getter, BlockPos pos) {
        return 0x00FFFFFF;
    }

    @Override
    Vector3f getCustomFogColor() {
        return fogColor;
    }

    @Override
    float getFogDistanceModifier() {
        return fogDistance.get();
    }
}
