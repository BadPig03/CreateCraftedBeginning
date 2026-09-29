package net.ty.createcraftedbeginning.mixin.common.create;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelBlockEntity;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelConfigurationPacket;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelPosition;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.server.level.ServerPlayer;
import net.ty.createcraftedbeginning.content.airtights.gasfactorygauge.GasFactoryGaugeConfiguration;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Map;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@Mixin(value = FactoryPanelConfigurationPacket.class, remap = false)
public abstract class FactoryPanelConfigurationPacketMixin {
    @Shadow
    @Final
    private Map<FactoryPanelPosition, Integer> inputAmounts;
    @Shadow
    @Final
    private FactoryPanelPosition position;

    @Inject(method = "applySettings", at = @At("HEAD"))
    private void ccb$applySettingsHead(ServerPlayer player, FactoryPanelBlockEntity blockEntity, CallbackInfo callback) {
        GasFactoryGaugeConfiguration.sanitizeInputs(blockEntity, inputAmounts);
    }

    @ModifyExpressionValue(method = "applySettings", at = @At(value = "FIELD", target = "Lcom/simibubi/create/content/logistics/factoryBoard/FactoryPanelConfigurationPacket;outputAmount:I"))
    private int ccb$applySettings(int outputAmount, ServerPlayer player, FactoryPanelBlockEntity blockEntity) {
        return GasFactoryGaugeConfiguration.getOutputAmount(blockEntity, position, outputAmount);
    }

    @Inject(method = "applySettings", at = @At("TAIL"))
    private void ccb$applySettingsTail(ServerPlayer player, FactoryPanelBlockEntity blockEntity, CallbackInfo callback) {
        GasFactoryGaugeConfiguration.finish(blockEntity, position);
    }
}
