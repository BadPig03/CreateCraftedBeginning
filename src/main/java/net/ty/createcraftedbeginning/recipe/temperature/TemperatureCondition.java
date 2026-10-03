package net.ty.createcraftedbeginning.recipe.temperature;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import net.createmod.catnip.codecs.stream.CatnipStreamCodecBuilders;
import net.createmod.catnip.lang.Lang;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.StringRepresentable;
import net.ty.createcraftedbeginning.api.thermoregulatorhandlers.AirtightThermoregulatorHandler;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Arrays;
import java.util.function.Function;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public enum TemperatureCondition implements StringRepresentable {
    SUPERCHILLED(0xFF1E90FF, temp -> temp <= AirtightThermoregulatorHandler.SUPERCHILLED),
    CHILLED(0xFF87CEEB, temp -> temp > AirtightThermoregulatorHandler.SUPERCHILLED && temp <= AirtightThermoregulatorHandler.CHILLED),
    NONE(0xFFFFFFFF, temp -> temp > AirtightThermoregulatorHandler.CHILLED && temp < AirtightThermoregulatorHandler.HEATED),
    HEATED(0xFFFFA500, temp -> temp >= AirtightThermoregulatorHandler.HEATED && temp < AirtightThermoregulatorHandler.SUPERHEATED),
    SUPERHEATED(0xFFFF4500, temp -> temp >= AirtightThermoregulatorHandler.SUPERHEATED);

    public static final Codec<TemperatureCondition> CODEC = StringRepresentable.fromEnum(TemperatureCondition::values);
    public static final StreamCodec<ByteBuf, TemperatureCondition> STREAM_CODEC = CatnipStreamCodecBuilders.ofEnum(TemperatureCondition.class);

    private final int color;
    private final Function<Float, Boolean> function;

    TemperatureCondition(int color, Function<Float, Boolean> function) {
        this.color = color;
        this.function = function;
    }

    public static TemperatureCondition getConditionByTemperature(float temperature) {
        return Arrays.stream(values()).filter(condition -> condition.test(temperature)).findFirst().orElse(NONE);
    }

    @Override
    public String getSerializedName() {
        return Lang.asId(name());
    }

    public String getTranslationKey() {
        return "recipe.temperature_condition." + getSerializedName();
    }

    public int getColor() {
        return color;
    }

    public boolean supportsCompatibleMatching() {
        return this != SUPERHEATED && this != SUPERCHILLED;
    }

    public boolean test(float temperature) {
        return function.apply(temperature);
    }
}
