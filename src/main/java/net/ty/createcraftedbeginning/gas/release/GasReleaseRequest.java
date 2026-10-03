package net.ty.createcraftedbeginning.gas.release;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.ty.createcraftedbeginning.api.gas.GasPressure;
import net.ty.createcraftedbeginning.api.gas.GasStack;
import net.ty.createcraftedbeginning.api.gasreleasehandlers.GasReleaseCause;
import net.ty.createcraftedbeginning.api.gasreleasehandlers.GasReleaseMode;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Optional;
import java.util.OptionalLong;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public record GasReleaseRequest(GasStack gas, BlockPos sourcePos, BlockPos effectPos, Optional<Direction> direction, GasReleaseCause cause, GasReleaseMode mode, OptionalLong sourcePressurePa) {
    public GasReleaseRequest {
        gas = gas.copy();
        sourcePos = sourcePos.immutable();
        effectPos = effectPos.immutable();
        if (sourcePressurePa.isPresent() && sourcePressurePa.getAsLong() < GasPressure.VACUUM_PA) {
            throw new IllegalArgumentException("Gas release source pressure must be non-negative; got " + sourcePressurePa.getAsLong() + " Pa.");
        }

        if (mode == GasReleaseMode.DIRECTIONAL && direction.isEmpty()) {
            throw new IllegalArgumentException("Directional gas release requires a direction.");
        }

        if (mode == GasReleaseMode.DIRECTIONAL && !effectPos.equals(sourcePos.relative(direction.orElseThrow()))) {
            throw new IllegalArgumentException("Directional gas release effect position must match its source and direction; got source=" + sourcePos + ", direction=" + direction.orElseThrow() + ", effect=" + effectPos + '.');
        }

        if (mode == GasReleaseMode.RADIAL && direction.isPresent()) {
            throw new IllegalArgumentException("Radial gas release cannot have a direction; got " + direction.orElseThrow() + '.');
        }

        if (mode == GasReleaseMode.RADIAL && !effectPos.equals(sourcePos)) {
            throw new IllegalArgumentException("Radial gas release effect position must match its source; got source=" + sourcePos + ", effect=" + effectPos + '.');
        }
    }

    public static GasReleaseRequest directional(GasStack gas, BlockPos sourcePos, Direction direction, GasReleaseCause cause) {
        return directional(gas, sourcePos, direction, cause, OptionalLong.empty());
    }

    public static GasReleaseRequest directional(GasStack gas, BlockPos sourcePos, Direction direction, GasReleaseCause cause, long sourcePressurePa) {
        return directional(gas, sourcePos, direction, cause, OptionalLong.of(sourcePressurePa));
    }

    public static GasReleaseRequest radial(GasStack gas, BlockPos sourcePos, GasReleaseCause cause) {
        return radial(gas, sourcePos, cause, OptionalLong.empty());
    }

    public static GasReleaseRequest radial(GasStack gas, BlockPos sourcePos, GasReleaseCause cause, long sourcePressurePa) {
        return radial(gas, sourcePos, cause, OptionalLong.of(sourcePressurePa));
    }

    private static GasReleaseRequest directional(GasStack gas, BlockPos sourcePos, Direction direction, GasReleaseCause cause, OptionalLong sourcePressurePa) {
        return new GasReleaseRequest(gas, sourcePos, sourcePos.relative(direction), Optional.of(direction), cause, GasReleaseMode.DIRECTIONAL, sourcePressurePa);
    }

    private static GasReleaseRequest radial(GasStack gas, BlockPos sourcePos, GasReleaseCause cause, OptionalLong sourcePressurePa) {
        return new GasReleaseRequest(gas, sourcePos, sourcePos, Optional.empty(), cause, GasReleaseMode.RADIAL, sourcePressurePa);
    }

    @Override
    public GasStack gas() {
        return gas.copy();
    }
}
