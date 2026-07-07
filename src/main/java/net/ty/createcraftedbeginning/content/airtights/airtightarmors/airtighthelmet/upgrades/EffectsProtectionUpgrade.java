package net.ty.createcraftedbeginning.content.airtights.airtightarmors.airtighthelmet.upgrades;

import net.createmod.catnip.data.Couple;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.ty.createcraftedbeginning.CreateCraftedBeginning;
import net.ty.createcraftedbeginning.api.armorhandlers.AirtightArmorsHandler;
import net.ty.createcraftedbeginning.api.armorhandlers.AirtightArmorsHandlerUtils;
import net.ty.createcraftedbeginning.api.gascanisters.CanisterContainerSuppliers;
import net.ty.createcraftedbeginning.api.gas.gases.Gas;
import net.ty.createcraftedbeginning.config.CCBConfig;
import net.ty.createcraftedbeginning.content.airtights.airtightupgrades.AirtightUpgrade;
import net.ty.createcraftedbeginning.content.airtights.airtightupgrades.GlobalAirtightUpgradesConsumptionManager;
import net.ty.createcraftedbeginning.data.CCBIcons;
import net.ty.createcraftedbeginning.data.CCBLang;
import net.ty.createcraftedbeginning.registry.CCBItems;
import org.jetbrains.annotations.Unmodifiable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public enum EffectsProtectionUpgrade implements AirtightUpgrade {
    INSTANCE;

    @Override
    public @Unmodifiable List<Component> getComponents(Player player, ItemStack item) {
        if (getGasConsumptionMultiplier(player) == 0) {
            return List.of(CCBLang.translateDirect("gui.gas_consumption.supply_require_only"));
        }
        return List.of(CCBLang.translateDirect("gui.airtight_helmet.effects_protection_upgrade.gas_cost"));
    }

    @Override
    public boolean canApply(Player player) {
        return isActive(player, player.getItemBySlot(EquipmentSlot.HEAD));
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
        return CCBIcons.I_EFFECTS_PROTECTION;
    }

    @Override
    public Component getDescription() {
        return CCBLang.translateDirect("gui.airtight_helmet.effects_protection_upgrade.description");
    }

    @Override
    public Component getTitle() {
        return CCBLang.translateDirect("gui.airtight_helmet.effects_protection_upgrade");
    }

    @Override
    public Couple<Integer> getOffset() {
        return Couple.create(36, 31);
    }

    @Override
    public int getGasConsumptionPerSecond(Player player, ItemStack item) {
        return 0;
    }

    @Override
    public int getIndex() {
        return 0;
    }

    @Override
    public Item getUpgradeItem() {
        return Items.MILK_BUCKET;
    }

    @Override
    public ResourceLocation getID() {
        return CreateCraftedBeginning.asResource("effects_protection");
    }

    @Override
    public void applyEffect(Player player) {
    }

    @Override
    public float getGasConsumptionMultiplier(Player player) {
        return CCBConfig.server().equipments.effectsProtectionMultiplier.getF();
    }

    @Override
    public boolean isActive(Player player, ItemStack item) {
        return item.is(CCBItems.AIRTIGHT_HELMET) && AirtightUpgrade.super.isActive(player, item);
    }

    public boolean canApply(Player player, MobEffectInstance effectInstance) {
        if (!canApply(player)) {
            return false;
        }

        Gas gasType = CanisterContainerSuppliers.getFirstAvailableGasContent(player).getGasType();
        AirtightArmorsHandler armorsHandler = AirtightArmorsHandlerUtils.of(gasType);
        if (!armorsHandler.canCureEffect(effectInstance)) {
            return false;
        }

        if (player.level().isClientSide) {
            return true;
        }

        int consumption = (effectInstance.getAmplifier() + 1) * effectInstance.getDuration();
        return GlobalAirtightUpgradesConsumptionManager.tryConsumeGas(player, this, EquipmentSlot.HEAD, consumption);
    }
}
