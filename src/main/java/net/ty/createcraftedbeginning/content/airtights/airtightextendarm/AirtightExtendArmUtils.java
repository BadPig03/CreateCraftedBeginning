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

        Optional<AffordableFuel> affordableFuel = findAffordableFuel(player);
        if (affordableFuel.isEmpty()) {
            removeArmModifiers(player);
            return;
        }

        AffordableFuel selectedFuel = affordableFuel.get();
        ACTIVE_FUELS.put(player, selectedFuel);
        applyArmModifiers(player, AirtightArmHandlerUtils.of(selectedFuel.gasType()));
    }

    static boolean isHoldingArms(Player player) {
        return player.getMainHandItem().is(CCBItems.AIRTIGHT_EXTEND_ARM) || player.getOffhandItem().is(CCBItems.AIRTIGHT_EXTEND_ARM);
    }

    static PowerUseResult tryUseBlockPower(Player player, BlockPos blockPos) {
        return tryUsePower(player, () -> player.canInteractWithBlock(blockPos, 0), () -> requiresExtendedBlockRange(player, blockPos));
    }

    static PowerUseResult tryUseEntityPower(Player player, Entity targetEntity) {
        return tryUsePower(player, () -> player.canInteractWithEntity(targetEntity, 0), () -> requiresExtendedEntityRange(player, targetEntity));
    }

    static PowerUseResult tryUseAttackPower(Player player, Entity targetEntity) {
        return tryUsePower(player, () -> player.canInteractWithEntity(targetEntity, 0), () -> requiresPoweredAttack(player, targetEntity));
    }

    private static Optional<AffordableFuel> findAffordableFuel(Player player) {
        return CanisterContainerConsumers.findAffordableFuel(player, getSelectedGasType(player), gasType -> getRawGasConsumption(AirtightArmHandlerUtils.of(gasType)));
    }

    private static boolean requiresExtendedBlockRange(Player player, BlockPos blockPos) {
        AttributeInstance blockRangeAttribute = player.getAttributes().getInstance(Attributes.BLOCK_INTERACTION_RANGE);
        if (blockRangeAttribute == null) {
            return false;
        }

        double unpoweredRangeAdjustment = getAdjustmentWithoutModifier(blockRangeAttribute, BLOCK_RANGE_MODIFIER_ID);
        return unpoweredRangeAdjustment < 0 && !player.canInteractWithBlock(blockPos, unpoweredRangeAdjustment);
    }

    private static boolean requiresExtendedEntityRange(Player player, Entity targetEntity) {
        AttributeInstance entityRangeAttribute = player.getAttributes().getInstance(Attributes.ENTITY_INTERACTION_RANGE);
        if (entityRangeAttribute == null) {
            return false;
        }

        double unpoweredRangeAdjustment = getAdjustmentWithoutModifier(entityRangeAttribute, ENTITY_RANGE_MODIFIER_ID);
        return unpoweredRangeAdjustment < 0 && !player.canInteractWithEntity(targetEntity, unpoweredRangeAdjustment);
    }

    private static boolean requiresPoweredAttack(Player player, Entity targetEntity) {
        AttributeInstance knockbackAttribute = player.getAttributes().getInstance(Attributes.ATTACK_KNOCKBACK);
        return requiresExtendedEntityRange(player, targetEntity) || getModifierAmount(knockbackAttribute) > 0;
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

        ConsumptionResult consumptionResult = consumeCurrentFuelAndRefresh(player);
        if (!consumptionResult.success()) {
            return PowerUseResult.insufficient(consumptionResult.attemptedGas());
        }
        return PowerUseResult.consumed();
    }

    private static ConsumptionResult consumeCurrentFuelAndRefresh(Player player) {
        AffordableFuel selectedFuel = ACTIVE_FUELS.get(player);
        if (selectedFuel == null) {
            return ConsumptionResult.failure(getSelectedGasType(player));
        }

        GasStack attemptedGas = selectedFuel.gasContent().copy();
        boolean fuelConsumed = CanisterContainerConsumers.interactContainer(player, selectedFuel.gasType(), selectedFuel.amount(), () -> true, false);
        refreshArmModifiers(player);
        return new ConsumptionResult(fuelConsumed, attemptedGas);
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

    private static double getModifierAmount(@Nullable AttributeInstance attribute) {
        if (attribute == null) {
            return 0;
        }

        AttributeModifier knockbackModifier = attribute.getModifier(KNOCKBACK_MODIFIER_ID);
        return knockbackModifier == null || knockbackModifier.operation() != Operation.ADD_VALUE ? 0 : knockbackModifier.amount();
    }

    private static double getAdjustmentWithoutModifier(AttributeInstance attribute, ResourceLocation modifierId) {
        AttributeModifier modifier = attribute.getModifier(modifierId);
        if (modifier == null || modifier.amount() <= 0) {
            return 0;
        }

        double currentAttributeValue = attribute.getValue();
        AttributeInstance attributeWithoutModifier = new AttributeInstance(attribute.getAttribute(), ignored -> {});
        attributeWithoutModifier.replaceFrom(attribute);
        attributeWithoutModifier.removeModifier(modifierId);
        return attributeWithoutModifier.getValue() - currentAttributeValue;
    }

    private static void removeModifier(@Nullable AttributeInstance attribute, ResourceLocation modifierId) {
        if (attribute == null) {
            return;
        }

        attribute.removeModifier(modifierId);
    }

    private static void syncModifier(@Nullable AttributeInstance attribute, ResourceLocation modifierId, double modifierAmount) {
        if (attribute == null) {
            return;
        }

        if (!GasConsumptions.isFinite(modifierAmount) || modifierAmount <= 0) {
            attribute.removeModifier(modifierId);
            return;
        }

        AttributeModifier currentModifier = attribute.getModifier(modifierId);
        if (currentModifier != null && Double.compare(currentModifier.amount(), modifierAmount) == 0 && currentModifier.operation() == Operation.ADD_VALUE) {
            return;
        }

        attribute.addOrUpdateTransientModifier(new AttributeModifier(modifierId, modifierAmount, Operation.ADD_VALUE));
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

        private static PowerUseResult insufficient(Gas gasType) {
            return new PowerUseResult(PowerUseOutcome.INSUFFICIENT_GAS, gasType.isEmpty() ? GasStack.EMPTY : new GasStack(gasType, 1));
        }

        private static PowerUseResult insufficient(GasStack attemptedGas) {
            return new PowerUseResult(PowerUseOutcome.INSUFFICIENT_GAS, attemptedGas);
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

        private static ConsumptionResult failure(Gas gasType) {
            return new ConsumptionResult(false, gasType.isEmpty() ? GasStack.EMPTY : new GasStack(gasType, 1));
        }
    }
}
