package net.ty.createcraftedbeginning.api.gas.gases.flowsources;

import net.createmod.catnip.math.BlockFace;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class BlockedSource extends GasFlowSource {
    public BlockedSource(BlockFace location) {
        super(location);
    }

    @Override
    public boolean isEndpoint() {
        return false;
    }

    @Override
    public void manageSource(Level level, BlockEntity networkBE) {
    }
}
