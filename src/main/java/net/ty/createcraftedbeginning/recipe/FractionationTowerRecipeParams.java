package net.ty.createcraftedbeginning.recipe;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.ty.createcraftedbeginning.recipe.gas.processing.GasProcessingRecipeParams;
import net.ty.createcraftedbeginning.recipe.temperature.TemperatureRecipeData;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class FractionationTowerRecipeParams extends GasProcessingRecipeParams {
    public static final MapCodec<FractionationTowerRecipeParams> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(gasRecipeCodec(FractionationTowerRecipeParams::new).forGetter(Function.identity()), TemperatureRecipeData.MAP_CODEC.forGetter(params -> params.temperature), FractionationTowerOutput.CODEC.listOf().fieldOf("layer_outputs").forGetter(params -> params.outputs)).apply(instance, (params, temperature, outputs) -> {
        params.temperature = temperature;
        params.outputs = new ArrayList<>(outputs);
        return params;
    }));
    public static final StreamCodec<RegistryFriendlyByteBuf, FractionationTowerRecipeParams> STREAM_CODEC = gasRecipeStreamCodec(FractionationTowerRecipeParams::new);
    private static final StreamCodec<RegistryFriendlyByteBuf, List<FractionationTowerOutput>> OUTPUTS_STREAM_CODEC = FractionationTowerOutput.STREAM_CODEC.apply(ByteBufCodecs.list());

    TemperatureRecipeData temperature = TemperatureRecipeData.DEFAULT;
    List<FractionationTowerOutput> outputs = new ArrayList<>();

    @Override
    protected void encode(RegistryFriendlyByteBuf buffer) {
        super.encode(buffer);
        TemperatureRecipeData.STREAM_CODEC.encode(buffer, temperature);
        OUTPUTS_STREAM_CODEC.encode(buffer, outputs);
    }

    @Override
    protected void decode(RegistryFriendlyByteBuf buffer) {
        super.decode(buffer);
        temperature = TemperatureRecipeData.STREAM_CODEC.decode(buffer);
        outputs = OUTPUTS_STREAM_CODEC.decode(buffer);
    }
}
