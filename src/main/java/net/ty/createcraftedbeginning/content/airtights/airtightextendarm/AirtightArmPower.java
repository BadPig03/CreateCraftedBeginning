package net.ty.createcraftedbeginning.content.airtights.airtightextendarm;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.ty.createcraftedbeginning.api.armhandlers.AirtightArmHandler;
import net.ty.createcraftedbeginning.api.armhandlers.AirtightArmHandlers;
import net.ty.createcraftedbeginning.api.gas.Gas;
import net.ty.createcraftedbeginning.api.gas.GasStack;
import net.ty.createcraftedbeginning.config.CCBConfig;
import net.ty.createcraftedbeginning.content.airtights.gascanister.container.CanisterContainerClients;
import net.ty.createcraftedbeginning.content.airtights.gascanister.container.CanisterContainerConsumers;
import net.ty.createcraftedbeginning.content.airtights.gascanister.container.CanisterContainerConsumers.AffordableFuel;
import net.ty.createcraftedbeginning.registry.CCBItems;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Map;
import java.util.Optional;
import java.util.WeakHashMap;
import java.util.function.BooleanSupplier;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
final class AirtightArmPower {
    private static final int POWER_REFRESH_INTERVAL = 5;
    private static final Map<Player, AffordableFuel> ACTIVE_FUELS = new WeakHashMap<>();

    private AirtightArmPower() {
    }

    static void tick(Player player) {
        if (player.level().isClientSide) {
            return;
        }

        if (!isHoldingArms(player)) {
            removeArmModifiers(player);
            return;
        }

        if (!Mth.isMultipleOf(player.tickCount, POWER_REFRESH_INTERVAL)) {
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

        Optional<AffordableFuel> affordableFuel = CanisterContainerConsumers.findAffordableFuel(player, getSelectedGasType(player), context -> {
            AirtightArmHandler armHandler = AirtightArmHandlers.resolveForEquipment(context.gasType());
            return CCBConfig.server().equipment.airtightExtendArm.gasPerPoweredAction.get() * armHandler.getGasConsumptionMultiplier();
        });
        if (affordableFuel.isEmpty()) {
            removeArmModifiers(player);
            return;
        }

        AffordableFuel selectedFuel = affordableFuel.get();
        ACTIVE_FUELS.put(player, selectedFuel);
        AirtightArmAttributes.applyArmModifiers(player, AirtightArmHandlers.resolveForEquipment(selectedFuel.gasType()));
    }

    static boolean isHoldingArms(Player player) {
        return player.getMainHandItem().is(CCBItems.AIRTIGHT_EXTEND_ARM) || player.getOffhandItem().is(CCBItems.AIRTIGHT_EXTEND_ARM);
    }

    static PowerUseResult tryUseBlockPower(Player player, BlockPos blockPos) {
        return tryUsePower(player, () -> player.canInteractWithBlock(blockPos, 0), () -> AirtightArmAttributes.requiresExtendedBlockRange(player, blockPos));
    }

    static PowerUseResult tryUseEntityPower(Player player, Entity targetEntity) {
        return tryUsePower(player, () -> player.canInteractWithEntity(targetEntity, 0), () -> AirtightArmAttributes.requiresExtendedEntityRange(player, targetEntity));
    }

    static PowerUseResult tryUseAttackPower(Player player, Entity targetEntity) {
        return tryUsePower(player, () -> player.canInteractWithEntity(targetEntity, 0), () -> AirtightArmAttributes.requiresPoweredAttack(player, targetEntity));
    }

    private static PowerUseResult tryUsePower(Player player, BooleanSupplier canReachWithCurrentPower, BooleanSupplier requiresCurrentPower) {
        if (player.level().isClientSide || !isHoldingArms(player)) {
            return PowerUseResult.pass();
        }

        refreshArmModifiers(player);
        if (!canReachWithCurrentPower.getAsBoolean()) {
            if (ACTIVE_FUELS.containsKey(player)) {
                return PowerUseResult.outOfRange();
            }

            return PowerUseResult.insufficient(getSelectedGasType(player));
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
        boolean fuelConsumed = CanisterContainerConsumers.interactContainer(player, selectedFuel, () -> true, false);
        refreshArmModifiers(player);
        return new ConsumptionResult(fuelConsumed, attemptedGas);
    }

    private static Gas getSelectedGasType(Player player) {
        if (player.level().isClientSide) {
            return CanisterContainerClients.getDisplayedGasContent().getGasType();
        }

        return CanisterContainerClients.getStoredGasType(player);
    }

    private static void removeArmModifiers(Player player) {
        ACTIVE_FUELS.remove(player);

        AirtightArmAttributes.removeArmModifiers(player);
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
            if (gasType.isEmpty()) {
                return new PowerUseResult(PowerUseOutcome.INSUFFICIENT_GAS, GasStack.EMPTY);
            }

            return new PowerUseResult(PowerUseOutcome.INSUFFICIENT_GAS, new GasStack(gasType, 1));
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
            if (gasType.isEmpty()) {
                return new ConsumptionResult(false, GasStack.EMPTY);
            }

            return new ConsumptionResult(false, new GasStack(gasType, 1));
        }
    }
}
