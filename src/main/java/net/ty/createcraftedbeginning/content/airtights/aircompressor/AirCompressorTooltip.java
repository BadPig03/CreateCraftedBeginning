package net.ty.createcraftedbeginning.content.airtights.aircompressor;

import com.simibubi.create.content.kinetics.base.IRotate.StressImpact;
import net.minecraft.ChatFormatting;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.ty.createcraftedbeginning.api.gas.gases.GasAmountUtils;
import net.ty.createcraftedbeginning.api.gas.gases.GasStack;
import net.ty.createcraftedbeginning.foundation.lang.CCBLang;
import net.ty.createcraftedbeginning.platform.CCBClientBridge;
import net.ty.createcraftedbeginning.recipe.PressurizationRecipe;
import net.ty.createcraftedbeginning.registry.CCBBlocks;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
final class AirCompressorTooltip {
    private AirCompressorTooltip() {
    }

    static boolean addHoveringInformation(List<Component> tooltip, @Nullable Level level, GasStack inputGas, boolean overStressed, boolean isSpeedRequirementFulfilled, float speed) {
        boolean hasInvalidGasWarning = false;
        if (isInputGasInvalid(level, inputGas)) {
            CCBLang.translate("gui.invalid_ingredient").style(ChatFormatting.GOLD).forGoggles(tooltip);
            CCBLang.addToGoggles(tooltip, "gui.air_compressor.invalid_gas", inputGas.getHoverName());
            hasInvalidGasWarning = true;
        }
        if (overStressed && CCBClientBridge.isOverstressedTooltipEnabled()) {
            if (hasInvalidGasWarning) {
                tooltip.add(CommonComponents.EMPTY);
            }
            CCBLang.translate("gui.overstressed").style(ChatFormatting.GOLD).forGoggles(tooltip);
            CCBLang.addToGoggles(tooltip, "gui.network_overstressed");
            return true;
        }

        if (isSpeedRequirementFulfilled || speed == 0) {
            return hasInvalidGasWarning;
        }

        if (hasInvalidGasWarning) {
            tooltip.add(CommonComponents.EMPTY);
        }
        CCBLang.translate("gui.speed_requirement").style(ChatFormatting.GOLD).forGoggles(tooltip);
        CCBLang.addToGoggles(tooltip, "gui.not_fast_enough", Component.translatable(CCBBlocks.AIR_COMPRESSOR_BLOCK.get().getDescriptionId()));
        return true;
    }

    static void addGoggleInformation(List<Component> tooltip, boolean isPlayerSneaking, OverheatState overheatState, GasStack inputGas, GasStack outputGas, double stressApplied, float theoreticalSpeed) {
        CCBLang.translate("gui.air_compressor").forGoggles(tooltip);
        CCBLang.translate("gui.air_compressor.overheat_state").style(ChatFormatting.GRAY).forGoggles(tooltip);
        CCBLang.translate(overheatState.getTranslationKey()).style(overheatState.getDisplayColor()).forGoggles(tooltip, 1);
        if (isPlayerSneaking) {
            addTankDetails(tooltip, inputGas, outputGas);
        }
        if (!StressImpact.isEnabled()) {
            return;
        }

        tooltip.add(CommonComponents.EMPTY);
        CCBLang.translate("gui.stress_impact").style(ChatFormatting.GRAY).forGoggles(tooltip);
        CCBLang.number(stressApplied * Mth.abs(theoreticalSpeed)).translate("gui.unit.stress").style(ChatFormatting.AQUA).space().add(CCBLang.translate("gui.at_current_speed").style(ChatFormatting.DARK_GRAY)).forGoggles(tooltip, 1);
    }

    private static boolean isInputGasInvalid(@Nullable Level level, GasStack inputGas) {
        return level != null && !inputGas.isEmpty() && PressurizationRecipe.findRecipe(level, inputGas).isEmpty();
    }

    private static void addTankDetails(List<Component> tooltip, GasStack inputGas, GasStack outputGas) {
        long tankCapacity = AirCompressorProcessing.getTankCapacity();
        tooltip.add(CommonComponents.EMPTY);
        addTankTooltip(tooltip, "gui.air_compressor.input_capacity", inputGas, tankCapacity);
        tooltip.add(CommonComponents.EMPTY);
        addTankTooltip(tooltip, "gui.air_compressor.output_capacity", outputGas, tankCapacity);
    }

    private static void addTankTooltip(List<Component> tooltip, String titleKey, GasStack gas, long tankCapacity) {
        CCBLang.translate(titleKey).style(ChatFormatting.GRAY).forGoggles(tooltip);
        if (gas.isEmpty()) {
            CCBLang.gasName(GasStack.EMPTY).style(ChatFormatting.GRAY).forGoggles(tooltip, 1);
            GasAmountUtils.precise(tankCapacity).style(ChatFormatting.GOLD).forGoggles(tooltip, 1);
            return;
        }

        CCBLang.gasName(gas).style(ChatFormatting.GRAY).forGoggles(tooltip, 1);
        GasAmountUtils.precise(gas.getAmount()).style(ChatFormatting.GOLD).text(ChatFormatting.GRAY, " / ").add(GasAmountUtils.precise(tankCapacity).style(ChatFormatting.DARK_GRAY)).forGoggles(tooltip, 1);
    }
}
