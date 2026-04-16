package net.ty.createcraftedbeginning.content.airtights.airtightarmors.airtightchestplate.upgrades;

import net.createmod.catnip.data.Couple;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.ty.createcraftedbeginning.CreateCraftedBeginning;
import net.ty.createcraftedbeginning.api.gas.cansiters.CanisterContainerSuppliers;
import net.ty.createcraftedbeginning.api.gas.gases.GasStack;
import net.ty.createcraftedbeginning.content.airtights.airtightarmors.airtightchestplate.AirtightChestplateItem;
import net.ty.createcraftedbeginning.content.airtights.airtightupgrades.AirtightUpgrade;
import net.ty.createcraftedbeginning.data.CCBIcons;
import net.ty.createcraftedbeginning.data.CCBLang;
import net.ty.createcraftedbeginning.registry.CCBItems;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public enum HasteUpgrade implements AirtightUpgrade {
    INSTANCE;

    public static void refreshModifiers(@NotNull Player player) {
        AttributeInstance attackSpeedInstance = player.getAttributes().getInstance(Attributes.ATTACK_SPEED);
        AttributeInstance blockBreakSpeedInstance = player.getAttributes().getInstance(Attributes.BLOCK_BREAK_SPEED);
        if (attackSpeedInstance == null || blockBreakSpeedInstance == null) {
            return;
        }

        boolean canApply = INSTANCE.canApply(player);
        boolean hasAttackModifier = attackSpeedInstance.hasModifier(AirtightChestplateItem.ATTACK_SPEED_ID);
        if (canApply && !hasAttackModifier) {
            attackSpeedInstance.addTransientModifier(AirtightChestplateItem.ATTACK_SPEED_MODIFIER);
        }
        else if (!canApply && hasAttackModifier) {
            attackSpeedInstance.removeModifier(AirtightChestplateItem.ATTACK_SPEED_MODIFIER);
        }

        boolean hasBlockModifier = blockBreakSpeedInstance.hasModifier(AirtightChestplateItem.BLOCK_BREAK_SPEED_ID);
        if (canApply && !hasBlockModifier) {
            blockBreakSpeedInstance.addTransientModifier(AirtightChestplateItem.BLOCK_BREAK_SPEED_MODIFIER);
        }
        else if (!canApply && hasBlockModifier) {
            blockBreakSpeedInstance.removeModifier(AirtightChestplateItem.BLOCK_BREAK_SPEED_MODIFIER);
        }
    }

    @Override
    public int getIndex() {
        return 4;
    }

    @Override
    public @NotNull ResourceLocation getID() {
        return CreateCraftedBeginning.asResource("haste");
    }

    @Override
    public Item getUpgradeItem() {
        return Items.GOLDEN_PICKAXE;
    }

    @Override
    public @NotNull Couple<Integer> getOffset() {
        return Couple.create(132, 55);
    }

    @Override
    public CCBIcons getIcon() {
        return CCBIcons.I_HASTE;
    }

    @Override
    public boolean isRightIndicator() {
        return true;
    }

    @Override
    public @NotNull Component getTitle() {
        return CCBLang.translateDirect("gui.airtight_chestplate.haste_upgrade");
    }

    @Override
    public @NotNull Component getDescription() {
        return CCBLang.translateDirect("gui.airtight_chestplate.haste_upgrade.description");
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
        ItemStack chestplate = player.getItemBySlot(EquipmentSlot.CHEST);
        return chestplate.is(CCBItems.AIRTIGHT_CHESTPLATE) && isEnabled(chestplate) && CanisterContainerSuppliers.isAnyContainerAvailable(player) && !CanisterContainerSuppliers.getFirstAvailableGasContent(player).isEmpty();
    }

    @Override
    public void applyEffect(Player player) {
    }
}
