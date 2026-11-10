package net.ty.createcraftedbeginning.content.airtights.airtighthandhelddrill;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.ty.createcraftedbeginning.api.canister.GasConsumptionMath;
import net.ty.createcraftedbeginning.api.drillhandlers.AirtightDrillHandler;
import net.ty.createcraftedbeginning.api.drillhandlers.AirtightDrillHandlers;
import net.ty.createcraftedbeginning.api.gas.Gas;
import net.ty.createcraftedbeginning.api.gas.GasStack;
import net.ty.createcraftedbeginning.config.CCBConfig;
import net.ty.createcraftedbeginning.content.airtights.airtighthandhelddrill.upgrades.ExperienceConversionUpgrade;
import net.ty.createcraftedbeginning.content.airtights.airtighthandhelddrill.upgrades.HarvestOptimizationUpgrade;
import net.ty.createcraftedbeginning.content.airtights.airtighthandhelddrill.upgrades.LiquidReplacementUpgrade;
import net.ty.createcraftedbeginning.content.airtights.airtighthandhelddrill.upgrades.MagnetUpgrade;
import net.ty.createcraftedbeginning.content.airtights.gascanister.container.CanisterContainerConsumers;
import net.ty.createcraftedbeginning.content.airtights.gascanister.container.CanisterContainerConsumers.AffordableFuel;
import net.ty.createcraftedbeginning.content.airtights.gascanister.container.CanisterContainerSuppliers;
import net.ty.createcraftedbeginning.gas.interaction.GasInteractionFeedback;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Optional;
import java.util.Set;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
final class AirtightHandheldDrillFuel {
    private AirtightHandheldDrillFuel() {
    }

    static double calculateGasConsumptionForBlock(Level level, BlockPos blockPos, boolean silkTouch, boolean magnet, boolean experienceConversion, boolean liquidReplacement) {
        BlockState blockState = level.getBlockState(blockPos);
        if (blockState.getDestroySpeed(level, blockPos) == 0) {
            return 0;
        }

        double blockConsumption = 0;
        int perBlockConsumption = CCBConfig.server().equipment.airtightHandheldDrill.gasPerBlock.get();
        double liquidReplacementConsumption = CCBConfig.server().equipment.airtightHandheldDrill.liquidReplacementGasMultiplier.get() * perBlockConsumption;
        if (blockState.getBlock() instanceof LiquidBlock) {
            if (liquidReplacement) {
                return liquidReplacementConsumption;
            }

            return blockConsumption;
        }

        if (liquidReplacement && !blockState.getFluidState().is(Fluids.EMPTY)) {
            blockConsumption += liquidReplacementConsumption;
        }

        blockConsumption += perBlockConsumption;
        if (silkTouch && !experienceConversion) {
            blockConsumption *= CCBConfig.server().equipment.airtightHandheldDrill.harvestOptimizationGasMultiplier.get();
        }
        if (magnet) {
            blockConsumption *= MagnetUpgrade.BASE_GAS_MULTIPLIER * CCBConfig.server().equipment.airtightHandheldDrill.magnetGasMultiplier.get();
        }

        if (!experienceConversion) {
            return blockConsumption;
        }

        return blockConsumption * (ExperienceConversionUpgrade.BASE_GAS_MULTIPLIER * CCBConfig.server().equipment.airtightHandheldDrill.experienceConversionGasMultiplier.get());
    }

    static Optional<AffordableFuel> findAffordableDrillFuel(Player player, ItemStack drill, AirtightHandheldDrillMiningContext context) {
        double[] baseGasConsumption = {Double.NaN};
        return CanisterContainerConsumers.findAffordableFuel(player, usageContext -> {
            if (Double.isNaN(baseGasConsumption[0])) {
                baseGasConsumption[0] = calculateBaseGasConsumption(drill, context);
            }
            return calculateRawGasConsumption(baseGasConsumption[0], usageContext.gasType());
        });
    }

