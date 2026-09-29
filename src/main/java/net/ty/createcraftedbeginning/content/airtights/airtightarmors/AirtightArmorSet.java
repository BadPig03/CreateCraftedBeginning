package net.ty.createcraftedbeginning.content.airtights.airtightarmors;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.ty.createcraftedbeginning.registry.CCBItems;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class AirtightArmorSet {
    private AirtightArmorSet() {
    }

    public static boolean isEntireArmoredUp(Player player) {
        ItemStack helmet = player.getItemBySlot(EquipmentSlot.HEAD);
        if (!helmet.is(CCBItems.AIRTIGHT_HELMET)) {
            return false;
        }

        ItemStack chestplate = player.getItemBySlot(EquipmentSlot.CHEST);
        if (!chestplate.is(CCBItems.AIRTIGHT_CHESTPLATE)) {
            return false;
        }

        ItemStack leggings = player.getItemBySlot(EquipmentSlot.LEGS);
        if (!leggings.is(CCBItems.AIRTIGHT_LEGGINGS)) {
            return false;
        }

        ItemStack boots = player.getItemBySlot(EquipmentSlot.FEET);
        return boots.is(CCBItems.AIRTIGHT_BOOTS);
    }
}
