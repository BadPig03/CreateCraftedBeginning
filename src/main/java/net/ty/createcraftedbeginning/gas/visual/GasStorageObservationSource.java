package net.ty.createcraftedbeginning.gas.visual;

import net.createmod.catnip.data.Iterate;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.ty.createcraftedbeginning.api.gas.GasCapabilities;
import net.ty.createcraftedbeginning.api.gas.handler.GasHandler;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public interface GasStorageObservationSource {
    static @Nullable BlockEntity owner(@Nullable BlockEntity observed) {
        if (observed == null || observed.isRemoved()) {
            return null;
        }

        if (observed instanceof GasStorageObservationSource source) {
            return source.getGasStorageOwner();
        }

        return observed;
    }

    static Set<GasHandler> handlers(Level level, BlockPos pos, BlockEntity observed) {
        Set<GasHandler> handlers = new HashSet<>();
        boolean includeCapabilities = !(observed instanceof GasStorageObservationSource source) || source.includeGasStorageCapabilities();
        if (includeCapabilities) {
            for (Direction direction : Iterate.directions) {
                GasHandler handler = level.getCapability(GasCapabilities.BLOCK, pos, direction);
                if (handler == null) {
                    continue;
                }

                handlers.add(handler);
            }
        }
        if (observed instanceof GasStorageObservationSource source) {
            handlers.addAll(source.getAdditionalObservedGasHandlers());
        }
        return handlers;
    }

    @Nullable BlockEntity getGasStorageOwner();

    default boolean includeGasStorageCapabilities() {
        return true;
    }

    default List<GasHandler> getAdditionalObservedGasHandlers() {
        return List.of();
    }
}
