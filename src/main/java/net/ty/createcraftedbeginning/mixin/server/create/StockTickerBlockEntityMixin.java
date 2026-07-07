package net.ty.createcraftedbeginning.mixin.server.create;

import com.simibubi.create.content.logistics.stockTicker.StockTickerBlockEntity;
import net.createmod.catnip.data.Iterate;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.ty.createcraftedbeginning.content.breezes.breezecooler.BreezeCoolerBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@Mixin(value = StockTickerBlockEntity.class, remap = false)
public abstract class StockTickerBlockEntityMixin {
    @SuppressWarnings("DataFlowIssue")
    @Inject(method = "isKeeperPresent", at = @At("RETURN"), cancellable = true)
    private void ccb$isKeeperPresent(CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValue()) {
            return;
        }

        StockTickerBlockEntity be = (StockTickerBlockEntity) (Object) this;
        Level level = be.getLevel();
        if (level == null) {
            return;
        }

        for (Direction side : Iterate.horizontalDirections) {
            if (!(level.getBlockEntity(be.getBlockPos().relative(side)) instanceof BreezeCoolerBlockEntity)) {
                continue;
            }

            cir.setReturnValue(true);
            return;
        }
    }
}
