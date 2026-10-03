package net.ty.createcraftedbeginning.recipe.gas.processing;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.simibubi.create.content.processing.recipe.ProcessingRecipeParams;
import net.createmod.catnip.codecs.stream.CatnipStreamCodecBuilders;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.ty.createcraftedbeginning.api.gas.GasStack;
import net.ty.createcraftedbeginning.recipe.gas.GasRecipeData;
import net.ty.createcraftedbeginning.recipe.gas.GasRecipeRequirement;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.function.Function;
import java.util.function.Supplier;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class GasProcessingRecipeParams extends ProcessingRecipeParams {
    public static final MapCodec<GasProcessingRecipeParams> CODEC = gasRecipeCodec(GasProcessingRecipeParams::new);
    public static final StreamCodec<RegistryFriendlyByteBuf, GasProcessingRecipeParams> STREAM_CODEC = gasRecipeStreamCodec(GasProcessingRecipeParams::new);

    protected NonNullList<GasRecipeRequirement> gasRequirements;
    protected NonNullList<GasStack> gasResults;

    protected GasProcessingRecipeParams() {
        gasRequirements = NonNullList.create();
        gasResults = NonNullList.create();
    }

    protected static <P extends GasProcessingRecipeParams> MapCodec<P> gasRecipeCodec(Supplier<P> factory) {
        return RecordCodecBuilder.mapCodec(instance -> instance.group(codec(factory).forGetter(Function.identity()), GasRecipeData.MAP_CODEC.forGetter(GasProcessingRecipeParams::gasRecipeData)).apply(instance, (params, gasRecipeData) -> {
            params.gasRequirements.addAll(gasRecipeData.requirements());
            params.gasResults.addAll(gasRecipeData.results());
            return params;
        }));
    }

    protected static <P extends GasProcessingRecipeParams> StreamCodec<RegistryFriendlyByteBuf, P> gasRecipeStreamCodec(Supplier<P> factory) {
        return streamCodec(factory);
    }

    @Override
    protected void encode(RegistryFriendlyByteBuf buffer) {
        super.encode(buffer);
        CatnipStreamCodecBuilders.nonNullList(GasRecipeRequirement.STREAM_CODEC).encode(buffer, gasRequirements);
        CatnipStreamCodecBuilders.nonNullList(GasStack.STREAM_CODEC).encode(buffer, gasResults);
    }

    @Override
    protected void decode(RegistryFriendlyByteBuf buffer) {
        super.decode(buffer);
        gasRequirements = CatnipStreamCodecBuilders.nonNullList(GasRecipeRequirement.STREAM_CODEC).decode(buffer);
        gasResults = CatnipStreamCodecBuilders.nonNullList(GasStack.STREAM_CODEC).decode(buffer);
    }

    protected final GasRecipeData gasRecipeData() {
        return new GasRecipeData(gasRequirements, gasResults);
    }
}
