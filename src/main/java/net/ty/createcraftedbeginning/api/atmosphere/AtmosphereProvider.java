package net.ty.createcraftedbeginning.api.atmosphere;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.ty.createcraftedbeginning.api.gas.Gas;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.OptionalLong;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public interface AtmosphereProvider {
    default @Nullable Gas resolveComposition(Level level, BlockPos pos) {
        return null;
    }

    default OptionalLong resolvePressurePa(Level level, BlockPos pos) {
        return OptionalLong.empty();
    }
}
