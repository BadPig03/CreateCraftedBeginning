package net.ty.createcraftedbeginning.content.airtights.airtightarmors.airtightchestplate.upgrades;

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
import net.ty.createcraftedbeginning.api.CCBAPI;
import net.ty.createcraftedbeginning.api.armorhandlers.AirtightArmorsHandler;
import net.ty.createcraftedbeginning.api.armorhandlers.AirtightArmorsHandlers;
import net.ty.createcraftedbeginning.api.canister.GasConsumptionMath;
import net.ty.createcraftedbeginning.config.CCBConfig;
import net.ty.createcraftedbeginning.content.airtights.airtightupgrades.AirtightUpgrade;
import net.ty.createcraftedbeginning.content.airtights.airtightupgrades.AirtightUpgradeIcon;
import net.ty.createcraftedbeginning.content.airtights.airtightupgrades.AirtightUpgradePowerMode;
import net.ty.createcraftedbeginning.content.airtights.gascanister.container.CanisterContainerConsumers;
import net.ty.createcraftedbeginning.content.airtights.gascanister.container.CanisterContainerConsumers.AffordableFuel;
import net.ty.createcraftedbeginning.content.airtights.gascanister.container.CanisterContainerSuppliers;
import net.ty.createcraftedbeginning.foundation.lang.CCBLang;
import net.ty.createcraftedbeginning.gas.interaction.GasInteractionFeedback;
import net.ty.createcraftedbeginning.registry.CCBItems;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;
import java.util.Optional;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public enum ElytraUpgrade implements AirtightUpgrade {
    INSTANCE;

    private static final ResourceLocation ID = CCBAPI.asResource("elytra");
    private static final Couple<Integer> OFFSET = Couple.create(36, 31);
    private static final int BOOST_COOLDOWN_TICKS = 40;
    private static final double BOOST_FORWARD_IMPULSE = 0.1;
    private static final double BOOST_TARGET_SPEED = 1.5;
    private static final double BOOST_ALIGNMENT_FACTOR = 0.8;

    public static boolean shouldInterceptBoostInput(Player player) {
        if (!player.isFallFlying() || !player.getMainHandItem().isEmpty()) {
            return false;
        }

        ItemStack chestplate = player.getItemBySlot(EquipmentSlot.CHEST);
        return chestplate.is(CCBItems.AIRTIGHT_CHESTPLATE) && INSTANCE.isEnabled(chestplate);
    }

    public static boolean canRequestBoost(Player player) {
        if (!shouldInterceptBoostInput(player)) {
            return false;
        }

        ItemStack chestplate = player.getItemBySlot(EquipmentSlot.CHEST);
        return !player.getCooldowns().isOnCooldown(chestplate.getItem());
    }

    public static boolean applyClientSpeedBoost(Player player) {
        if (!player.level().isClientSide || !canRequestBoost(player)) {
            return false;
        }

        ItemStack chestplate = player.getItemBySlot(EquipmentSlot.CHEST);
        Optional<AffordableFuel> boostFuel = findBoostFuel(player, chestplate);
        if (boostFuel.isEmpty()) {
            return false;
        }

        AffordableFuel selectedFuel = boostFuel.get();
        float boostMultiplier = AirtightArmorsHandlers.resolveForEquipment(selectedFuel.gasType()).getMultiplierForBoostingElytra();
        Vec3 nextMovement = calculateBoostMovement(player.getDeltaMovement(), player.getLookAngle(), boostMultiplier);
        if (nextMovement == null) {
            return false;
        }

        player.setDeltaMovement(nextMovement);
        player.hasImpulse = true;
        player.getCooldowns().addCooldown(chestplate.getItem(), BOOST_COOLDOWN_TICKS);
        return true;
    }

    public static void tryApplySpeedBoost(Player player) {
        if (player.level().isClientSide || !canRequestBoost(player)) {
            return;
        }

        ItemStack chestplate = player.getItemBySlot(EquipmentSlot.CHEST);
        Optional<AffordableFuel> boostFuel = findBoostFuel(player, chestplate);
        if (boostFuel.isEmpty()) {
            GasInteractionFeedback.sendWarningFeedback(player, "gui.warnings.insufficient_gas");
            return;
        }

        AffordableFuel selectedFuel = boostFuel.get();
        float boostMultiplier = AirtightArmorsHandlers.resolveForEquipment(selectedFuel.gasType()).getMultiplierForBoostingElytra();
        Vec3 nextMovement = calculateBoostMovement(player.getDeltaMovement(), player.getLookAngle(), boostMultiplier);
        if (nextMovement == null) {
            return;
        }

        if (!CanisterContainerConsumers.interactContainer(player, selectedFuel, () -> true, false)) {
            GasInteractionFeedback.sendWarningFeedback(player, "gui.warnings.insufficient_gas", selectedFuel.gasContent().getHoverName());
            return;
        }

        player.setDeltaMovement(nextMovement);
        player.hasImpulse = true;
        player.getCooldowns().addCooldown(chestplate.getItem(), BOOST_COOLDOWN_TICKS);
    }

    private static @Nullable Vec3 calculateBoostMovement(Vec3 currentMovement, Vec3 lookDirection, float boostMultiplier) {
        if (!GasConsumptionMath.isFinite(boostMultiplier) || boostMultiplier <= 0) {
            return null;
        }

        double directionLengthSqr = lookDirection.lengthSqr();
        if (!Double.isFinite(directionLengthSqr) || directionLengthSqr <= 1.0E-12) {
            return null;
        }

        Vec3 forward = lookDirection.scale(1.0 / Math.sqrt(directionLengthSqr));
        double forwardSpeed = currentMovement.dot(forward);
        Vec3 lateralMovement = currentMovement.subtract(forward.scale(forwardSpeed));
        double alignment = Math.min(BOOST_ALIGNMENT_FACTOR * boostMultiplier, 1);
        double forwardImpulse = (BOOST_FORWARD_IMPULSE + BOOST_ALIGNMENT_FACTOR * Math.max(0, BOOST_TARGET_SPEED - forwardSpeed)) * boostMultiplier;
        Vec3 nextMovement = currentMovement.subtract(lateralMovement.scale(alignment)).add(forward.scale(forwardImpulse));
        if (!Double.isFinite(nextMovement.x) || !Double.isFinite(nextMovement.y) || !Double.isFinite(nextMovement.z)) {
            return null;
        }

        return nextMovement;
    }

    private static Optional<AffordableFuel> findBoostFuel(Player player, ItemStack chestplate) {
        int baseGasCost = INSTANCE.getGasConsumptionPerSecond(player, chestplate);
        return CanisterContainerConsumers.findAffordableFuel(player, context -> {
            AirtightArmorsHandler armorHandler = AirtightArmorsHandlers.resolveForEquipment(context.gasType());
            float boostMultiplier = armorHandler.getMultiplierForBoostingElytra();
            if (!GasConsumptionMath.isFinite(boostMultiplier) || boostMultiplier <= 0) {
                return -1;
            }

            return baseGasCost * armorHandler.getConsumptionMultiplier(EquipmentSlot.CHEST);
        });
    }

    @Override
    public @Unmodifiable List<Component> getComponents(Player player, ItemStack item) {
        int gasCost = getGasConsumptionPerSecond(player, item);
        if (gasCost == 0) {
            return List.of(CCBLang.translateDirect("gui.gas_consumption.supply_require_only"));
        }

        return List.of(CCBLang.translateDirect("gui.gas_consumption_per_boost", gasCost));
    }

    @Override
    public boolean canApply(Player player) {
        return isActive(player, player.getItemBySlot(EquipmentSlot.CHEST));
    }

    @Override
    public boolean meetsConditions(Player player, ItemStack item) {
        return player.isFallFlying();
    }

    @Override
    public boolean isRightIndicator() {
        return false;
    }

    @Override
    public AirtightUpgradeIcon getIcon() {
        return AirtightUpgradeIcon.ELYTRA;
    }

    @Override
    public Component getDescription() {
        return CCBLang.translateDirect("gui.airtight_chestplate.elytra_upgrade.description");
    }

    @Override
    public Component getTitle() {
        return CCBLang.translateDirect("gui.airtight_chestplate.elytra_upgrade");
    }

    @Override
    public Couple<Integer> getOffset() {
        return OFFSET;
    }

    @Override
    public AirtightUpgradePowerMode getPowerMode() {
        return AirtightUpgradePowerMode.ON_DEMAND;
    }

    @Override
    public int getGasConsumptionPerSecond(Player player, ItemStack item) {
        return CCBConfig.server().equipment.airtightChestplate.gasPerElytraBoost.get();
    }

    @Override
    public Item getDefaultUpgradeItem() {
        return Items.ELYTRA;
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
        return item.is(CCBItems.AIRTIGHT_CHESTPLATE) && AirtightUpgrade.super.isActive(player, item);
    }

    public boolean canFly(Player player, ItemStack item) {
        return item.is(CCBItems.AIRTIGHT_CHESTPLATE) && isEnabled(item) && !CanisterContainerSuppliers.getFirstAvailableGasContent(player).isEmpty();
    }
}
