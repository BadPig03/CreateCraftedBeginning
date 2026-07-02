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
import net.ty.createcraftedbeginning.config.CCBConfig;
import net.ty.createcraftedbeginning.content.airtights.airtightupgrades.AirtightUpgrade;
import net.ty.createcraftedbeginning.data.CCBIcons;
import net.ty.createcraftedbeginning.data.CCBLang;
import net.ty.createcraftedbeginning.registry.CCBItems;
import org.jetbrains.annotations.Unmodifiable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public enum MagnetUpgrade implements AirtightUpgrade {
    INSTANCE;

    @Override
    public @Unmodifiable List<Component> getComponents(Player player, ItemStack item) {
        float multiplier = CCBConfig.server().equipments.magnetMultiplier.getF();
        return List.of(CCBLang.translateDirect("gui.gas_consumption.multiplier", multiplier));
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
        return false;
    }

    @Override
    public CCBIcons getIcon() {
        return CCBIcons.I_MAGNET;
    }

    @Override
    public Component getDescription() {
        return CCBLang.translateDirect("gui.airtight_handheld_drill.magnet_upgrade.description");
    }

    @Override
    public Component getTitle() {
        return CCBLang.translateDirect("gui.airtight_handheld_drill.magnet_upgrade");
    }

    @Override
    public Couple<Integer> getOffset() {
        return Couple.create(141, 78);
    }

    @Override
    public int getGasConsumptionPerSecond(Player player, ItemStack item) {
        return -1;
    }

    @Override
    public int getIndex() {
        return 1;
    }

    @Override
    public Item getUpgradeItem() {
        return Items.HOPPER;
    }

    @Override
    public ResourceLocation getID() {
        return CreateCraftedBeginning.asResource("magnet");
    }

    @Override
    public void applyEffect(Player player) {
    }

    @Override
    public boolean isActive(Player player, ItemStack item) {
        return item.is(CCBItems.AIRTIGHT_HANDHELD_DRILL) && AirtightUpgrade.super.isActive(player, item);
    }

    public boolean canApply(ItemStack item) {
        return item.is(CCBItems.AIRTIGHT_HANDHELD_DRILL) && isEnabled(item);
    }
}
