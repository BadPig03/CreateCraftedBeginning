package net.ty.createcraftedbeginning.content.breezes.breezechamber;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.ty.createcraftedbeginning.api.gas.GasPressure;
import net.ty.createcraftedbeginning.api.gas.GasStack;
import net.ty.createcraftedbeginning.api.gas.pressure.GasPressureCompartment;
import net.ty.createcraftedbeginning.config.CCBConfig;
import net.ty.createcraftedbeginning.content.breezes.breezechamber.BreezeChamberBlockEntity.ChargerType;
import net.ty.createcraftedbeginning.content.breezes.breezechamber.BreezeChamberRecipeIndex.GasConversion;
import net.ty.createcraftedbeginning.gas.atmosphere.AtmosphereStateResolver;
import net.ty.createcraftedbeginning.gas.storage.GasTank;
import net.ty.createcraftedbeginning.recipe.gas.consumption.GasConsumptionPlan;
import net.ty.createcraftedbeginning.recipe.gas.consumption.GasConsumptionPlanner;
import org.jetbrains.annotations.ApiStatus.Internal;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;
import java.util.Optional;

@Internal
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class BreezeChamberConversionPlanner {
    private final BreezeChamberBlockEntity chamber;

    @Internal
    public BreezeChamberConversionPlanner(BreezeChamberBlockEntity chamber) {
        this.chamber = chamber;
    }

    @Internal
    public int getProcessingAmount(int windTime) {
        if (windTime == 0 || chamber.getLevel() == null) {
            return 0;
        }

        GasTank outputTank = chamber.getTankBehaviourInternal().getPrimaryHandler();
        long ambientPressurePa = getAmbientPressurePa();
        long pressureLimitPa = Math.min(ambientPressurePa, outputTank.getMaxPressurePa());
        if (pressureLimitPa <= GasPressure.VACUUM_PA) {
            return 0;
        }

        long outputPressurePa = outputTank.getPressurePa();
        if (outputPressurePa >= pressureLimitPa) {
            return 0;
        }

        long pressureDifferencePa = Math.max(0, ambientPressurePa - outputPressurePa);
        int maxProcessingRate = Math.max(1, CCBConfig.server().machines.breezeChamber.maxProcessingPerSecond.get());
        double pressureStrength = Math.min(1.0, (double) pressureDifferencePa / ambientPressurePa);
        return Mth.clamp((int) Math.round(maxProcessingRate * pressureStrength), 1, maxProcessingRate);
    }

    static boolean canAcceptOutputPhysically(GasTank outputTank, GasStack outputStack) {
        return !outputStack.isEmpty() && outputStack.getAmount() > 0 && (outputTank.isEmpty() || GasStack.isSameGasSameComponents(outputTank.getGasStack(), outputStack)) && outputTank.getRemainingAmount() >= outputStack.getAmount();
    }

    Optional<GasConversion> findProcessingTarget(ChargerType chargerType, GasStack inputStack, GasPressureCompartment inputTank, GasTank outputTank) {
        for (GasConversion conversion : getConversions(chargerType, inputStack)) {
            if (!canAcceptProcessingOutput(outputTank, conversion.output()) || GasConsumptionPlanner.plan(conversion.input(), inputTank).isEmpty()) {
                continue;
            }

            return Optional.of(conversion);
        }

        return Optional.empty();
    }

    @Internal
    public Optional<GasConversionPlan> planConversion(GasConversion conversion, GasPressureCompartment inputTank, GasTank outputTank, long processingBudget) {
        long inputAmount = conversion.input().amount();
        GasStack outputPerBatch = conversion.output();
        long outputAmount = outputPerBatch.getAmount();
        if (processingBudget < inputAmount || !canAcceptProcessingOutput(outputTank, outputPerBatch)) {
            return Optional.empty();
        }

        long outputHeadroom = getProcessingOutputHeadroom(outputTank);
        long maximumBatchCount = Math.min(processingBudget / inputAmount, outputHeadroom / outputAmount);
        long batchCount = GasConsumptionPlanner.findMaximumMultiplier(conversion.input(), inputTank, maximumBatchCount);
        if (batchCount <= 0) {
            return Optional.empty();
        }

        Optional<GasConsumptionPlan> inputPlan = GasConsumptionPlanner.plan(conversion.input(), inputTank, batchCount);
        return inputPlan.map(plan -> new GasConversionPlan(conversion, plan, batchCount));
    }

    long retainableProcessingCredit(ChargerType chargerType, GasPressureCompartment inputTank, GasTank outputTank, long remainingBudget) {
        if (remainingBudget <= 0 || inputTank.getStoredAmount() <= 0) {
            return 0;
        }

        Optional<GasConversion> target = findProcessingTarget(chargerType, inputTank.getGasStack(), inputTank, outputTank);
        return target.map(conversion -> Math.min(remainingBudget, conversion.input().amount() - 1)).orElse(0L);
    }

    List<GasConversion> getConversions(ChargerType chargerType, GasStack inputStack) {
        Level level = chamber.getLevel();
        if (level == null || inputStack.isEmpty()) {
            return List.of();
        }

        return switch (chargerType) {
            case NORMAL -> BreezeChamberRecipeIndex.findEnergizationCandidates(level.getRecipeManager(), inputStack);
            case BAD -> BreezeChamberRecipeIndex.findDissipationCandidates(level.getRecipeManager(), inputStack);
            case NONE -> List.of();
        };
    }

    @Internal
    public record GasConversionPlan(GasConversion conversion, GasConsumptionPlan inputPlan, long batchCount) {}

    private boolean canAcceptProcessingOutput(GasTank outputTank, GasStack outputStack) {
        return canAcceptOutputPhysically(outputTank, outputStack) && getProcessingOutputHeadroom(outputTank) >= outputStack.getAmount();
    }

    private long getAmbientPressurePa() {
        Level level = chamber.getLevel();
        if (level == null) {
            return GasPressure.VACUUM_PA;
        }

        return Math.max(GasPressure.VACUUM_PA, AtmosphereStateResolver.resolvePressurePa(level, chamber.getBlockPos()));
    }

    private long getProcessingPressureLimitPa(GasTank outputTank) {
        return Math.min(getAmbientPressurePa(), outputTank.getMaxPressurePa());
    }

    private long getProcessingOutputHeadroom(GasTank outputTank) {
        long pressureLimitPa = getProcessingPressureLimitPa(outputTank);
        long pressureLimitedCapacity = GasPressure.amount(outputTank.getVolume(), pressureLimitPa);
        long maximumProcessingAmount = Math.min(outputTank.getMaxAmount(), pressureLimitedCapacity);
        return Math.max(0, maximumProcessingAmount - outputTank.getStoredAmount());
    }
}
