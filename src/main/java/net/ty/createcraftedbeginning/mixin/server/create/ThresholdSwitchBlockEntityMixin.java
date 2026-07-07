package net.ty.createcraftedbeginning.mixin.server.create;

import com.simibubi.create.content.redstone.DirectedDirectionalBlock;
import com.simibubi.create.content.redstone.thresholdSwitch.ThresholdSwitchBlockEntity;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.createmod.catnip.math.BlockFace;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.ty.createcraftedbeginning.api.gas.gases.behaviours.GasManipulationBehaviour;
import net.ty.createcraftedbeginning.content.airtights.airtightforgingpress.AirtightForgingPressStructuralBlockEntity;
import net.ty.createcraftedbeginning.content.airtights.airtightforgingpress.AirtightForgingPressStructuralShaftBlockEntity;
import net.ty.createcraftedbeginning.content.airtights.airtightreactorkettle.AirtightReactorKettleStructuralBlockEntity;
import net.ty.createcraftedbeginning.registry.CCBBlocks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@Mixin(value = ThresholdSwitchBlockEntity.class, remap = false)
public abstract class ThresholdSwitchBlockEntityMixin extends SmartBlockEntity {
    @Unique
    private GasManipulationBehaviour ccb$observedGasTank;

    @Shadow
    protected abstract BlockPos getTargetPos();

    private ThresholdSwitchBlockEntityMixin(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Inject(method = "addBehaviours", at = @At("TAIL"))
    private void ccb$addGasBehaviour(List<BlockEntityBehaviour> behaviours, CallbackInfo ci) {
        ccb$observedGasTank = new GasManipulationBehaviour(this, (w, p, s) -> new BlockFace(p, DirectedDirectionalBlock.getTargetDirection(s))).bypassSidedness();
        behaviours.add(ccb$observedGasTank);
    }

    @Inject(method = "updateCurrentLevel", at = @At("HEAD"))
    private void ccb$updateCurrentLevel(CallbackInfo ci) {
        ccb$observedGasTank.findNewCapability();
    }

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
