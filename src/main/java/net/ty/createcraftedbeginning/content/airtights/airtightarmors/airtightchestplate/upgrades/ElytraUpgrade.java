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
import org.jetbrains.annotations.Unmodifiable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public enum ElytraUpgrade implements AirtightUpgrade {
    INSTANCE;

    private static final ResourceLocation ID = CCBAPI.asResource("elytra");
    private static final Couple<Integer> OFFSET = Couple.create(36, 31);
    private static final int BOOST_COOLDOWN_TICKS = 40;
    private static final int BOOST_PULSE_TICKS = 10;
    private static final Map<UUID, BoostPulse> CLIENT_BOOST_PULSES = new HashMap<>();
    private static final Map<UUID, BoostPulse> SERVER_BOOST_PULSES = new HashMap<>();

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

    public static boolean shouldInterceptBoostInput(Player player) {
        if (!player.isFallFlying() || !player.getMainHandItem().isEmpty()) {
            return false;
        }

        ItemStack chestplate = player.getItemBySlot(EquipmentSlot.CHEST);
        return hasEnabledUpgrade(chestplate);
    }

    public static boolean canRequestBoost(Player player) {
        if (!shouldInterceptBoostInput(player) || hasActiveBoostPulse(player)) {
            return false;
        }

        ItemStack chestplate = player.getItemBySlot(EquipmentSlot.CHEST);
        return isBoostReady(player, chestplate);
    }

    public static boolean tryStartClientBoostPulse(Player player) {
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
        return startBoostPulse(player, boostMultiplier);
    }

    public static void tryStartServerBoostPulse(Player player) {
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
        if (!GasConsumptionMath.isFinite(boostMultiplier) || boostMultiplier <= 0) {
            return;
        }

        if (!CanisterContainerConsumers.interactContainer(player, selectedFuel, () -> true, false)) {
            GasInteractionFeedback.sendWarningFeedback(player, "gui.warnings.insufficient_gas", selectedFuel.gasContent().getHoverName());
            return;
        }

        if (!startBoostPulse(player, boostMultiplier)) {
            return;
        }

        player.getCooldowns().addCooldown(chestplate.getItem(), BOOST_COOLDOWN_TICKS);
    }

    public static boolean tickBoostPulse(Player player) {
        Map<UUID, BoostPulse> boostPulses = getBoostPulses(player);
        UUID playerId = player.getUUID();
        BoostPulse pulse = boostPulses.get(playerId);
        if (pulse == null) {
            return false;
        }

        if (!canContinueBoostPulse(player)) {
            boostPulses.remove(playerId);
            return false;
        }

        long gameTime = player.level().getGameTime();
        if (pulse.lastAppliedGameTime() == gameTime) {
            return false;
        }

        Vec3 currentMovement = player.getDeltaMovement();
        Vec3 nextMovement = applyPulseStep(currentMovement, player.getLookAngle(), pulse.boostMultiplier());
        player.setDeltaMovement(nextMovement);
        player.hasImpulse = true;
        int ticksRemaining = pulse.ticksRemaining() - 1;
        if (ticksRemaining <= 0) {
            boostPulses.remove(playerId);
            return true;
        }

        boostPulses.put(playerId, new BoostPulse(pulse.boostMultiplier(), ticksRemaining, gameTime));
        return true;
    }

    public static void clearBoostPulse(Player player) {
        getBoostPulses(player).remove(player.getUUID());
    }

    public static void clearClientBoostPulses() {
        CLIENT_BOOST_PULSES.clear();
    }

    public static Vec3 applyPulseStep(Vec3 currentMovement, Vec3 lookDirection, float boostMultiplier) {
        if (!Float.isFinite(boostMultiplier) || boostMultiplier <= 0) {
            return currentMovement;
        }

        double directionLengthSqr = lookDirection.lengthSqr();
        if (!Double.isFinite(directionLengthSqr) || directionLengthSqr <= 1.0E-12) {
            return currentMovement;
        }

        double impulse = 1.0 / BOOST_PULSE_TICKS * boostMultiplier;
        if (!Double.isFinite(impulse)) {
            return currentMovement;
        }

        Vec3 normalizedLook = lookDirection.scale(1.0 / Math.sqrt(directionLengthSqr));
        Vec3 nextMovement = currentMovement.add(normalizedLook.scale(impulse));
        if (!Double.isFinite(nextMovement.x) || !Double.isFinite(nextMovement.y) || !Double.isFinite(nextMovement.z)) {
            return currentMovement;
        }

        return nextMovement;
    }

    public boolean canFly(Player player, ItemStack item) {
        return item.is(CCBItems.AIRTIGHT_CHESTPLATE) && isEnabled(item) && !CanisterContainerSuppliers.getFirstAvailableGasContent(player).isEmpty();
    }

    private static boolean hasEnabledUpgrade(ItemStack chestplate) {
        return chestplate.is(CCBItems.AIRTIGHT_CHESTPLATE) && INSTANCE.isEnabled(chestplate);
    }

    private static boolean isBoostReady(Player player, ItemStack chestplate) {
        return hasEnabledUpgrade(chestplate) && !player.getCooldowns().isOnCooldown(chestplate.getItem());
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

    private static Map<UUID, BoostPulse> getBoostPulses(Player player) {
        if (player.level().isClientSide) {
            return CLIENT_BOOST_PULSES;
        }

        return SERVER_BOOST_PULSES;
    }

    private static boolean hasActiveBoostPulse(Player player) {
        return getBoostPulses(player).containsKey(player.getUUID());
    }

    private static boolean canContinueBoostPulse(Player player) {
        ItemStack chestplate = player.getItemBySlot(EquipmentSlot.CHEST);
        return player.isFallFlying() && hasEnabledUpgrade(chestplate);
    }

    private static boolean startBoostPulse(Player player, float boostMultiplier) {
        if (!GasConsumptionMath.isFinite(boostMultiplier) || boostMultiplier <= 0 || hasActiveBoostPulse(player)) {
            return false;
        }

        getBoostPulses(player).put(player.getUUID(), new BoostPulse(boostMultiplier, BOOST_PULSE_TICKS, Long.MIN_VALUE));
        return tickBoostPulse(player);
    }

    private record BoostPulse(float boostMultiplier, int ticksRemaining, long lastAppliedGameTime) {}
}
