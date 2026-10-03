package net.ty.createcraftedbeginning.recipe.pressure;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.ty.createcraftedbeginning.api.gas.GasPressure;
import org.jetbrains.annotations.Contract;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Optional;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public record PressureRequirement(Optional<Long> minimumPressurePa, Optional<Long> maximumPressurePa) {
    public static final PressureRequirement NONE = new PressureRequirement(Optional.empty(), Optional.empty());
    public static final StreamCodec<RegistryFriendlyByteBuf, PressureRequirement> STREAM_CODEC = StreamCodec.of((buffer, requirement) -> {
        writeOptionalPressure(buffer, requirement.minimumPressurePa);
        writeOptionalPressure(buffer, requirement.maximumPressurePa);
    }, buffer -> new PressureRequirement(readOptionalPressure(buffer), readOptionalPressure(buffer)));
    private static final Codec<Long> NON_NEGATIVE_PRESSURE_CODEC = Codec.LONG.validate(pressurePa -> pressurePa < GasPressure.VACUUM_PA ? DataResult.error(() -> "Pressure must be non-negative; got " + pressurePa + " Pa.") : DataResult.success(pressurePa));
    public static final MapCodec<PressureRequirement> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(NON_NEGATIVE_PRESSURE_CODEC.optionalFieldOf("minimum_pressure").forGetter(PressureRequirement::minimumPressurePa), NON_NEGATIVE_PRESSURE_CODEC.optionalFieldOf("maximum_pressure").forGetter(PressureRequirement::maximumPressurePa)).apply(instance, PressureRequirement::new));
    public static final Codec<PressureRequirement> CODEC = MAP_CODEC.codec();

    public PressureRequirement {
        minimumPressurePa.ifPresent(pressurePa -> validatePressure("Minimum", pressurePa));
        maximumPressurePa.ifPresent(pressurePa -> validatePressure("Maximum", pressurePa));
        if (minimumPressurePa.isPresent() && maximumPressurePa.isPresent() && minimumPressurePa.get() > maximumPressurePa.get()) {
            throw new IllegalArgumentException("Minimum pressure must not exceed maximum pressure; got minimum=" + minimumPressurePa.get() + " Pa, maximum=" + maximumPressurePa.get() + " Pa.");
        }
    }

    public static PressureRequirement atLeast(long minimumPressurePa) {
        return new PressureRequirement(Optional.of(minimumPressurePa), Optional.empty());
    }

    public static PressureRequirement atMost(long maximumPressurePa) {
        return new PressureRequirement(Optional.empty(), Optional.of(maximumPressurePa));
    }

    @Contract("_, _ -> new")
    public static PressureRequirement between(long minimumPressurePa, long maximumPressurePa) {
        return new PressureRequirement(Optional.of(minimumPressurePa), Optional.of(maximumPressurePa));
    }

    private static void validatePressure(String boundaryName, long pressurePa) {
        if (pressurePa >= GasPressure.VACUUM_PA) {
            return;
        }

        throw new IllegalArgumentException(boundaryName + " pressure must be non-negative; got " + pressurePa + " Pa.");
    }

    private static void writeOptionalPressure(RegistryFriendlyByteBuf buffer, Optional<Long> pressurePa) {
        buffer.writeBoolean(pressurePa.isPresent());
        pressurePa.ifPresent(pressure -> ByteBufCodecs.VAR_LONG.encode(buffer, pressure));
    }

    private static Optional<Long> readOptionalPressure(RegistryFriendlyByteBuf buffer) {
        if (!buffer.readBoolean()) {
            return Optional.empty();
        }

        return Optional.of(ByteBufCodecs.VAR_LONG.decode(buffer));
    }

    public boolean hasMinimumPressure() {
        return minimumPressurePa.isPresent();
    }

    public boolean hasMaximumPressure() {
        return maximumPressurePa.isPresent();
    }

    public long minimumPressurePaOrVacuum() {
        return minimumPressurePa.orElse(GasPressure.VACUUM_PA);
    }

    public long maximumPressurePaOrUnbounded() {
        return maximumPressurePa.orElse(Long.MAX_VALUE);
    }

    public boolean allowsPressure(long pressurePa) {
        return pressurePa >= GasPressure.VACUUM_PA && pressurePa >= minimumPressurePaOrVacuum() && pressurePa <= maximumPressurePaOrUnbounded();
    }
}
