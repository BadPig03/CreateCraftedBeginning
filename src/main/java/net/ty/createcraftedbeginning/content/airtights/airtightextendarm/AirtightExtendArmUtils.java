package net.ty.createcraftedbeginning.content.airtights.airtightextendarm;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeMap;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.ty.createcraftedbeginning.api.CCBAPI;
import net.ty.createcraftedbeginning.api.armhandlers.AirtightArmHandler;
import net.ty.createcraftedbeginning.api.armhandlers.AirtightArmHandlerUtils;
import net.ty.createcraftedbeginning.api.gas.gases.Gas;
import net.ty.createcraftedbeginning.api.gas.gases.GasStack;
import net.ty.createcraftedbeginning.api.gascanisters.GasConsumptions;
import net.ty.createcraftedbeginning.config.CCBConfig;
import net.ty.createcraftedbeginning.content.airtights.gascanister.container.CanisterContainerClients;
import net.ty.createcraftedbeginning.content.airtights.gascanister.container.CanisterContainerConsumers;
import net.ty.createcraftedbeginning.content.airtights.gascanister.container.CanisterContainerConsumers.AffordableFuel;
import net.ty.createcraftedbeginning.registry.CCBItems;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Map;
import java.util.Optional;
import java.util.WeakHashMap;
import java.util.function.BooleanSupplier;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
final class AirtightExtendArmUtils {
    private static final ResourceLocation BLOCK_RANGE_MODIFIER_ID = CCBAPI.asResource("airtight_extend_arm_block_range");
    private static final ResourceLocation ENTITY_RANGE_MODIFIER_ID = CCBAPI.asResource("airtight_extend_arm_entity_range");
    private static final ResourceLocation KNOCKBACK_MODIFIER_ID = CCBAPI.asResource("airtight_extend_arm_knockback");
    private static final int POWER_REFRESH_INTERVAL = 5;
    private static final Map<Player, AffordableFuel> ACTIVE_FUELS = new WeakHashMap<>();

    private AirtightExtendArmUtils() {
    }

    static void tick(Player player) {
        if (player.level().isClientSide) {
            return;
        }

        if (!isHoldingArms(player)) {
            removeArmModifiers(player);
            return;
        }

        if (player.tickCount % POWER_REFRESH_INTERVAL != 0) {
            return;
        }

        refreshArmModifiers(player);
    }

    static void refreshArmModifiers(Player player) {
        if (player.level().isClientSide) {
            return;
        }

        if (!isHoldingArms(player)) {
            removeArmModifiers(player);
            return;
        }

        Optional<AffordableFuel> fuel = findAffordableFuel(player);
        if (fuel.isEmpty()) {
            removeArmModifiers(player);
            return;
        }

        AffordableFuel selectedFuel = fuel.get();
        ACTIVE_FUELS.put(player, selectedFuel);
        applyArmModifiers(player, AirtightArmHandlerUtils.of(selectedFuel.gasType()));
    }

    static boolean isHoldingArms(Player player) {
        return player.getMainHandItem().is(CCBItems.AIRTIGHT_EXTEND_ARM) || player.getOffhandItem().is(CCBItems.AIRTIGHT_EXTEND_ARM);
    }

    static PowerUseResult tryUseBlockPower(Player player, BlockPos pos) {
        return tryUsePower(player, () -> player.canInteractWithBlock(pos, 0), () -> requiresExtendedBlockRange(player, pos));
    }

    static PowerUseResult tryUseEntityPower(Player player, Entity target) {
        return tryUsePower(player, () -> player.canInteractWithEntity(target, 0), () -> requiresExtendedEntityRange(player, target));
    }

    static PowerUseResult tryUseAttackPower(Player player, Entity target) {
        return tryUsePower(player, () -> player.canInteractWithEntity(target, 0), () -> requiresPoweredAttack(player, target));
    }

    private static Optional<AffordableFuel> findAffordableFuel(Player player) {
        Gas selectedGas = getSelectedGasType(player);
        return CanisterContainerConsumers.findAffordableFuel(player, selectedGas, gasType -> getRawGasConsumption(AirtightArmHandlerUtils.of(gasType)));
    }

