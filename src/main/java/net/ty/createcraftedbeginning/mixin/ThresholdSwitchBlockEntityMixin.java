package net.ty.createcraftedbeginning.mixin;

import com.simibubi.create.content.redstone.thresholdSwitch.ThresholdSwitchBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
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
        if (!(switchBlockEntity.getLevel().getBlockEntity(getTargetPos()) instanceof AirtightReactorKettleStructuralBlockEntity structuralBlockEntity) || !structuralBlockEntity.canStore()) {
            return;
        }

        cir.setReturnValue(CCBBlocks.AIRTIGHT_REACTOR_KETTLE_BLOCK.asStack());
    }
}
