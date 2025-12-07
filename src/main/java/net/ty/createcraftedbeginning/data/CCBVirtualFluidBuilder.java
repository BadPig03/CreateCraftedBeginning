package net.ty.createcraftedbeginning.data;

import com.tterrag.registrate.AbstractRegistrate;
import com.tterrag.registrate.builders.BuilderCallback;
import com.tterrag.registrate.builders.FluidBuilder;
import com.tterrag.registrate.util.nullness.NonNullFunction;
import com.tterrag.registrate.util.nullness.NonNullSupplier;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.fluids.FluidType.Properties;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

public class CCBVirtualFluidBuilder<T extends BaseFlowingFluid, P> extends FluidBuilder<T, P> {
    public CCBVirtualFluidBuilder(AbstractRegistrate<?> owner, P parent, String name, BuilderCallback callback, ResourceLocation texture, NonNullFunction<BaseFlowingFluid.Properties, T> sourceFactory, NonNullFunction<BaseFlowingFluid.Properties, T> flowingFactory) {
        super(owner, parent, name, callback, texture, texture, CCBVirtualFluidBuilder::defaultFluidType, flowingFactory);
        source(sourceFactory);
    }

    @SuppressWarnings("removal")
    private static @NotNull FluidType defaultFluidType(Properties properties, ResourceLocation stillTexture, ResourceLocation flowingTexture) {
        return new FluidType(properties) {
            @Override
            public void initializeClient(@NotNull Consumer<IClientFluidTypeExtensions> consumer) {
                consumer.accept(new IClientFluidTypeExtensions() {
                    @Override
                    public @NotNull ResourceLocation getStillTexture() {
                        return stillTexture;
                    }

                    @Override
                    public @NotNull ResourceLocation getFlowingTexture() {
                        return flowingTexture;
                    }
                });
            }
        };
    }

    @Override
    public @NotNull NonNullSupplier<T> asSupplier() {
        return this::getEntry;
    }
}
