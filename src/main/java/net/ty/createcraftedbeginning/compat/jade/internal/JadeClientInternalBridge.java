package net.ty.createcraftedbeginning.compat.jade.internal;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import snownee.jade.api.config.IWailaConfig.IConfigOverlay;
import snownee.jade.overlay.OverlayRenderer;

import javax.annotation.ParametersAreNonnullByDefault;

@OnlyIn(Dist.CLIENT)
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class JadeClientInternalBridge {
    private JadeClientInternalBridge() {
    }

    public static int applyOverlayAlpha(int color) {
        return IConfigOverlay.applyAlpha(color, OverlayRenderer.alpha);
    }
}
