package net.ty.createcraftedbeginning.content.airtights.airtightarmors.airtightleggings.upgrades;

import net.createmod.catnip.data.Couple;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.ty.createcraftedbeginning.CreateCraftedBeginning;
import net.ty.createcraftedbeginning.content.airtights.airtightupgrades.AirtightUpgrade;
import net.ty.createcraftedbeginning.data.CCBIcons;
import net.ty.createcraftedbeginning.data.CCBLang;
import net.ty.createcraftedbeginning.registry.CCBItems;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public enum SwiftSneakUpgrade implements AirtightUpgrade {
    INSTANCE;

    @Override
    public List<Component> getComponents(Player player, ItemStack item) {
        return List.of();
    }

    @Override
    public boolean canApply(Player player) {
        return isActive(player, player.getItemBySlot(EquipmentSlot.LEGS));
    }

    @Override
    public boolean meetsConditions(Player player, ItemStack item) {
        return true;
    }

    @Override
    public boolean isRightIndicator() {
        return false;
    }

    @Override
    public CCBIcons getIcon() {
        return CCBIcons.I_SWIFT_SNEAK;
    }

    @Override
    public Component getDescription() {
        return CCBLang.translateDirect("gui.airtight_leggings.swift_sneak_upgrade.description");
    }

    @Override
    public Component getTitle() {
        return CCBLang.translateDirect("gui.airtight_leggings.swift_sneak_upgrade");
    }

    @Override
    public Couple<Integer> getOffset() {
        return Couple.create(36, 79);
    }

    @Override
    public int getGasConsumptionPerSecond(Player player, ItemStack item) {
        return -1;
    }

    @Override
    public int getIndex() {
        return 2;
    }

    @Override
    public Item getUpgradeItem() {
        return Items.SOUL_SAND;
    }

    @Override
    public ResourceLocation getID() {
        return CreateCraftedBeginning.asResource("swift_sneak");
    }

    @Override
    public void applyEffect(Player player) {
    }

    @Override
    public boolean isActive(Player player, ItemStack item) {
        return item.is(CCBItems.AIRTIGHT_LEGGINGS) && AirtightUpgrade.super.isActive(player, item);
    }

    public boolean canApply(ItemStack item)
    {
        return item.is(CCBItems.AIRTIGHT_LEGGINGS) && isEnabled(item);
    }
}
