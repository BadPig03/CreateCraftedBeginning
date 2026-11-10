package net.ty.createcraftedbeginning.content.airtights.airtighthandhelddrill;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.InputConstants.Type;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import net.ty.createcraftedbeginning.api.CCBAPI;
import net.ty.createcraftedbeginning.registry.CCBItems;
import org.jetbrains.annotations.ApiStatus.Internal;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@Internal
@EventBusSubscriber(modid = CCBAPI.MOD_ID, value = Dist.CLIENT)
public final class AirtightHandheldDrillKeys {
    private static final KeyMapping SINGLE_BLOCK = new KeyMapping("key.createcraftedbeginning.drill_single_block", KeyConflictContext.IN_GAME, Type.KEYSYM, InputConstants.KEY_TAB, "key.categories.createcraftedbeginning");

    private AirtightHandheldDrillKeys() {
    }

    public static KeyMapping getSingleBlockKey() {
        return SINGLE_BLOCK;
    }

    static boolean canUseShortcuts(Minecraft minecraft) {
        LocalPlayer player = minecraft.player;
        return player != null && minecraft.gameMode != null && minecraft.screen == null && minecraft.getOverlay() == null && minecraft.isWindowActive() && player.isAlive() && !player.isSpectator() && player.getMainHandItem().is(CCBItems.AIRTIGHT_HANDHELD_DRILL);
    }

    @SubscribeEvent
    private static void registerKeys(RegisterKeyMappingsEvent event) {
        event.register(SINGLE_BLOCK);
    }
}
