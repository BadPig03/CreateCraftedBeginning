package net.ty.createcraftedbeginning.content.airtights.airtighthandhelddrill.upgrades;

import net.createmod.catnip.data.Couple;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.ty.createcraftedbeginning.api.CCBAPI;
import net.ty.createcraftedbeginning.content.airtights.airtightupgrades.AirtightUpgrade;
import net.ty.createcraftedbeginning.content.airtights.airtightupgrades.AirtightUpgradeIcon;
import net.ty.createcraftedbeginning.foundation.lang.CCBLang;
import net.ty.createcraftedbeginning.registry.CCBItems;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public enum HandheldDrillAerogelProtectionButton implements AirtightUpgrade {
    INSTANCE;

    private static final ResourceLocation ID = CCBAPI.asResource("handheld_drill_aerogel_protection");
    private static final Couple<Integer> OFFSET = Couple.create(94, 114);

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
        return LiquidReplacementUpgrade.INSTANCE.canApply(item);
    }

    @Override
    public boolean isRightIndicator() {
        return true;
    }

    @Override
    public AirtightUpgradeIcon getIcon() {
        return AirtightUpgradeIcon.AEROGEL_PROTECTION;
    }

    @Override
    public Component getDescription() {
        return CCBLang.translateDirect("gui.airtight_handheld_drill.aerogel_protection.description");
    }

    @Override
    public Component getTitle() {
        return CCBLang.translateDirect("gui.airtight_handheld_drill.aerogel_protection");
    }

    @Override
    public Couple<Integer> getOffset() {
        return OFFSET;
    }

    @Override
    public Item getDefaultUpgradeItem() {
        return Items.BARRIER;
    }

    @Override
    public ResourceLocation getID() {
        return ID;
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
        return item.is(CCBItems.AIRTIGHT_HANDHELD_DRILL) && isEnabled(item) && LiquidReplacementUpgrade.INSTANCE.canApply(item);
    }
}
