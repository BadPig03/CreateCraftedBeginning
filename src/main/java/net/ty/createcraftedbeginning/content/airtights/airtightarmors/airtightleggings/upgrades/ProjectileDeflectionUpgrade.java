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
import net.minecraft.world.phys.Vec3;
import net.ty.createcraftedbeginning.CreateCraftedBeginning;
import net.ty.createcraftedbeginning.api.gas.armorhandlers.AirtightArmorsHandler;
import net.ty.createcraftedbeginning.api.gas.canisters.CanisterContainerSuppliers;
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
public enum ProjectileDeflectionUpgrade implements AirtightUpgrade {
    INSTANCE;

    @Override
    public @Unmodifiable List<Component> getComponents(Player player, ItemStack item) {
        if (getGasConsumptionMultiplier(player) == 0) {
            return List.of(CCBLang.translateDirect("gui.gas_consumption.supply_require_only"));
        }
        return List.of(CCBLang.translateDirect("gui.airtight_leggings.projectile_deflection_upgrade.gas_cost"));
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
        return false;
    }

    @Override
    public CCBIcons getIcon() {
        return CCBIcons.I_PROJECTILE_DEFLECTION;
    }

    @Override
    public Component getDescription() {
        return CCBLang.translateDirect("gui.airtight_leggings.projectile_deflection_upgrade.description");
    }

    @Override
    public Component getTitle() {
        return CCBLang.translateDirect("gui.airtight_leggings.projectile_deflection_upgrade");
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
        return Items.SHIELD;
    }

    @Override
    public ResourceLocation getID() {
        return CreateCraftedBeginning.asResource("projectile_deflection");
    }

    @Override
    public void applyEffect(Player player) {
    }

    @Override
    public float getGasConsumptionMultiplier(Player player) {
        return CCBConfig.server().equipments.projectileDeflectionMultiplier.getF();
    }

    @Override
    public boolean isActive(Player player, ItemStack item) {
        return item.is(CCBItems.AIRTIGHT_LEGGINGS) && AirtightUpgrade.super.isActive(player, item);
    }

    public boolean canApply(Player player, Vec3 movement) {
        if (!isActive(player, player.getItemBySlot(EquipmentSlot.LEGS))) {
            return false;
        }

        Gas gasType = CanisterContainerSuppliers.getFirstAvailableGasContent(player).getGasType();
        AirtightArmorsHandler armorsHandler = AirtightArmorsHandler.REGISTRY.get(gasType);
        if (armorsHandler == null) {
            return false;
        }

        if (player.level().isClientSide) {
            return true;
        }

        float consumption = (float) movement.length();
        return GlobalAirtightUpgradesConsumptionManager.tryConsumeGas(player, this, EquipmentSlot.LEGS, consumption);
    }
}
