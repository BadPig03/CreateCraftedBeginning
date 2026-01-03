package net.ty.createcraftedbeginning.api.gas.reactorkettle;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import net.createmod.catnip.codecs.stream.CatnipStreamCodecBuilders;
import net.createmod.catnip.lang.Lang;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.StringRepresentable;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.function.Function;

public enum TemperatureCondition implements StringRepresentable {
    SUPERCHILLED(0xFF1E90FF, t -> t <= ReactorKettleThermoregulator.SUPERCHILLED),
    CHILLED(0xFF87CEEB, t -> t > ReactorKettleThermoregulator.SUPERCHILLED && t <= ReactorKettleThermoregulator.CHILLED),
    NONE(0xFFFFFFFF, t -> t > ReactorKettleThermoregulator.CHILLED && t < ReactorKettleThermoregulator.HEATED),
    HEATED(0xFFFFA500, t -> t >= ReactorKettleThermoregulator.HEATED && t < ReactorKettleThermoregulator.SUPERHEATED),
    SUPERHEATED(0xFFFF4500, t -> t >= ReactorKettleThermoregulator.SUPERHEATED);

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
    public @NotNull String getSerializedName() {
        return Lang.asId(name());
    }

    public @NotNull String getTranslationKey() {
        return "recipe.temperature_condition." + getSerializedName();
    }

    public int getColor() {
        return color;
    }

    public boolean test(float temperature) {
        return function.apply(temperature);
    }
}