    private static boolean requiresExtendedBlockRange(Player player, BlockPos pos) {
        AttributeInstance instance = player.getAttributes().getInstance(Attributes.BLOCK_INTERACTION_RANGE);
        if (instance == null) {
            return false;
        }

        double rangeAdjustment = getAdjustmentWithoutModifier(instance, BLOCK_RANGE_MODIFIER_ID);
        return rangeAdjustment < 0 && !player.canInteractWithBlock(pos, rangeAdjustment);
    }

    private static boolean requiresExtendedEntityRange(Player player, Entity target) {
        AttributeInstance instance = player.getAttributes().getInstance(Attributes.ENTITY_INTERACTION_RANGE);
        if (instance == null) {
            return false;
        }

        double rangeAdjustment = getAdjustmentWithoutModifier(instance, ENTITY_RANGE_MODIFIER_ID);
        return rangeAdjustment < 0 && !player.canInteractWithEntity(target, rangeAdjustment);
    }

    private static boolean requiresPoweredAttack(Player player, Entity target) {
        AttributeInstance knockback = player.getAttributes().getInstance(Attributes.ATTACK_KNOCKBACK);
        return requiresExtendedEntityRange(player, target) || getModifierAmount(knockback) > 0;
    }

    private static PowerUseResult tryUsePower(Player player, BooleanSupplier canReachWithCurrentPower, BooleanSupplier requiresCurrentPower) {
        if (player.level().isClientSide || !isHoldingArms(player)) {
            return PowerUseResult.pass();
        }

        refreshArmModifiers(player);
        if (!canReachWithCurrentPower.getAsBoolean()) {
            return ACTIVE_FUELS.containsKey(player) ? PowerUseResult.outOfRange() : PowerUseResult.insufficient(getSelectedGasType(player));
        }

        if (!requiresCurrentPower.getAsBoolean()) {
            return PowerUseResult.pass();
        }

        ConsumptionResult consumption = consumeCurrentFuelAndRefresh(player);
        return consumption.success() ? PowerUseResult.consumed() : PowerUseResult.insufficient(consumption.attemptedGas());
    }

    private static ConsumptionResult consumeCurrentFuelAndRefresh(Player player) {
        AffordableFuel selectedFuel = ACTIVE_FUELS.get(player);
        if (selectedFuel == null) {
            return ConsumptionResult.failure(getSelectedGasType(player));
        }

        GasStack attemptedGas = selectedFuel.gasContent().copy();
        boolean consumed = CanisterContainerConsumers.interactContainer(player, selectedFuel.gasType(), selectedFuel.amount(), () -> true, false);
        refreshArmModifiers(player);
        return new ConsumptionResult(consumed, attemptedGas);
    }

    private static Gas getSelectedGasType(Player player) {
        if (player.level().isClientSide) {
            return CanisterContainerClients.getDisplayedGasContent().getGasType();
        }
        return CanisterContainerClients.getStoredGasType(player);
    }

    private static double getRawGasConsumption(AirtightArmHandler armHandler) {
        return CCBConfig.server().equipments.perUseConsumption.get() * armHandler.getGasConsumptionMultiplier();
    }

    private static void removeArmModifiers(Player player) {
        ACTIVE_FUELS.remove(player);

        AttributeMap attributes = player.getAttributes();
        removeModifier(attributes.getInstance(Attributes.BLOCK_INTERACTION_RANGE), BLOCK_RANGE_MODIFIER_ID);
        removeModifier(attributes.getInstance(Attributes.ENTITY_INTERACTION_RANGE), ENTITY_RANGE_MODIFIER_ID);
        removeModifier(attributes.getInstance(Attributes.ATTACK_KNOCKBACK), KNOCKBACK_MODIFIER_ID);
    }

