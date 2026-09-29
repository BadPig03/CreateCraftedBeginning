package net.ty.createcraftedbeginning.mixin.common.create;

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
import net.ty.createcraftedbeginning.content.airtights.airtightforgingpress.AirtightForgingPressStructuralBlockEntity;
import net.ty.createcraftedbeginning.content.airtights.airtightforgingpress.AirtightForgingPressStructuralShaftBlockEntity;
import net.ty.createcraftedbeginning.content.airtights.airtightreactorkettle.AirtightReactorKettleStructuralBlockEntity;
import net.ty.createcraftedbeginning.gas.behaviour.GasManipulationBehaviour;
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

    private ThresholdSwitchBlockEntityMixin(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Shadow
    protected abstract BlockPos getTargetPos();

    @Inject(method = "addBehaviours", at = @At("TAIL"))
    private void ccb$addGasBehaviour(List<BlockEntityBehaviour> behaviours, CallbackInfo callback) {
        ccb$observedGasTank = new GasManipulationBehaviour(this, (level, pos, state) -> new BlockFace(pos, DirectedDirectionalBlock.getTargetDirection(state))).bypassSidedness();
        behaviours.add(ccb$observedGasTank);
    }

    @Inject(method = "updateCurrentLevel", at = @At("HEAD"))
    private void ccb$updateCurrentLevel(CallbackInfo callback) {
        ccb$observedGasTank.findNewCapability();
    }

    @Inject(method = "getDisplayItemForScreen", at = @At("HEAD"), cancellable = true)
    private void ccb$getDisplayItemForScreen(CallbackInfoReturnable<ItemStack> callback) {
        Level level = getLevel();
        if (level == null) {
            return;
        }

        BlockPos pos = getTargetPos();
        BlockEntity blockEntity = level.getBlockEntity(pos);
        BlockState state = level.getBlockState(pos);
        switch (blockEntity) {
            case AirtightReactorKettleStructuralBlockEntity ignored when AirtightReactorKettleStructuralBlockEntity.canStore(state) -> callback.setReturnValue(new ItemStack(CCBBlocks.AIRTIGHT_REACTOR_KETTLE_BLOCK));
            case AirtightForgingPressStructuralBlockEntity ignored when AirtightForgingPressStructuralBlockEntity.isLowerStore(state) -> callback.setReturnValue(new ItemStack(CCBBlocks.AIRTIGHT_FORGING_PRESS_BLOCK));
            case AirtightForgingPressStructuralShaftBlockEntity ignored when AirtightForgingPressStructuralShaftBlockEntity.isUpperStore(state) -> callback.setReturnValue(new ItemStack(CCBBlocks.AIRTIGHT_FORGING_PRESS_BLOCK));
            case null, default -> {
            }
        }
    }
}
