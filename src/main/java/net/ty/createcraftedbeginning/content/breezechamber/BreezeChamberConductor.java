package net.ty.createcraftedbeginning.content.breezechamber;

import com.simibubi.create.api.behaviour.interaction.ConductorBlockInteractionBehavior;
import net.minecraft.world.level.block.state.BlockState;

public class BreezeChamberConductor {
    public static class BreezeChamber extends ConductorBlockInteractionBehavior {
		@Override
		public boolean isValidConductor(BlockState state) {
			return true;
		}
	}
}
