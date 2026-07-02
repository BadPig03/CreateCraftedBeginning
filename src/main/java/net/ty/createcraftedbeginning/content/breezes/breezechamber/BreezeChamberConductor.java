package net.ty.createcraftedbeginning.content.breezes.breezechamber;

import com.simibubi.create.api.behaviour.interaction.ConductorBlockInteractionBehavior;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class BreezeChamberConductor {
    public static class BreezeChamber extends ConductorBlockInteractionBehavior {
        @Override
        public boolean isValidConductor(BlockState state) {
            return true;
        }
    }
}
