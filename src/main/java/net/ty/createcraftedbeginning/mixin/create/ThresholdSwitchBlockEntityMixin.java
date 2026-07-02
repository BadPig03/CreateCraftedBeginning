package net.ty.createcraftedbeginning.mixin.create;

import com.simibubi.create.content.redstone.thresholdSwitch.ThresholdSwitchBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.ty.createcraftedbeginning.content.airtights.airtightforgingpress.AirtightForgingPressStructuralBlockEntity;
import net.ty.createcraftedbeginning.content.airtights.airtightforgingpress.AirtightForgingPressStructuralShaftBlockEntity;
import net.ty.createcraftedbeginning.content.airtights.airtightreactorkettle.AirtightReactorKettleStructuralBlockEntity;
import net.ty.createcraftedbeginning.registry.CCBBlocks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ThresholdSwitchBlockEntity.class)
public abstract class ThresholdSwitchBlockEntityMixin {
    @Shadow
    abstract BlockPos getTargetPos();

    @SuppressWarnings("DataFlowIssue")
    @Inject(method = "getDisplayItemForScreen", at = @At("HEAD"), cancellable = true)
    private void ccb$getDisplayItemForScreen(CallbackInfoReturnable<ItemStack> cir) {
        ThresholdSwitchBlockEntity switchBlockEntity = (ThresholdSwitchBlockEntity) (Object) this;
        Level level = switchBlockEntity.getLevel();
        BlockPos pos = getTargetPos();
        BlockEntity blockEntity = level.getBlockEntity(pos);
        BlockState state = level.getBlockState(pos);
        if (blockEntity instanceof AirtightReactorKettleStructuralBlockEntity && AirtightReactorKettleStructuralBlockEntity.canStore(state)) {
            cir.setReturnValue(new ItemStack(CCBBlocks.AIRTIGHT_REACTOR_KETTLE_BLOCK));
        }
        else if (blockEntity instanceof AirtightForgingPressStructuralBlockEntity && AirtightForgingPressStructuralBlockEntity.isLowerStore(state)) {
            cir.setReturnValue(new ItemStack(CCBBlocks.AIRTIGHT_FORGING_PRESS_BLOCK));
        }
        else if (blockEntity instanceof AirtightForgingPressStructuralShaftBlockEntity && AirtightForgingPressStructuralShaftBlockEntity.isUpperStore(state)) {
            cir.setReturnValue(new ItemStack(CCBBlocks.AIRTIGHT_FORGING_PRESS_BLOCK));
        }
    }
}
