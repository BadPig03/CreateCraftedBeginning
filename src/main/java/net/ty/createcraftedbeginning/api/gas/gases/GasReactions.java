package net.ty.createcraftedbeginning.api.gas.gases;

import com.simibubi.create.foundation.utility.BlockHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.NeoForge;
import net.ty.createcraftedbeginning.api.gas.gases.GasCollisionEvent.Flow;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("unused")
public class GasReactions {
    public static void handlePipeFlowCollision(Level level, BlockPos pos, @NotNull GasStack gas1, @NotNull GasStack gas2) {
        BlockHelper.destroyBlock(level, pos, 1);
        Flow event = new Flow(level, pos, gas1.getGasType(), gas2.getGasType(), null);
        NeoForge.EVENT_BUS.post(event);
        if (event.getState() == null) {
            return;
        }

        level.setBlockAndUpdate(pos, event.getState());
    }
}
