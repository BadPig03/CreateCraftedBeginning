package net.ty.createcraftedbeginning.api.gas.recipes;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import net.createmod.catnip.codecs.stream.CatnipStreamCodecBuilders;
import net.createmod.catnip.lang.Lang;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.StringRepresentable;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public enum TemperatureMatching implements StringRepresentable {
    EXACT,
    COMPATIBLE;

    public static final Codec<TemperatureMatching> CODEC = StringRepresentable.fromEnum(TemperatureMatching::values);
    public static final StreamCodec<ByteBuf, TemperatureMatching> STREAM_CODEC = CatnipStreamCodecBuilders.ofEnum(TemperatureMatching.class);

    public static int getMatchPriority(TemperatureMatching currentMatching, TemperatureCondition requiredCondition, float temperature) {
        if (!currentMatching.isValidFor(requiredCondition)) {
            return 0;
        }

        if (requiredCondition.test(temperature)) {
            return 3;
        }

        if (currentMatching == EXACT) {
            return 0;
        }

        TemperatureCondition actualCondition = TemperatureCondition.getConditionByTemperature(temperature);
        return switch (requiredCondition) {
            case NONE -> 1;
            case HEATED -> actualCondition == TemperatureCondition.SUPERHEATED ? 2 : 0;
            case CHILLED -> actualCondition == TemperatureCondition.SUPERCHILLED ? 2 : 0;
            case SUPERHEATED, SUPERCHILLED -> 0;
        };
    }

    public boolean isValidFor(TemperatureCondition requiredCondition) {
        return this != COMPATIBLE || requiredCondition.supportsCompatibleMatching();
    }

    @Override
    public String getSerializedName() {
        return Lang.asId(name());
    }

    public boolean test(TemperatureCondition requiredCondition, float temperature) {
        return getMatchPriority(this, requiredCondition, temperature) > 0;
    }
}
