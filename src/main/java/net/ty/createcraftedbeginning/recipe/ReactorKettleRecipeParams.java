package net.ty.createcraftedbeginning.recipe;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.ty.createcraftedbeginning.recipe.gas.processing.GasProcessingRecipeParams;
import net.ty.createcraftedbeginning.recipe.temperature.TemperatureCondition;
import net.ty.createcraftedbeginning.recipe.temperature.TemperatureMatching;
import net.ty.createcraftedbeginning.recipe.temperature.TemperatureRecipeData;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.function.Function;
import java.util.function.Supplier;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class ReactorKettleRecipeParams extends GasProcessingRecipeParams {
    public static final MapCodec<ReactorKettleRecipeParams> CODEC = reactorKettleCodec(ReactorKettleRecipeParams::new);
    public static final StreamCodec<RegistryFriendlyByteBuf, ReactorKettleRecipeParams> STREAM_CODEC = gasRecipeStreamCodec(ReactorKettleRecipeParams::new);

    protected TemperatureRecipeData temperatureRecipeData;

    protected ReactorKettleRecipeParams() {
        temperatureRecipeData = TemperatureRecipeData.DEFAULT;
    }

    protected static <P extends ReactorKettleRecipeParams> MapCodec<P> reactorKettleCodec(Supplier<P> factory) {
        return RecordCodecBuilder.mapCodec(instance -> instance.group(gasRecipeCodec(factory).forGetter(Function.identity()), TemperatureRecipeData.MAP_CODEC.forGetter(ReactorKettleRecipeParams::temperatureRecipeData)).apply(instance, (params, temperatureRecipeData) -> {
            params.temperatureRecipeData = temperatureRecipeData;
            return params;
        }));
    }

    @Override
    protected void encode(RegistryFriendlyByteBuf buffer) {
        super.encode(buffer);
        TemperatureRecipeData.STREAM_CODEC.encode(buffer, temperatureRecipeData);
    }

    @Override
    protected void decode(RegistryFriendlyByteBuf buffer) {
        super.decode(buffer);
        temperatureRecipeData = TemperatureRecipeData.STREAM_CODEC.decode(buffer);
    }

    protected final TemperatureRecipeData temperatureRecipeData() {
        return temperatureRecipeData;
    }

    protected final void temperatureCondition(TemperatureCondition condition) {
        temperatureRecipeData = temperatureRecipeData.withCondition(condition);
    }

    protected final void temperatureMatching(TemperatureMatching matching) {
        temperatureRecipeData = temperatureRecipeData.withMatching(matching);
    }
}
