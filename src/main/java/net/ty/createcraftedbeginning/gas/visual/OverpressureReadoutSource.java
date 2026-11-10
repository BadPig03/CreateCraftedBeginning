package net.ty.createcraftedbeginning.gas.visual;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.ty.createcraftedbeginning.gas.visual.OverpressureTooltips.Snapshot;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public interface OverpressureReadoutSource {
    static @Nullable Snapshot readout(BlockEntity owner) {
        if (!(owner instanceof OverpressureReadoutSource source)) {
            return null;
        }

        return source.getOverpressureReadout();
    }

    @Nullable Snapshot getOverpressureReadout();
}
