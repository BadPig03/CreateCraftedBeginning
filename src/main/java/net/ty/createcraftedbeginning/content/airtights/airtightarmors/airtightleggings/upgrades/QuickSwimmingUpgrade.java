package net.ty.createcraftedbeginning.content.airtights.airtightarmors.airtightleggings.upgrades;

import net.createmod.catnip.data.Couple;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
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

public enum QuickSwimmingUpgrade implements AirtightUpgrade {
    INSTANCE;

    private static final int DURATION_THRESHOLD = 30;

    @Override
    public int getIndex() {
        return 1;
    }

    @Override
    public @NotNull ResourceLocation getID() {
        return CreateCraftedBeginning.asResource("quick_swimming");
    }

    @Override
    public Item getUpgradeItem() {
        return Items.AXOLOTL_BUCKET;
    }

    @Override
    public @NotNull Couple<Integer> getOffset() {
        return Couple.create(36, 55);
    }

    @Override
    public CCBIcons getIcon() {
        return CCBIcons.I_QUICK_SWIMMING;
    }

    @Override
    public boolean isRightIndicator() {
        return false;
    }

    @Override
    public @NotNull Component getTitle() {
        return CCBLang.translateDirect("gui.airtight_leggings.quick_swimming_upgrade");
    }

    @Override
    public @NotNull Component getDescription() {
        return CCBLang.translateDirect("gui.airtight_leggings.quick_swimming_upgrade.description");
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
        if (!leggings.is(CCBItems.AIRTIGHT_LEGGINGS) || !isEnabled(leggings) || !CanisterContainerSuppliers.isAnyContainerAvailable(player) || !player.isInWaterOrBubble()) {
            return false;
        }

        GasStack gasContent = CanisterContainerSuppliers.getFirstAvailableGasContent(player);
        if (gasContent.isEmpty()) {
            return false;
        }

        int gasCost = getGasCost(player);
        if (gasCost < 0) {
            return false;
        }

        MobEffectInstance existingEffect = player.getEffect(MobEffects.DOLPHINS_GRACE);
        return existingEffect == null || existingEffect.getAmplifier() == 0 && existingEffect.endsWithin(DURATION_THRESHOLD);
    }

    @Override
    public void applyEffect(@NotNull Player player) {
        player.addEffect(new MobEffectInstance(MobEffects.DOLPHINS_GRACE, DURATION_THRESHOLD, 0, true, false));
    }
}
