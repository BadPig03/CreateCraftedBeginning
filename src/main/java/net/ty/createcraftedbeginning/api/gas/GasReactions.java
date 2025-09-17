package net.ty.createcraftedbeginning.api.gas;

import com.simibubi.create.foundation.utility.BlockHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.NeoForge;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("unused")
public class GasReactions {
    public static void handlePipeFlowCollision(Level level, BlockPos pos, @NotNull GasStack gas1, @NotNull GasStack gas2) {
        Gas g1 = gas1.getGas();
        Gas g2 = gas2.getGas();

        BlockHelper.destroyBlock(level, pos, 1);

        GasCollisionEvent.Flow event = new GasCollisionEvent.Flow(level, pos, g1, g2, null);
        NeoForge.EVENT_BUS.post(event);
        if (event.getState() != null) {
            level.setBlockAndUpdate(pos, event.getState());
        }
    }
}
