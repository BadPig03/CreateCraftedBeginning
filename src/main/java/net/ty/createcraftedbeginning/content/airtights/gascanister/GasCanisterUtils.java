package net.ty.createcraftedbeginning.content.airtights.gascanister;

import net.minecraft.ChatFormatting;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.ty.createcraftedbeginning.api.gas.gases.GasCapabilities.GasHandler;
import net.ty.createcraftedbeginning.api.gas.gases.GasStack;
import net.ty.createcraftedbeginning.data.CCBLang;
import net.ty.createcraftedbeginning.registry.CCBDataComponents;
import net.ty.createcraftedbeginning.registry.CCBSoundEvents;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class GasCanisterUtils {
    public static final int COLOR_RED = 0xFFFF5D6C;
    public static final int COLOR_CYAN = 0xFF71C7D5;
    public static final int COLOR_WHITE = 0xFFEFEFEF;

    private GasCanisterUtils() {
    }

    public static boolean shouldCauseBlockBreakReset(ItemStack oldStack, ItemStack newStack) {
        if (!newStack.is(oldStack.getItem())) {
            return true;
        }

        if (!newStack.isDamageableItem() || !oldStack.isDamageableItem()) {
            return !ItemStack.isSameItemSameComponents(newStack, oldStack);
        }

        DataComponentMap newComponents = newStack.getComponents();
        DataComponentMap oldComponents = oldStack.getComponents();
        if (newComponents.isEmpty() || oldComponents.isEmpty()) {
            return !(newComponents.isEmpty() && oldComponents.isEmpty());
        }

        Set<DataComponentType<?>> newKeys = new HashSet<>(newComponents.keySet());
        Set<DataComponentType<?>> oldKeys = new HashSet<>(oldComponents.keySet());
        newKeys.remove(CCBDataComponents.CANISTER_CONTAINER_CONTENTS);
        newKeys.remove(CCBDataComponents.CANISTER_CONTAINER_CAPACITIES);
        oldKeys.remove(CCBDataComponents.CANISTER_CONTAINER_CONTENTS);
        oldKeys.remove(CCBDataComponents.CANISTER_CONTAINER_CAPACITIES);
        return !newKeys.equals(oldKeys) || !newKeys.stream().allMatch(key -> Objects.equals(newComponents.get(key), oldComponents.get(key)));
    }

    public static boolean isCanisterInjectable(ItemStack itemStack, GasStack resource) {
        if (!(itemStack.getCapability(GasHandler.ITEM) instanceof GasCanisterContainerContents canisterContents)) {
            return false;
        }

        GasStack gasContent = canisterContents.getGasInTank(0);
        return gasContent.isEmpty() || GasStack.isSameGasSameComponents(gasContent, resource) && !canisterContents.isFull();
    }

    /**
     * Displays a custom warning hint to the player with visual and audio feedback (server-side only).
     * <p>
     * This method shows a client-side warning message to the player in red text format
     * and plays a denial sound effect, but only executes on the server side. The message
     * is displayed as an action bar message (above the hotbar) for prominent visibility.
     * </p>
     * <p>
     * The method includes a client-side check to ensure it only runs on the server,
     * preventing duplicate execution when called from client-side code.
     * </p>
     *
     * @param player the player to display the warning to (must not be null)
     * @param key    the translation key for the warning message (must not be null)
     * @param args   optional arguments for the translation key
     * @see CCBLang#translateDirect(String, Object...)
     * @see CCBSoundEvents#DENY
     */
    public static void displayCustomWarningHint(Player player, String key, Object... args) {
        Level level = player.level();
        if (level.isClientSide) {
            return;
        }

        player.displayClientMessage(CCBLang.translateDirect(key, args).withStyle(ChatFormatting.RED), true);
        CCBSoundEvents.DENY.playOnServer(level, player.blockPosition(), 1, 1);
    }
}
