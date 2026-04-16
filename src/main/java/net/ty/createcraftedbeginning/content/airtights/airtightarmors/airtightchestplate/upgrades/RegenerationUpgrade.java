package net.ty.createcraftedbeginning.content.airtights.airtightarmors.airtightchestplate.upgrades;

import net.createmod.catnip.data.Couple;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.ty.createcraftedbeginning.CreateCraftedBeginning;
import net.ty.createcraftedbeginning.api.gas.armorhandlers.AirtightArmorsHandler;
import net.ty.createcraftedbeginning.api.gas.cansiters.CanisterContainerConsumers;
import net.ty.createcraftedbeginning.api.gas.cansiters.CanisterContainerSuppliers;
import net.ty.createcraftedbeginning.api.gas.gases.Gas;
import net.ty.createcraftedbeginning.api.gas.gases.GasStack;
import net.ty.createcraftedbeginning.config.CCBConfig;
import net.ty.createcraftedbeginning.content.airtights.airtightupgrades.AirtightUpgrade;
import net.ty.createcraftedbeginning.data.CCBIcons;
import net.ty.createcraftedbeginning.data.CCBLang;
import net.ty.createcraftedbeginning.registry.CCBItems;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public enum RegenerationUpgrade implements AirtightUpgrade {
    INSTANCE;

    private static final int DURATION_THRESHOLD = 30;
    private static final int DURATION_LIMIT = 10;

    @Override
    public int getIndex() {
        return 3;
    }

    @Override
    public @NotNull ResourceLocation getID() {
        return CreateCraftedBeginning.asResource("regeneration");
    }

    @Override
    public Item getUpgradeItem() {
        return Items.ENCHANTED_GOLDEN_APPLE;
    }

    @Override
    public @NotNull Couple<Integer> getOffset() {
        return Couple.create(132, 31);
    }

    @Override
    public CCBIcons getIcon() {
        return CCBIcons.I_REGENERATION;
    }

    @Override
    public boolean isRightIndicator() {
        return true;
    }

    @Override
    public @NotNull Component getTitle() {
        return CCBLang.translateDirect("gui.airtight_chestplate.regeneration_upgrade");
    }

    @Override
    public @NotNull Component getDescription() {
        return CCBLang.translateDirect("gui.airtight_chestplate.regeneration_upgrade.description");
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

        Gas gasType = gasContent.getGasType();
        AirtightArmorsHandler armorsHandler = AirtightArmorsHandler.REGISTRY.get(gasType);
        if (armorsHandler == null) {
            return -1;
        }

        return Mth.ceil(CCBConfig.server().equipments.regenerationGasCost.get() * armorsHandler.getConsumptionMultiplier()[0]);
    }

    @Override
    public boolean canApply(@NotNull Player player) {
        if (player.getHealth() >= player.getMaxHealth()) {
            return false;
        }

        ItemStack chestplate = player.getItemBySlot(EquipmentSlot.CHEST);
        if (!chestplate.is(CCBItems.AIRTIGHT_CHESTPLATE) || !isEnabled(chestplate) || !CanisterContainerSuppliers.isAnyContainerAvailable(player)) {
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

        MobEffectInstance existingEffect = player.getEffect(MobEffects.REGENERATION);
        if (existingEffect != null && (existingEffect.getAmplifier() > 2 || !existingEffect.endsWithin(DURATION_LIMIT))) {
            return false;
        }

        Level level = player.level();
        return CanisterContainerConsumers.interactContainer(player, gasContent.getGasType(), gasCost, () -> !level.isClientSide);
    }

    @Override
    public void applyEffect(@NotNull Player player) {
        player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, DURATION_THRESHOLD, 2, true, false));
    }
}
