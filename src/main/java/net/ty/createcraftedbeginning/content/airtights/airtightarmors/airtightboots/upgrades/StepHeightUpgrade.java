package net.ty.createcraftedbeginning.content.airtights.airtightarmors.airtightboots.upgrades;

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
import net.ty.createcraftedbeginning.content.airtights.airtightarmors.airtightboots.AirtightBootsItem;
import net.ty.createcraftedbeginning.content.airtights.airtightupgrades.AirtightUpgrade;
import net.ty.createcraftedbeginning.data.CCBIcons;
import net.ty.createcraftedbeginning.data.CCBLang;
import net.ty.createcraftedbeginning.registry.CCBItems;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public enum StepHeightUpgrade implements AirtightUpgrade {
    INSTANCE;

    public static void refreshModifiers(@NotNull Player player) {
        AttributeInstance instance = player.getAttributes().getInstance(Attributes.STEP_HEIGHT);
        if (instance == null) {
            return;
        }

        boolean canApply = INSTANCE.canApply(player);
        boolean hasModifier = instance.hasModifier(AirtightBootsItem.STEP_HEIGHT_ID);
        if (canApply && !hasModifier) {
            instance.addTransientModifier(AirtightBootsItem.STEP_HEIGHT_MODIFIER);
        }
        else if (!canApply && hasModifier) {
            instance.removeModifier(AirtightBootsItem.STEP_HEIGHT_MODIFIER);
        }
    }

    @Override
    public int getIndex() {
        return 2;
    }

    @Override
    public @NotNull ResourceLocation getID() {
        return CreateCraftedBeginning.asResource("step_height");
    }

    @Override
    public Item getUpgradeItem() {
        return Items.LADDER;
    }

    @Override
    public @NotNull Couple<Integer> getOffset() {
        return Couple.create(36, 79);
    }

    @Override
    public CCBIcons getIcon() {
        return CCBIcons.I_STEP_HEIGHT;
    }

    @Override
    public boolean isRightIndicator() {
        return false;
    }

    @Override
    public @NotNull Component getTitle() {
        return CCBLang.translateDirect("gui.airtight_boots.step_height_upgrade");
    }

    @Override
    public @NotNull Component getDescription() {
        return CCBLang.translateDirect("gui.airtight_boots.step_height_upgrade.description");
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
        ItemStack boots = player.getItemBySlot(EquipmentSlot.FEET);
        return boots.is(CCBItems.AIRTIGHT_BOOTS) && isEnabled(boots) && CanisterContainerSuppliers.isAnyContainerAvailable(player) && !CanisterContainerSuppliers.getFirstAvailableGasContent(player).isEmpty();
    }

    @Override
    public void applyEffect(Player player) {
    }
}
