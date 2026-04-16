package net.ty.createcraftedbeginning.content.airtights.airtightarmors.airtightleggings.upgrades;

import net.createmod.catnip.data.Couple;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.ty.createcraftedbeginning.CreateCraftedBeginning;
import net.ty.createcraftedbeginning.api.gas.cansiters.CanisterContainerSuppliers;
import net.ty.createcraftedbeginning.api.gas.gases.GasStack;
import net.ty.createcraftedbeginning.content.airtights.airtightupgrades.AirtightUpgrade;
import net.ty.createcraftedbeginning.data.CCBIcons;
import net.ty.createcraftedbeginning.data.CCBLang;
import net.ty.createcraftedbeginning.registry.CCBItems;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public enum BlastResistanceUpgrade implements AirtightUpgrade {
    INSTANCE;

    @Override
    public int getIndex() {
        return 4;
    }

    @Override
    public @NotNull ResourceLocation getID() {
        return CreateCraftedBeginning.asResource("blast_resistance");
    }

    @Override
    public Item getUpgradeItem() {
        return Items.OBSIDIAN;
    }

    @Override
    public @NotNull Couple<Integer> getOffset() {
        return Couple.create(132, 55);
    }

    @Override
    public CCBIcons getIcon() {
        return CCBIcons.I_BLAST_RESISTANCE;
    }

    @Override
    public boolean isRightIndicator() {
        return true;
    }

    @Override
    public @NotNull Component getTitle() {
        return CCBLang.translateDirect("gui.airtight_leggings.blast_resistance_upgrade");
    }

    @Override
    public @NotNull Component getDescription() {
        return CCBLang.translateDirect("gui.airtight_leggings.blast_resistance_upgrade.description");
    }

    @Override
    public @Nullable Component getGasCostComponent(Player player) {
        int gasCost = getGasCost(player);
        if (gasCost < 0) {
            return null;
        }

        return CCBLang.translateDirect("gui.gas_cost_per_second", gasCost);
    }

    @Override
    public int getGasCost(Player player) {
        GasStack gasContent = CanisterContainerSuppliers.getFirstAvailableGasContent(player);
        if (gasContent.isEmpty()) {
            return -1;
        }

        return 0;
    }

    @Override
    public boolean canApply(@NotNull Player player) {
        ItemStack leggings = player.getItemBySlot(EquipmentSlot.LEGS);
        if (!leggings.is(CCBItems.AIRTIGHT_LEGGINGS) || !isEnabled(leggings) || !CanisterContainerSuppliers.isAnyContainerAvailable(player)) {
            return false;
        }

        GasStack gasContent = CanisterContainerSuppliers.getFirstAvailableGasContent(player);
        if (gasContent.isEmpty()) {
            return false;
        }

        int gasCost = getGasCost(player);
        return gasCost >= 0;
    }

    @Override
    public void applyEffect(Player player) {
    }
}
