package net.ty.createcraftedbeginning.mixin.common.create;

import com.simibubi.create.api.packager.InventoryIdentifier;
import com.simibubi.create.content.logistics.packagerLink.LogisticallyLinkedBehaviour;
import com.simibubi.create.content.logistics.packagerLink.LogisticsManager;
import com.simibubi.create.content.logistics.packagerLink.PackagerLinkBlockEntity;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.ty.createcraftedbeginning.content.airtights.gaspackager.GasPackagerBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@Mixin(value = LogisticsManager.class, remap = false)
public abstract class LogisticsManagerMixin {
    @Inject(method = "getInventoryIdentifierFromLink", at = @At("HEAD"), cancellable = true)
    private static void ccb$getInventoryIdentifierFromLink(LogisticallyLinkedBehaviour link, CallbackInfoReturnable<InventoryIdentifier> callback) {
        if (!(link.blockEntity instanceof PackagerLinkBlockEntity blockEntity) || !(blockEntity.getPackager() instanceof GasPackagerBlockEntity gasPackager)) {
            return;
        }

        callback.setReturnValue(gasPackager.getGasInventoryIdentifier());
    }
}
