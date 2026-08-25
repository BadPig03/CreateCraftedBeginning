package net.ty.createcraftedbeginning.client;

import com.simibubi.create.infrastructure.config.AllConfigs;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.ty.createcraftedbeginning.config.CCBConfig;
import net.ty.createcraftedbeginning.platform.client.ClientContextBridge.Service;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@OnlyIn(Dist.CLIENT)
public final class CCBClientContextBridgeImpl implements Service {
    @Override
    public @Nullable Player getClientPlayer() {
        return Minecraft.getInstance().player;
    }

    @Override
    public boolean isOverstressedTooltipEnabled() {
        return AllConfigs.client().enableOverstressedTooltip.get();
    }

    @Override
    public int getMaxItemStackDisplay() {
        return CCBConfig.client().maxItemStackDisplay.get();
    }

    @Override
    public float getFilterItemRenderDistance() {
        return AllConfigs.client().filterItemRenderDistance.getF();
    }
}
