package net.ty.createcraftedbeginning.content.airtights.airtightreactorkettle;

import com.simibubi.create.content.kinetics.mechanicalArm.ArmInteractionPoint;
import com.simibubi.create.content.kinetics.mechanicalArm.ArmInteractionPointType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.ty.createcraftedbeginning.registry.CCBBlocks;
import org.jetbrains.annotations.NotNull;

public class AirtightReactorKettleInteractionPoint extends ArmInteractionPoint {
    public AirtightReactorKettleInteractionPoint(ArmInteractionPointType type, Level level, BlockPos pos, BlockState state) {
        super(type, level, pos, state);
    }

    public static class ReactorKettleType extends ArmInteractionPointType {
		@Override
		public boolean canCreatePoint(Level level, BlockPos pos, @NotNull BlockState state) {
			return state.is(CCBBlocks.AIRTIGHT_REACTOR_KETTLE_STRUCTURAL_BLOCK) && state.getValue(AirtightReactorKettleStructuralBlock.STRUCTURAL_POSITION).canStore();
		}

		@Override
		public ArmInteractionPoint createPoint(Level level, BlockPos pos, BlockState state) {
			return new AirtightReactorKettleInteractionPoint(this, level, pos, state);
		}
	}
}
