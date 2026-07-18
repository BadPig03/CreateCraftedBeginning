package net.ty.createcraftedbeginning.mixin.server.create;

import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelBehaviour;
import com.tterrag.registrate.util.entry.BlockEntry;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.level.block.state.BlockState;
import net.ty.createcraftedbeginning.content.airtights.gasfactorygauge.GasFactoryGaugeBehaviour;
import net.ty.createcraftedbeginning.content.airtights.gasfactorygauge.GasFactoryGaugeBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@Mixin(value = FactoryPanelBehaviour.class, remap = false)
public abstract class FactoryPanelBehaviourMixin {
    @SuppressWarnings("DataFlowIssue")
    @Inject(method = "tryRestock", at = @At("HEAD"), cancellable = true)
    private void ccb$tryRestock(CallbackInfo ci) {
        FactoryPanelBehaviour behaviour = (FactoryPanelBehaviour) (Object) this;
        if (!(behaviour instanceof GasFactoryGaugeBehaviour gasBehaviour)) {
            return;
        }

        gasBehaviour.performGasRestock();
        ci.cancel();
    }

    @SuppressWarnings("MethodMayBeStatic")
    @Redirect(method = "moveTo", at = @At(value = "INVOKE", target = "Lcom/tterrag/registrate/util/entry/BlockEntry;has(Lnet/minecraft/world/level/block/state/BlockState;)Z"))
    private boolean ccb$moveTo(BlockEntry<?> entry, BlockState state) {
        return entry.has(state) || state.getBlock() instanceof GasFactoryGaugeBlock;
    }
}
