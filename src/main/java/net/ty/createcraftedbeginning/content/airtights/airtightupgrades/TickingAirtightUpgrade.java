package net.ty.createcraftedbeginning.content.airtights.airtightupgrades;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public interface TickingAirtightUpgrade extends AirtightUpgrade {
    boolean shouldApplyEffect(Player player, ItemStack item);

    default void tick(Player player, ItemStack item) {
        if (!isActive(player, item) || !shouldApplyEffect(player, item)) {
            return;
        }

        applyEffect(player);
    }
}
