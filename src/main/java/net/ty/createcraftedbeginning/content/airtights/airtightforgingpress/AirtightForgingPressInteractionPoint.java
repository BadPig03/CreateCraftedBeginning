package net.ty.createcraftedbeginning.content.airtights.airtightforgingpress;

import com.simibubi.create.content.kinetics.mechanicalArm.ArmInteractionPoint;
import com.simibubi.create.content.kinetics.mechanicalArm.ArmInteractionPointType;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.ty.createcraftedbeginning.registry.CCBBlocks;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class AirtightForgingPressInteractionPoint extends ArmInteractionPoint {
    public AirtightForgingPressInteractionPoint(ArmInteractionPointType type, Level level, BlockPos pos, BlockState state) {
        super(type, level, pos, state);
    }

    public static class ForgingPressType extends ArmInteractionPointType {
        @Override
        public boolean canCreatePoint(Level level, BlockPos pos, BlockState state) {
            if (state.is(CCBBlocks.AIRTIGHT_FORGING_PRESS_BLOCK)) {
                return true;
            }
            else if (state.is(CCBBlocks.AIRTIGHT_FORGING_PRESS_STRUCTURAL_BLOCK)) {
                return state.getValue(AirtightForgingPressStructuralBlock.STRUCTURAL_POSITION).isLowerStore();
            }
            else if (state.is(CCBBlocks.AIRTIGHT_FORGING_PRESS_STRUCTURAL_SHAFT_BLOCK)) {
                return state.getValue(AirtightForgingPressStructuralShaftBlock.STRUCTURAL_POSITION) == AirtightForgingPressStructuralPosition.TOP_CENTER;
            }
            return false;
        }

        @Override
        public ArmInteractionPoint createPoint(Level level, BlockPos pos, BlockState state) {
            return new AirtightForgingPressInteractionPoint(this, level, pos, state);
        }
    }
}