    private static void applyArmModifiers(Player player, AirtightArmHandler armHandler) {
        AttributeMap attributes = player.getAttributes();
        syncModifier(attributes.getInstance(Attributes.BLOCK_INTERACTION_RANGE), BLOCK_RANGE_MODIFIER_ID, armHandler.getIncreasedBlockInteractionRange());
        syncModifier(attributes.getInstance(Attributes.ENTITY_INTERACTION_RANGE), ENTITY_RANGE_MODIFIER_ID, armHandler.getIncreasedEntityInteractionRange());
        syncModifier(attributes.getInstance(Attributes.ATTACK_KNOCKBACK), KNOCKBACK_MODIFIER_ID, armHandler.getIncreasedKnockback());
    }

    private static double getModifierAmount(@Nullable AttributeInstance instance) {
        if (instance == null) {
            return 0;
        }

        AttributeModifier modifier = instance.getModifier(KNOCKBACK_MODIFIER_ID);
        return modifier == null || modifier.operation() != Operation.ADD_VALUE ? 0 : modifier.amount();
    }

    private static double getAdjustmentWithoutModifier(AttributeInstance instance, ResourceLocation id) {
        AttributeModifier modifier = instance.getModifier(id);
        if (modifier == null || modifier.amount() <= 0) {
            return 0;
        }

        double currentValue = instance.getValue();
        AttributeInstance copy = new AttributeInstance(instance.getAttribute(), ignored -> {});
        copy.replaceFrom(instance);
        copy.removeModifier(id);
        return copy.getValue() - currentValue;
    }

    private static void removeModifier(@Nullable AttributeInstance instance, ResourceLocation id) {
        if (instance == null) {
            return;
        }

        instance.removeModifier(id);
    }

    private static void syncModifier(@Nullable AttributeInstance instance, ResourceLocation id, double amount) {
        if (instance == null) {
            return;
        }

        if (!GasConsumptions.isFinite(amount) || amount <= 0) {
            instance.removeModifier(id);
            return;
        }

        AttributeModifier currentModifier = instance.getModifier(id);
        if (currentModifier != null && Double.compare(currentModifier.amount(), amount) == 0 && currentModifier.operation() == Operation.ADD_VALUE) {
            return;
        }

        instance.addOrUpdateTransientModifier(new AttributeModifier(id, amount, Operation.ADD_VALUE));
    }

    private enum PowerUseOutcome {
        PASS,
        CONSUMED,
        INSUFFICIENT_GAS,
        OUT_OF_RANGE
    }

    record PowerUseResult(PowerUseOutcome outcome, GasStack attemptedGas) {
        PowerUseResult {
            attemptedGas = attemptedGas.copy();
        }

        private static PowerUseResult pass() {
            return new PowerUseResult(PowerUseOutcome.PASS, GasStack.EMPTY);
        }

        private static PowerUseResult consumed() {
            return new PowerUseResult(PowerUseOutcome.CONSUMED, GasStack.EMPTY);
        }

        private static PowerUseResult insufficient(Gas gas) {
            return new PowerUseResult(PowerUseOutcome.INSUFFICIENT_GAS, gas.isEmpty() ? GasStack.EMPTY : new GasStack(gas, 1));
        }

        private static PowerUseResult insufficient(GasStack gas) {
            return new PowerUseResult(PowerUseOutcome.INSUFFICIENT_GAS, gas);
        }

        private static PowerUseResult outOfRange() {
            return new PowerUseResult(PowerUseOutcome.OUT_OF_RANGE, GasStack.EMPTY);
        }

        boolean allowed() {
            return outcome == PowerUseOutcome.PASS || outcome == PowerUseOutcome.CONSUMED;
        }

        boolean shouldWarn() {
            return outcome == PowerUseOutcome.INSUFFICIENT_GAS;
        }
    }

    private record ConsumptionResult(boolean success, GasStack attemptedGas) {
        private ConsumptionResult {
            attemptedGas = attemptedGas.copy();
        }

        private static ConsumptionResult failure(Gas gas) {
            return new ConsumptionResult(false, gas.isEmpty() ? GasStack.EMPTY : new GasStack(gas, 1));
        }
    }
}
