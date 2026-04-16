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

public enum JumpStrengthUpgrade implements AirtightUpgrade {
    INSTANCE;

    public static void refreshModifiers(@NotNull Player player) {
        AttributeInstance jumpInstance = player.getAttributes().getInstance(Attributes.JUMP_STRENGTH);
        AttributeInstance safeInstance = player.getAttributes().getInstance(Attributes.SAFE_FALL_DISTANCE);
        if (jumpInstance == null || safeInstance == null) {
            return;
        }

        boolean canApply = INSTANCE.canApply(player);
        boolean hasJumpModifier = jumpInstance.hasModifier(AirtightBootsItem.JUMP_STRENGTH_ID);
        if (canApply && !hasJumpModifier) {
            jumpInstance.addTransientModifier(AirtightBootsItem.JUMP_STRENGTH_MODIFIER);
        }
        else if (!canApply && hasJumpModifier) {
            jumpInstance.removeModifier(AirtightBootsItem.JUMP_STRENGTH_MODIFIER);
        }

        boolean hasSafeModifier = safeInstance.hasModifier(AirtightBootsItem.SAFE_FALL_DISTANCE_ID);
        if (canApply && !hasSafeModifier) {
            safeInstance.addTransientModifier(AirtightBootsItem.SAFE_FALL_DISTANCE_MODIFIER);
        }
        else if (!canApply && hasSafeModifier) {
            safeInstance.removeModifier(AirtightBootsItem.SAFE_FALL_DISTANCE_MODIFIER);
        }
    }

    @Override
    public int getIndex() {
        return 1;
    }

    @Override
    public @NotNull ResourceLocation getID() {
        return CreateCraftedBeginning.asResource("jump_strength");
    }

    @Override
    public Item getUpgradeItem() {
        return Items.RABBIT_FOOT;
    }

    @Override
    public @NotNull Couple<Integer> getOffset() {
        return Couple.create(36, 55);
    }

    @Override
    public CCBIcons getIcon() {
        return CCBIcons.I_JUMP_STRENGTH;
    }

    @Override
    public boolean isRightIndicator() {
        return false;
    }

    @Override
    public @NotNull Component getTitle() {
        return CCBLang.translateDirect("gui.airtight_boots.jump_strength_upgrade");
    }

    @Override
    public @NotNull Component getDescription() {
        return CCBLang.translateDirect("gui.airtight_boots.jump_strength_upgrade.description");
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
