package net.ty.createcraftedbeginning.mixin.server.create;

import com.simibubi.create.content.processing.basin.BasinBlockEntity;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.ty.createcraftedbeginning.content.airtights.gasinjectionchamber.GasInjectionChamberBasinIntegration;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@Mixin(value = BasinBlockEntity.class, remap = false)
public abstract class BasinBlockEntityMixin {
    @SuppressWarnings("DataFlowIssue")
    @Inject(method = "tick", at = @At(value = "INVOKE", target = "Ljava/util/Optional;ifPresent(Ljava/util/function/Consumer;)V", ordinal = 0), require = 0)
    private void ccb$tick(CallbackInfo ci) {
        GasInjectionChamberBasinIntegration.onBasinContentsChanged((BasinBlockEntity) (Object) this);
    }
}
