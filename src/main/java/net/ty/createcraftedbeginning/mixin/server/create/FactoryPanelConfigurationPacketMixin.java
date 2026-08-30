package net.ty.createcraftedbeginning.mixin.server.create;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelBehaviour;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelBlockEntity;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelConfigurationPacket;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelPosition;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.ty.createcraftedbeginning.content.airtights.gasfactorygauge.GasFactoryGaugeBehaviour;
import net.ty.createcraftedbeginning.content.airtights.gasfilter.GasVirtualUtils;
import net.ty.createcraftedbeginning.foundation.CCBMathUtils;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

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
    private void ccb$applySettingsHead(ServerPlayer player, FactoryPanelBlockEntity blockEntity, CallbackInfo ci) {
        if (blockEntity.getLevel() == null || inputAmounts.isEmpty()) {
            return;
        }

        for (Entry<FactoryPanelPosition, Integer> entry : inputAmounts.entrySet()) {
            FactoryPanelBehaviour source = FactoryPanelBehaviour.at(blockEntity.getLevel(), entry.getKey());
            if (source == null || !GasVirtualUtils.isVirtualItem(source.getFilter())) {
                continue;
            }

            entry.setValue(CCBMathUtils.clampNonNegative(entry.getValue(), GasFactoryGaugeBehaviour.MAX_TARGET_AMOUNT));
        }
    }

    @ModifyExpressionValue(method = "applySettings", at = @At(value = "FIELD", target = "Lcom/simibubi/create/content/logistics/factoryBoard/FactoryPanelConfigurationPacket;outputAmount:I"))
    private int ccb$applySettings(int outputAmount, ServerPlayer player, FactoryPanelBlockEntity blockEntity) {
        FactoryPanelBehaviour behaviour = blockEntity.panels.get(position.slot());
        if (!(behaviour instanceof GasFactoryGaugeBehaviour)) {
            return outputAmount;
        }
        return Mth.clamp(outputAmount, 1, GasFactoryGaugeBehaviour.MAX_TARGET_AMOUNT);
    }

    @Inject(method = "applySettings", at = @At("TAIL"))
    private void ccb$applySettingsTail(ServerPlayer player, FactoryPanelBlockEntity blockEntity, CallbackInfo ci) {
        FactoryPanelBehaviour behaviour = blockEntity.panels.get(position.slot());
        if (!(behaviour instanceof GasFactoryGaugeBehaviour gasGauge)) {
            return;
        }

        gasGauge.activeCraftingArrangement = List.of();
    }
}
