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
import net.ty.createcraftedbeginning.content.airtights.airtighthandhelddrill.upgrades.LiquidReplacementUpgrade;
import net.ty.createcraftedbeginning.content.airtights.airtighthandhelddrill.upgrades.MagnetUpgrade;
import net.ty.createcraftedbeginning.content.airtights.airtighthandhelddrill.upgrades.SilkTouchUpgrade;
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

    static float calculateGasConsumptionForBlock(Level level, BlockPos blockPos, boolean silkTouch, boolean magnet, boolean experienceConversion, boolean liquidReplacement) {
        BlockState blockState = level.getBlockState(blockPos);
        if (blockState.getDestroySpeed(level, blockPos) == 0) {
            return 0;
        }

        float blockConsumption = 0;
        int perBlockConsumption = CCBConfig.server().equipment.airtightHandheldDrill.gasPerBlock.get();
        float liquidReplacementConsumption = CCBConfig.server().equipment.airtightHandheldDrill.liquidReplacementGasMultiplier.getF() * perBlockConsumption;
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
            blockConsumption *= (SilkTouchUpgrade.BASE_GAS_MULTIPLIER * CCBConfig.server().equipment.airtightHandheldDrill.silkTouchGasMultiplier.getF());
        }
        if (magnet) {
            blockConsumption *= (MagnetUpgrade.BASE_GAS_MULTIPLIER * CCBConfig.server().equipment.airtightHandheldDrill.magnetGasMultiplier.getF());
        }

        if (!experienceConversion) {
            return blockConsumption;
        }

        return blockConsumption * (ExperienceConversionUpgrade.BASE_GAS_MULTIPLIER * CCBConfig.server().equipment.airtightHandheldDrill.experienceConversionGasMultiplier.getF());
    }

    static Optional<AffordableFuel> findAffordableDrillFuel(Player player, ItemStack drill, AirtightHandheldDrillMiningContext context) {
        double[] baseGasConsumption = {Double.NaN};
        return CanisterContainerConsumers.findAffordableFuel(player, usageContext -> {
            if (Double.isNaN(baseGasConsumption[0])) {
                baseGasConsumption[0] = calculateBaseGasConsumption(drill, context);
            }
            return calculateRawGasConsumption(baseGasConsumption[0], usageContext.gasType(), usageContext.sourcePressurePa());
        });
    }

    static double calculateRawGasConsumption(double baseGasConsumption, Gas gasType, long sourcePressurePa) {
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

        boolean silkTouch = SilkTouchUpgrade.INSTANCE.canApply(drill);
        boolean magnet = MagnetUpgrade.INSTANCE.canApply(drill);
        boolean experienceConversion = ExperienceConversionUpgrade.INSTANCE.canApply(drill);
        boolean liquidReplacement = LiquidReplacementUpgrade.INSTANCE.canApply(drill);
        return destructionPos.stream().mapToDouble(pos -> calculateGasConsumptionForBlock(context.level(), pos, silkTouch, magnet, experienceConversion, liquidReplacement)).sum();
    }
}
