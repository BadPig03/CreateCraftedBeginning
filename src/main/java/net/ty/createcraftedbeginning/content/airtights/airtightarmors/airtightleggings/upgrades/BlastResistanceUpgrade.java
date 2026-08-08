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
import net.ty.createcraftedbeginning.content.airtights.airtightupgrades.AirtightUpgradeIcon;
import net.ty.createcraftedbeginning.data.CCBLang;
import net.ty.createcraftedbeginning.registry.CCBItems;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public enum BlastResistanceUpgrade implements AirtightUpgrade {
    INSTANCE;

    private static final ResourceLocation ID = CreateCraftedBeginning.asResource("blast_resistance");
    private static final Couple<Integer> OFFSET = Couple.create(132, 55);

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
        return true;
    }

    @Override
    public AirtightUpgradeIcon getIcon() {
        return AirtightUpgradeIcon.BLAST_RESISTANCE;
    }

    @Override
    public Component getDescription() {
        return CCBLang.translateDirect("gui.airtight_leggings.blast_resistance_upgrade.description");
    }

    @Override
    public Component getTitle() {
        return CCBLang.translateDirect("gui.airtight_leggings.blast_resistance_upgrade");
    }

    @Override
    public Couple<Integer> getOffset() {
        return OFFSET;
    }

    @Override
    public Item getUpgradeItem() {
        return Items.RESPAWN_ANCHOR;
    }

    @Override
    public ResourceLocation getID() {
        return ID;
    }

    @Override
    public void applyEffect(Player player) {
    }

    @Override
    public boolean isActive(Player player, ItemStack item) {
        return item.is(CCBItems.AIRTIGHT_LEGGINGS) && AirtightUpgrade.super.isActive(player, item);
    }

    public boolean canApply(ItemStack item) {
        return item.is(CCBItems.AIRTIGHT_LEGGINGS) && isEnabled(item);
    }
}
