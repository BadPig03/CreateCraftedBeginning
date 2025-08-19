package net.ty.createcraftedbeginning.data;

import com.tterrag.registrate.AbstractRegistrate;
import com.tterrag.registrate.builders.BuilderCallback;
import com.tterrag.registrate.builders.FluidBuilder;
import com.tterrag.registrate.util.nullness.NonNullFunction;
import com.tterrag.registrate.util.nullness.NonNullSupplier;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;
import org.jetbrains.annotations.NotNull;

public class CCBVirtualFluidBuilder<T extends BaseFlowingFluid, P> extends FluidBuilder<T, P> {
    public CCBVirtualFluidBuilder(AbstractRegistrate<?> owner, P parent, String name, BuilderCallback callback, ResourceLocation stillTexture, ResourceLocation flowingTexture, FluidBuilder.FluidTypeFactory typeFactory, NonNullFunction<BaseFlowingFluid.Properties, T> sourceFactory, NonNullFunction<BaseFlowingFluid.Properties, T> flowingFactory) {
        super(owner, parent, name, callback, stillTexture, flowingTexture, typeFactory, flowingFactory);
        source(sourceFactory);
    }

    @Override
    public @NotNull NonNullSupplier<T> asSupplier() {
        return this::getEntry;
    }
}
