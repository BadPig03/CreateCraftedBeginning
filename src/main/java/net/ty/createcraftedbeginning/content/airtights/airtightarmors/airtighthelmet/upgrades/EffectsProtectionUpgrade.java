package net.ty.createcraftedbeginning.content.airtights.airtightarmors.airtighthelmet.upgrades;

import net.createmod.catnip.data.Couple;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
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

public enum EffectsProtectionUpgrade implements AirtightUpgrade {
    INSTANCE;

    public static boolean canApply(@NotNull Player player, MobEffectInstance effectInstance) {
        ItemStack helmet = player.getItemBySlot(EquipmentSlot.HEAD);
        if (!helmet.is(CCBItems.AIRTIGHT_HELMET) || !INSTANCE.isEnabled(helmet) || !CanisterContainerSuppliers.isAnyContainerAvailable(player)) {
            return false;
        }

        GasStack gasContent = CanisterContainerSuppliers.getFirstAvailableGasContent(player);
        if (gasContent.isEmpty()) {
            return false;
        }

        Gas gasType = gasContent.getGasType();
        AirtightArmorsHandler armorsHandler = AirtightArmorsHandler.REGISTRY.get(gasType);
        if (armorsHandler == null || !armorsHandler.canCureEffect(effectInstance)) {
            return false;
        }

        float multiplier = CCBConfig.server().equipments.effectsProtectionGasCostMultiplier.getF();
        int gasConsumption = Mth.ceil((effectInstance.getAmplifier() + 1) * effectInstance.getDuration() * multiplier * armorsHandler.getConsumptionMultiplier()[0]);
        return CanisterContainerConsumers.interactContainer(player, gasType, gasConsumption, () -> !player.level().isClientSide);
    }

    @Override
    public int getIndex() {
        return 0;
    }

    @Override
    public @NotNull ResourceLocation getID() {
        return CreateCraftedBeginning.asResource("effects_protection");
    }

    @Override
    public Item getUpgradeItem() {
        return Items.MILK_BUCKET;
    }

    @Override
    public @NotNull Couple<Integer> getOffset() {
        return Couple.create(36, 31);
    }

    @Override
    public CCBIcons getIcon() {
        return CCBIcons.I_EFFECTS_PROTECTION;
    }

    @Override
    public boolean isRightIndicator() {
        return false;
    }

    @Override
    public @NotNull Component getTitle() {
        return CCBLang.translateDirect("gui.airtight_helmet.effects_protection_upgrade");
    }

    @Override
    public @NotNull Component getDescription() {
        return CCBLang.translateDirect("gui.airtight_helmet.effects_protection_upgrade.description");
    }

    @Override
    public @Nullable Component getGasCostComponent(Player player) {
        int gasCost = getGasCost(player);
        if (gasCost < 0) {
            return null;
        }

        return CCBLang.translateDirect("gui.airtight_helmet.effects_protection_upgrade.gas_cost");
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
    public boolean canApply(Player player) {
        return false;
    }

    @Override
    public void applyEffect(Player player) {
    }
}
