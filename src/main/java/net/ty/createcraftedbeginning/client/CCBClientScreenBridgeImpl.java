package net.ty.createcraftedbeginning.client;

import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelBehaviour;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelPosition;
import net.createmod.catnip.gui.ScreenOpener;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.ty.createcraftedbeginning.content.airtights.gasfactorygauge.GasFactoryGaugeBehaviour;
import net.ty.createcraftedbeginning.content.airtights.gasfactorygauge.GasFactoryGaugeScreen;
import net.ty.createcraftedbeginning.platform.client.ClientScreenBridge.Service;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@OnlyIn(Dist.CLIENT)
public final class CCBClientScreenBridgeImpl implements Service {
    private static @Nullable FactoryPanelBehaviour resolveFactoryPanelBehaviour(FactoryPanelPosition panelPosition) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            return null;
        }
        return FactoryPanelBehaviour.at(level, panelPosition);
    }

    @Override
    public void openGasFactoryGaugeScreen(FactoryPanelPosition panelPosition, Player player) {
        if (!(player instanceof LocalPlayer) || !(resolveFactoryPanelBehaviour(panelPosition) instanceof GasFactoryGaugeBehaviour behaviour)) {
            return;
        }

        ScreenOpener.open(new GasFactoryGaugeScreen(behaviour));
    }

    @Override
    public @Nullable FactoryPanelBehaviour resolveFactoryPanelBehaviour(RegistryFriendlyByteBuf extraData) {
        return resolveFactoryPanelBehaviour(FactoryPanelPosition.STREAM_CODEC.decode(extraData));
    }
}
