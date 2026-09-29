package net.ty.createcraftedbeginning.content.airtights.airtighthatch;

import com.simibubi.create.content.kinetics.mechanicalArm.ArmBlockEntity;
import com.simibubi.create.content.kinetics.mechanicalArm.ArmInteractionPoint;
import com.simibubi.create.content.kinetics.mechanicalArm.ArmInteractionPointType;
import net.createmod.catnip.math.VecHelper;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.ty.createcraftedbeginning.registry.CCBSoundEvents;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class AirtightHatchInteractionPoint extends ArmInteractionPoint {
    private AirtightHatchInteractionPoint(ArmInteractionPointType type, Level level, BlockPos pos, BlockState state) {
        super(type, level, pos, state);
    }

    @Override
    protected Vec3 getInteractionPositionVector() {
        Direction exposedFace = getInteractionDirection();
        return VecHelper.getCenterOf(pos).add(Vec3.atLowerCornerOf(exposedFace.getNormal()).scale(0.45));
    }

    @Override
    protected Direction getInteractionDirection() {
        return cachedState.getValue(AirtightHatchBlock.FACING).getOpposite();
    }

    @Override
    public ItemStack insert(ArmBlockEntity arm, ItemStack stack, boolean simulate) {
        if (stack.isEmpty()) {
            return stack;
        }

        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (!(blockEntity instanceof AirtightHatchBlockEntity hatch) || !hatch.canInstallCanisterFromAutomation(stack)) {
            return stack;
        }

        ItemStack remainder = stack.copy();
        remainder.shrink(1);
        if (simulate) {
            return remainder;
        }

        ItemStack canisterToInstall = stack.copyWithCount(1);
        if (!hatch.installCanister(canisterToInstall)) {
            return stack;
        }

        CCBSoundEvents.CANISTER_ADDED.playOnServer(level, pos, 1, 1);
        return remainder;
    }

    @Override
    public ItemStack extract(ArmBlockEntity arm, int slot, int amount, boolean simulate) {
        if (slot != 0 || amount <= 0) {
            return ItemStack.EMPTY;
        }

        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (!(blockEntity instanceof AirtightHatchBlockEntity hatch) || hatch.isEmpty()) {
            return ItemStack.EMPTY;
        }

        ItemStack canister = hatch.createCanisterItemStack();
        if (canister.isEmpty()) {
            return ItemStack.EMPTY;
        }

        if (simulate) {
            return canister;
        }

        ItemStack removed = hatch.removeCanisterForAutomation();
        if (!removed.isEmpty()) {
            CCBSoundEvents.CANISTER_REMOVED.playOnServer(level, pos, 1, 1);
        }
        return removed;
    }

    @Override
    public int getSlotCount(ArmBlockEntity arm) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (!(blockEntity instanceof AirtightHatchBlockEntity hatch && !hatch.isEmpty())) {
            return 0;
        }

        return 1;
    }

    public static class AirtightHatchType extends ArmInteractionPointType {
        @Override
        public boolean canCreatePoint(Level level, BlockPos pos, BlockState state) {
            return state.getBlock() instanceof AirtightHatchBlock;
        }

        @Override
        public ArmInteractionPoint createPoint(Level level, BlockPos pos, BlockState state) {
            return new AirtightHatchInteractionPoint(this, level, pos, state);
        }
    }
}
