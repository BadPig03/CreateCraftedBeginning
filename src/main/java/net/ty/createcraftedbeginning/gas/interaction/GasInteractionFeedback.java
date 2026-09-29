package net.ty.createcraftedbeginning.gas.interaction;

import net.minecraft.ChatFormatting;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.ty.createcraftedbeginning.foundation.lang.CCBLang;
import net.ty.createcraftedbeginning.registry.CCBSoundEvents;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class GasInteractionFeedback {
    private GasInteractionFeedback() {
    }

    public static void sendWarningFeedback(Player player, String key, Object... args) {
        Level level = player.level();
        if (level.isClientSide) {
            return;
        }

        player.displayClientMessage(CCBLang.translateDirect(key, args).withStyle(ChatFormatting.RED), true);
        CCBSoundEvents.DENY.playOnServer(level, player.blockPosition(), 1, 1);
    }
}
