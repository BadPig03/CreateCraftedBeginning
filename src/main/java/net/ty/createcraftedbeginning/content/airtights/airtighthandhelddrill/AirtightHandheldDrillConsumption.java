package net.ty.createcraftedbeginning.content.airtights.airtighthandhelddrill;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.entity.player.Player;
import net.ty.createcraftedbeginning.api.canister.GasConsumptionMath;
import net.ty.createcraftedbeginning.content.airtights.gascanister.container.CanisterContainerConsumers.AffordableFuel;
import net.ty.createcraftedbeginning.content.airtights.gascanister.container.CanisterContainerConsumers.ConsumptionBatch;
import net.ty.createcraftedbeginning.foundation.transaction.ResourceTransaction;
import net.ty.createcraftedbeginning.gas.interaction.GasInteractionFeedback;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.function.BooleanSupplier;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
final class AirtightHandheldDrillConsumption {
    private final Player player;
    private final AffordableFuel fuel;
    private final ConsumptionBatch batch;
    private double successfulConsumption;
    private boolean failed;

    AirtightHandheldDrillConsumption(Player player, AffordableFuel fuel) {
        this.player = player;
        this.fuel = fuel;
        batch = new ConsumptionBatch(fuel.gasType());
    }

    boolean execute(double rawCost, BooleanSupplier action) {
        if (failed) {
            return false;
        }

        double nextConsumption = successfulConsumption + rawCost;
        long nextAmount = GasConsumptionMath.roundUp(nextConsumption);
        if (!GasConsumptionMath.isNonNegativeFinite(rawCost) || nextAmount < 0) {
            failed = true;
            return false;
        }

        long amount = nextAmount - GasConsumptionMath.roundUp(successfulConsumption);
        Attempt attempt = new Attempt(action);
        boolean committed = batch.execute(player, new AffordableFuel(fuel.gasContent(), fuel.sourcePressurePa(), amount), ResourceTransaction.participant(() -> true, () -> Boolean.TRUE, attempt::execute, ignored -> {}));
        if (committed) {
            successfulConsumption = nextConsumption;
        }
        else if (!attempt.started) {
            failed = true;
            GasInteractionFeedback.sendWarningFeedback(player, "gui.warnings.insufficient_gas", fuel.gasContent().getHoverName());
        }
        if (attempt.failure instanceof RuntimeException exception) {
            throw exception;
        }
        if (attempt.failure instanceof Error error) {
            throw error;
        }
        return committed;
    }

    boolean hasFailed() {
        return failed;
    }

    private static final class Attempt {
        private final BooleanSupplier action;
        private boolean started;
        private @Nullable Throwable failure;

        private Attempt(BooleanSupplier action) {
            this.action = action;
        }

        private boolean execute() {
            started = true;
            try {
                return action.getAsBoolean();
            }
            catch (RuntimeException | Error throwable) {
                failure = throwable;
                return true;
            }
        }
    }
}
