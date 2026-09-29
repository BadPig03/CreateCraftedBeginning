package net.ty.createcraftedbeginning.content.breezes.breezecooler;

import com.simibubi.create.content.processing.basin.BasinBlockEntity;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.ty.createcraftedbeginning.content.breezes.breezecooler.BreezeCoolerBlock.FrostLevel;
import net.ty.createcraftedbeginning.platform.SubLevelBridge;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class BreezeCoolerBasinCooling {
    private BreezeCoolerBasinCooling() {
    }

    public static boolean hasChilledSource(BasinBlockEntity basin) {
        Level level = basin.getLevel();
        if (level == null) {
            return false;
        }

        BlockPos pos = basin.getBlockPos();
        return SubLevelBridge.findAt(level, pos, pos.below(), source -> {
            if (!(level.getBlockEntity(source) instanceof BreezeCoolerBlockEntity cooler) || cooler.getFrostLevel() != FrostLevel.CHILLED) {
                return null;
            }

            return cooler;
        }) != null;
    }
}
