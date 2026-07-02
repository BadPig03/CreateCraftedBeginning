package net.ty.createcraftedbeginning.content.airtights.airtighthanddrill.upgrades;

import net.createmod.catnip.data.Couple;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
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
public enum HandheldDrillOutlineDisplayButton implements AirtightUpgrade {
    INSTANCE;

    @Override
    public List<Component> getComponents(Player player, ItemStack item) {
        return List.of();
    }

    @Override
    public boolean canApply(Player player) {
        return false;
    }

    @Override
    public boolean meetsConditions(Player player, ItemStack item) {
        return true;
    }

    @Override
    public boolean isRightIndicator() {
        return true;
    }

    @Override
    public CCBIcons getIcon() {
        return CCBIcons.I_OUTLINE_DISPLAY;
    }

    @Override
    public Component getDescription() {
        return CCBLang.translateDirect("gui.airtight_handheld_drill.outline_display.description");
    }

    @Override
    public Component getTitle() {
        return CCBLang.translateDirect("gui.airtight_handheld_drill.outline_display");
    }

    @Override
    public Couple<Integer> getOffset() {
        return Couple.create(76, 114);
    }

    @Override
    public int getGasConsumptionPerSecond(Player player, ItemStack item) {
        return -1;
    }

    @Override
    public int getIndex() {
        return 6;
    }

    @Override
    public Item getUpgradeItem() {
        return Items.BARRIER;
    }

    @Override
    public ResourceLocation getID() {
        return CreateCraftedBeginning.asResource("handheld_drill_outline_display");
    }

    @Override
    public void applyEffect(Player player) {
    }

    @Override
    public boolean startsEnabled() {
        return true;
    }

    @Override
    public boolean startsInstalled() {
        return true;
    }

    @Override
    public boolean isActive(Player player, ItemStack item) {
        return item.is(CCBItems.AIRTIGHT_HANDHELD_DRILL) && AirtightUpgrade.super.isActive(player, item);
    }

    public boolean canApply(ItemStack item) {
        return item.is(CCBItems.AIRTIGHT_HANDHELD_DRILL) && isEnabled(item);
    }
}