    static AirtightHandheldDrillMiningContext limitToAffordableTargets(Player player, ItemStack drill, AirtightHandheldDrillMiningContext context) {
        int targetCount = context.destructionPos().size();
        if (targetCount <= 1) {
            return context;
        }

        boolean silkTouch = HarvestOptimizationUpgrade.INSTANCE.canApply(drill);
        boolean magnet = MagnetUpgrade.INSTANCE.canApply(drill);
        boolean experienceConversion = ExperienceConversionUpgrade.INSTANCE.canApply(drill);
        boolean liquidReplacement = LiquidReplacementUpgrade.INSTANCE.canApply(drill);
        double[] cumulativeCosts = new double[targetCount + 1];
        int targetIndex = 0;
        for (BlockPos pos : context.destructionPos()) {
            cumulativeCosts[targetIndex + 1] = cumulativeCosts[targetIndex] + calculateGasConsumptionForBlock(context.level(), pos, silkTouch, magnet, experienceConversion, liquidReplacement);
            targetIndex++;
        }

        double fullCost = cumulativeCosts[targetCount];
        if (CanisterContainerConsumers.findAffordableFuel(player, usageContext -> calculateRawGasConsumption(fullCost, usageContext.gasType())).isPresent()) {
            return context;
        }

        int lowerLimit = 1;
        int upperLimit = targetCount - 1;
        int affordableCount = 0;
        while (lowerLimit <= upperLimit) {
            int candidateCount = lowerLimit + (upperLimit - lowerLimit) / 2;
            double candidateCost = cumulativeCosts[candidateCount];
            if (CanisterContainerConsumers.findAffordableFuel(player, usageContext -> calculateRawGasConsumption(candidateCost, usageContext.gasType())).isEmpty()) {
                upperLimit = candidateCount - 1;
                continue;
            }

            affordableCount = candidateCount;
            lowerLimit = candidateCount + 1;
        }
        return context.withTargetLimit(Math.max(1, affordableCount));
    }

    static double calculateRawGasConsumption(double baseGasConsumption, Gas gasType) {
        if (!GasConsumptionMath.isNonNegativeFinite(baseGasConsumption)) {
            return -1;
        }

        AirtightDrillHandler drillHandler = AirtightDrillHandlers.resolveForEquipment(gasType);
        double rawGasConsumption = baseGasConsumption * drillHandler.getConsumptionMultiplier();
        if (!GasConsumptionMath.isNonNegativeFinite(rawGasConsumption)) {
            return -1;
        }

        return rawGasConsumption;
    }

    static void displayInsufficientGasWarning(Player player) {
        GasStack gasContent = CanisterContainerSuppliers.getFirstAvailableGasContent(player);
        if (gasContent.isEmpty()) {
            GasInteractionFeedback.sendWarningFeedback(player, "gui.warnings.insufficient_gas");
            return;
        }

        GasInteractionFeedback.sendWarningFeedback(player, "gui.warnings.insufficient_gas", gasContent.getHoverName());
    }

    private static double calculateBaseGasConsumption(ItemStack drill, AirtightHandheldDrillMiningContext context) {
        Set<BlockPos> destructionPos = context.destructionPos();
        if (destructionPos.isEmpty()) {
            return -1;
        }

        boolean silkTouch = HarvestOptimizationUpgrade.INSTANCE.canApply(drill);
        boolean magnet = MagnetUpgrade.INSTANCE.canApply(drill);
        boolean experienceConversion = ExperienceConversionUpgrade.INSTANCE.canApply(drill);
        boolean liquidReplacement = LiquidReplacementUpgrade.INSTANCE.canApply(drill);
        return destructionPos.stream().mapToDouble(pos -> calculateGasConsumptionForBlock(context.level(), pos, silkTouch, magnet, experienceConversion, liquidReplacement)).sum();
    }
}
