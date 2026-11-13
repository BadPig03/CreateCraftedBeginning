package net.ty.createcraftedbeginning.mixin.client.create;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.api.equipment.goggles.IHaveHoveringInformation;
import com.simibubi.create.content.equipment.goggles.GoggleOverlayRenderer;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.HitResult;
import net.ty.createcraftedbeginning.content.opticalpower.amethystcollectorpanel.AmethystCollectorPanelBlockEntity;
import net.ty.createcraftedbeginning.content.opticalpower.amethystcollectorpanel.AmethystCollectorPanelOutput;
import net.ty.createcraftedbeginning.content.opticalpower.amethystcollectorpanel.AmethystCollectorPanelOutput.Limitation;
import net.ty.createcraftedbeginning.content.opticalpower.amethystmirror.AmethystMirrorControls;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@Mixin(value = GoggleOverlayRenderer.class, remap = false)
public abstract class GoggleOverlayRendererMixin {
    @ModifyExpressionValue(method = "renderOverlay", at = @At(value = "FIELD", target = "Lnet/minecraft/client/Minecraft;hitResult:Lnet/minecraft/world/phys/HitResult;"))
    private static @Nullable HitResult ccb$renderOverlay$hitResult(@Nullable HitResult original) {
        return AmethystMirrorControls.resolveGoggleTarget(original);
    }

    @WrapOperation(method = "renderOverlay", at = @At(value = "INVOKE", target = "Lcom/simibubi/create/api/equipment/goggles/IHaveGoggleInformation;addToGoggleTooltip(Ljava/util/List;Z)Z"))
    private static boolean ccb$renderOverlay$addToGoggleTooltip(IHaveGoggleInformation information, List<Component> tooltip, boolean sneaking, Operation<Boolean> original, @Share("ccb$collectorPanel") LocalRef<AmethystCollectorPanelBlockEntity> collectorPanel, @Share("ccb$collectorOutput") LocalRef<AmethystCollectorPanelOutput> collectorOutput) {
        if (!(information instanceof AmethystCollectorPanelBlockEntity panel)) {
            return original.call(information, tooltip, sneaking);
        }

        AmethystCollectorPanelOutput output = panel.getTooltipOutput();
        collectorPanel.set(panel);
        collectorOutput.set(output);
        return panel.addToGoggleTooltip(tooltip, output);
    }

    @WrapOperation(method = "renderOverlay", at = @At(value = "INVOKE", target = "Lcom/simibubi/create/api/equipment/goggles/IHaveHoveringInformation;addToTooltip(Ljava/util/List;Z)Z"))
    private static boolean ccb$renderOverlay$addToTooltip(IHaveHoveringInformation information, List<Component> tooltip, boolean sneaking, Operation<Boolean> original, @Share("ccb$collectorPanel") LocalRef<AmethystCollectorPanelBlockEntity> collectorPanel, @Share("ccb$collectorOutput") LocalRef<AmethystCollectorPanelOutput> collectorOutput) {
        if (!(information instanceof AmethystCollectorPanelBlockEntity panel) || collectorPanel.get() != panel) {
            return original.call(information, tooltip, sneaking);
        }

        AmethystCollectorPanelOutput output = collectorOutput.get();
        return (output == null || output.limitation() != Limitation.UNAVAILABLE) && panel.addToTooltip(tooltip, output);
    }
}
