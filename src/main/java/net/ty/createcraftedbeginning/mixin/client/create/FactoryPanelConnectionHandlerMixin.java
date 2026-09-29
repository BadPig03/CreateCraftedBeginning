package net.ty.createcraftedbeginning.mixin.client.create;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelBehaviour;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelConnectionHandler;
import com.tterrag.registrate.util.entry.BlockEntry;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.level.block.state.BlockState;
import net.ty.createcraftedbeginning.content.airtights.gasfactorygauge.GasFactoryGaugeConnections;
import net.ty.createcraftedbeginning.content.airtights.gasfactorygauge.GasFactoryGaugeConnections.Check;
import net.ty.createcraftedbeginning.content.airtights.gaspackager.GasPackagerBlock;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@Mixin(value = FactoryPanelConnectionHandler.class, remap = false)
public abstract class FactoryPanelConnectionHandlerMixin {
    @WrapOperation(method = "clientTick", at = @At(value = "INVOKE", target = "Lcom/tterrag/registrate/util/entry/BlockEntry;has(Lnet/minecraft/world/level/block/state/BlockState;)Z"))
    private static boolean ccb$clientTick(BlockEntry<?> entry, BlockState state, Operation<Boolean> original) {
        return original.call(entry, state) || state.getBlock() instanceof GasPackagerBlock;
    }

    @Inject(method = "checkForIssues(Lcom/simibubi/create/content/logistics/factoryBoard/FactoryPanelBehaviour;Lcom/simibubi/create/content/logistics/factoryBoard/FactoryPanelBehaviour;)Ljava/lang/String;", at = @At("HEAD"), cancellable = true)
    private static void ccb$checkForIssues(@Nullable FactoryPanelBehaviour from, @Nullable FactoryPanelBehaviour to, CallbackInfoReturnable<String> callback) {
        Check check = GasFactoryGaugeConnections.check(from, to);
        if (check == null) {
            return;
        }

        callback.setReturnValue(check.issue());
    }
}
