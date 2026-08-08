package net.ty.createcraftedbeginning.registry.registrate;

import com.tterrag.registrate.AbstractRegistrate;
import com.tterrag.registrate.builders.BuilderCallback;
import com.tterrag.registrate.builders.FluidBuilder;
import com.tterrag.registrate.util.nullness.NonNullFunction;
import com.tterrag.registrate.util.nullness.NonNullSupplier;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.fluids.FluidType.Properties;
import net.ty.createcraftedbeginning.content.fluids.CCBFluidClientExtensionRegistry;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class CCBVirtualFluidBuilder<T extends BaseFlowingFluid, P> extends FluidBuilder<T, P> {
    public CCBVirtualFluidBuilder(AbstractRegistrate<?> owner, P parent, String name, BuilderCallback callback, ResourceLocation texture, NonNullFunction<BaseFlowingFluid.Properties, T> sourceFactory, NonNullFunction<BaseFlowingFluid.Properties, T> flowingFactory) {
        this(owner, parent, name, callback, texture, CCBVirtualFluidBuilder::defaultFluidType, sourceFactory, flowingFactory);
    }

    public CCBVirtualFluidBuilder(AbstractRegistrate<?> owner, P parent, String name, BuilderCallback callback, ResourceLocation texture, FluidTypeFactory fluidTypeFactory, NonNullFunction<BaseFlowingFluid.Properties, T> sourceFactory, NonNullFunction<BaseFlowingFluid.Properties, T> flowingFactory) {
        super(owner, parent, name, callback, texture, texture, fluidTypeFactory, flowingFactory);
        source(sourceFactory);
    }

    private static FluidType defaultFluidType(Properties properties, ResourceLocation stillTexture, ResourceLocation flowingTexture) {
        FluidType fluidType = new FluidType(properties);
        CCBFluidClientExtensionRegistry.registerSimple(fluidType, stillTexture, flowingTexture);
        return fluidType;
    }

    @Override
    public NonNullSupplier<T> asSupplier() {
        return this::getEntry;
    }
}
