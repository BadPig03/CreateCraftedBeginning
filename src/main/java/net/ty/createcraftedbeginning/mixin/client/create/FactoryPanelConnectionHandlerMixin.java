package net.ty.createcraftedbeginning.mixin.client.create;

import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelBehaviour;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelBlock;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelConnectionHandler;
import com.tterrag.registrate.util.entry.BlockEntry;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.ty.createcraftedbeginning.content.airtights.gasfactorygauge.GasFactoryGaugeBlock;
import net.ty.createcraftedbeginning.content.airtights.gaspackager.GasPackagerBlock;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@Mixin(value = FactoryPanelConnectionHandler.class, remap = false)
public abstract class FactoryPanelConnectionHandlerMixin {
    @Redirect(method = "clientTick", at = @At(value = "INVOKE", target = "Lcom/tterrag/registrate/util/entry/BlockEntry;has(Lnet/minecraft/world/level/block/state/BlockState;)Z"))
    private static boolean ccb$clientTick(BlockEntry<?> entry, BlockState state) {
        return entry.has(state) || state.getBlock() instanceof GasPackagerBlock;
    }

    @Inject(method = "checkForIssues(Lcom/simibubi/create/content/logistics/factoryBoard/FactoryPanelBehaviour;Lcom/simibubi/create/content/logistics/factoryBoard/FactoryPanelBehaviour;)Ljava/lang/String;", at = @At("HEAD"), cancellable = true)
    private static void ccb$checkForIssues(@Nullable FactoryPanelBehaviour from, @Nullable FactoryPanelBehaviour to, CallbackInfoReturnable<String> cir) {
        if (from == null || to == null) {
            return;
        }

        BlockState toState = to.getWorld().getBlockState(to.getPos());
        BlockState fromState = from.getWorld().getBlockState(from.getPos());
        boolean toIsGasGauge = toState.getBlock() instanceof GasFactoryGaugeBlock;
        boolean fromIsGasGauge = fromState.getBlock() instanceof GasFactoryGaugeBlock;
        if (toIsGasGauge == fromIsGasGauge) {
            return;
        }

        if (!(toState.getBlock() instanceof FactoryPanelBlock) || !(fromState.getBlock() instanceof FactoryPanelBlock)) {
            return;
        }

        if (from.targetedBy.containsKey(to.getPanelPosition())) {
            cir.setReturnValue("factory_panel.already_connected");
            return;
        }

        if (from.targetedBy.size() >= 9) {
            cir.setReturnValue("factory_panel.cannot_add_more_inputs");
            return;
        }

        if (toState.getValue(FactoryPanelBlock.FACE) != fromState.getValue(FactoryPanelBlock.FACE) || toState.getValue(FactoryPanelBlock.FACING) != fromState.getValue(FactoryPanelBlock.FACING)) {
            cir.setReturnValue("factory_panel.same_orientation");
            return;
        }

        BlockPos diff = to.getPos().subtract(from.getPos());
        if (FactoryPanelBlock.connectedDirection(toState).getAxis().choose(diff.getX(), diff.getY(), diff.getZ()) != 0) {
            cir.setReturnValue("factory_panel.same_surface");
            return;
        }

        if (!diff.closerThan(BlockPos.ZERO, 16)) {
            cir.setReturnValue("factory_panel.too_far_apart");
            return;
        }

        if (to.panelBE().restocker) {
            cir.setReturnValue("factory_panel.input_in_restock_mode");
            return;
        }

        if (to.getFilter().isEmpty() || from.getFilter().isEmpty()) {
            cir.setReturnValue("factory_panel.no_item");
            return;
        }

        cir.setReturnValue(null);
    }
}