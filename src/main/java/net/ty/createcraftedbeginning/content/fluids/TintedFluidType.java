package net.ty.createcraftedbeginning.content.fluids;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.material.FluidState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public abstract class TintedFluidType extends FluidType {
    protected final ResourceLocation stillTexture;
    protected final ResourceLocation flowingTexture;

    public TintedFluidType(Properties properties, ResourceLocation stillTexture, ResourceLocation flowingTexture) {
        super(properties);
        this.stillTexture = stillTexture;
        this.flowingTexture = flowingTexture;
        CCBFluidClientExtensionRegistry.registerTinted(this);
    }

    public final ResourceLocation clientStillTexture() {
        return stillTexture;
    }

    public final ResourceLocation clientFlowingTexture() {
        return flowingTexture;
    }

    public final int clientTintColor(FluidStack stack) {
        return getTintColor(stack);
    }

    public final int clientTintColor(FluidState state, BlockAndTintGetter getter, BlockPos pos) {
        return getTintColor(state, getter, pos);
    }

    public final @Nullable Vector3f clientCustomFogColor() {
        return getCustomFogColor();
    }

    public final float clientFogDistanceModifier() {
        return getFogDistanceModifier();
    }

    protected abstract int getTintColor(FluidStack stack);

    protected abstract int getTintColor(FluidState state, BlockAndTintGetter getter, BlockPos pos);

    protected @Nullable Vector3f getCustomFogColor() {
        return null;
    }

    protected float getFogDistanceModifier() {
        return 1;
    }
}