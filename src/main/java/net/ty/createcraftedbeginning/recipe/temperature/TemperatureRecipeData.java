package net.ty.createcraftedbeginning.recipe.temperature;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public record TemperatureRecipeData(TemperatureCondition condition, TemperatureMatching matching) {
    public static final TemperatureRecipeData DEFAULT = new TemperatureRecipeData(TemperatureCondition.NONE, TemperatureMatching.EXACT);
    public static final MapCodec<TemperatureRecipeData> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(TemperatureCondition.CODEC.optionalFieldOf("temperature", TemperatureCondition.NONE).forGetter(TemperatureRecipeData::condition), TemperatureMatching.CODEC.optionalFieldOf("temperature_matching", TemperatureMatching.EXACT).forGetter(TemperatureRecipeData::matching)).apply(instance, TemperatureRecipeData::new));
    public static final Codec<TemperatureRecipeData> CODEC = MAP_CODEC.codec();
    public static final StreamCodec<RegistryFriendlyByteBuf, TemperatureRecipeData> STREAM_CODEC = StreamCodec.of((buffer, data) -> {
        TemperatureCondition.STREAM_CODEC.encode(buffer, data.condition());
        TemperatureMatching.STREAM_CODEC.encode(buffer, data.matching());
    }, buffer -> new TemperatureRecipeData(TemperatureCondition.STREAM_CODEC.decode(buffer), TemperatureMatching.STREAM_CODEC.decode(buffer)));

    public TemperatureRecipeData withCondition(TemperatureCondition condition) {
        return new TemperatureRecipeData(condition, matching);
    }

    public TemperatureRecipeData withMatching(TemperatureMatching matching) {
        return new TemperatureRecipeData(condition, matching);
    }

    public boolean isValid() {
        return matching.isValidFor(condition);
    }

    public boolean test(float temperature) {
        return matching.test(condition, temperature);
    }
}
