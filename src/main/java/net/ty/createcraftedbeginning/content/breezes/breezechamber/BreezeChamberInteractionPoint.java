package net.ty.createcraftedbeginning.content.breezes.breezechamber;

import com.simibubi.create.content.kinetics.mechanicalArm.AllArmInteractionPointTypes.DepositOnlyArmInteractionPoint;
import com.simibubi.create.content.kinetics.mechanicalArm.ArmBlockEntity;
import com.simibubi.create.content.kinetics.mechanicalArm.ArmInteractionPoint;
import com.simibubi.create.content.kinetics.mechanicalArm.ArmInteractionPointType;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class BreezeChamberInteractionPoint extends DepositOnlyArmInteractionPoint {
    private BreezeChamberInteractionPoint(ArmInteractionPointType type, Level level, BlockPos pos, BlockState state) {
        super(type, level, pos, state);
    }

    @Override
    public ItemStack insert(ArmBlockEntity armBlockEntity, ItemStack stack, boolean simulate) {
        ItemStack inputStack = stack.copy();
        InteractionResultHolder<ItemStack> insertionResult = BreezeChamberBlock.tryInsert(level, pos, inputStack, false, false, simulate);
        ItemStack remainder = insertionResult.getObject();
        if (simulate && insertionResult.getResult().consumesAction()) {
            inputStack.shrink(1);
        }
        if (inputStack.isEmpty()) {
            return remainder;
        }

        if (simulate) {
            return inputStack;
        }

        Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), remainder);
        return inputStack;
    }

    public static class BreezeChamberType extends ArmInteractionPointType {
        @Override
        public boolean canCreatePoint(Level level, BlockPos pos, BlockState state) {
            return state.getBlock() instanceof BreezeChamberBlock;
        }

        @Override
        public ArmInteractionPoint createPoint(Level level, BlockPos pos, BlockState state) {
            return new BreezeChamberInteractionPoint(this, level, pos, state);
        }
    }
}
