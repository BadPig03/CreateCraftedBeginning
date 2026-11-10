package net.ty.createcraftedbeginning.content.breezes.breezechamber;

import net.minecraft.ChatFormatting;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.ty.createcraftedbeginning.api.gas.Gas;
import net.ty.createcraftedbeginning.api.gas.handler.GasStorageHandler;
import net.ty.createcraftedbeginning.content.breezes.breezechamber.BreezeChamberBlock.WindLevel;
import net.ty.createcraftedbeginning.foundation.lang.CCBLang;
import net.ty.createcraftedbeginning.gas.visual.GasUnitsTooltips;
import net.ty.createcraftedbeginning.platform.client.GoggleTooltip;
import net.ty.createcraftedbeginning.platform.client.GoggleTooltip.Section;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
final class BreezeChamberDisplay {
    private final BreezeChamberBlockEntity chamber;

    BreezeChamberDisplay(BreezeChamberBlockEntity chamber) {
        this.chamber = chamber;
    }

    boolean addToGoggleTooltip(List<Component> tooltip) {
        Level level = chamber.getLevel();
        if (level == null) {
            return false;
        }

        WindLevel windLevel = chamber.getWindLevel();
        CCBLang.translate("gui.breeze_chamber").forGoggles(tooltip);
        CCBLang.translate("gui.breeze_chamber.current_state").style(ChatFormatting.GRAY).forGoggles(tooltip);
        CCBLang.translate(windLevel.getTranslatable()).style(windLevel.getChatFormatting()).forGoggles(tooltip, 1);

        BreezeChamberGasProcessor gasProcessor = chamber.getGasProcessorInternal();
        boolean isControllerActive = gasProcessor.isControllerActive();
        int remainingTime = chamber.getWindRemainingTime();
        if (windLevel != WindLevel.CALM) {
            if (GoggleTooltip.isVisible(tooltip, Section.BREEZE_TIME)) {
                CCBLang.translate("gui.breeze_chamber.remaining_time").style(ChatFormatting.GRAY).forGoggles(tooltip);
                ChatFormatting timeColor = remainingTime > 0 ? ChatFormatting.GREEN : ChatFormatting.RED;
                if (chamber.isCreative()) {
                    CCBLang.translate("gui.gas_container.infinity").style(timeColor).forGoggles(tooltip, 1);
                }
                else {
                    CCBLang.seconds(remainingTime, level.tickRateManager().tickrate()).style(timeColor).forGoggles(tooltip, 1);
                }
            }
            if (isControllerActive) {
                CCBLang.translate("gui.breeze_chamber.energization_level").style(ChatFormatting.GRAY).forGoggles(tooltip);
                CCBLang.translate("gui.breeze_chamber.current_level", CCBLang.number(chamber.getWindRemainingLevel())).style(ChatFormatting.BLUE).forGoggles(tooltip, 1);
            }
        }
        if (isControllerActive) {
            return true;
        }

        tooltip.add(CommonComponents.EMPTY);
        GasStorageHandler outputHandler = chamber.getTankBehaviourInternal().getPrimaryHandler();
        GasUnitsTooltips.addContainer(tooltip, outputHandler);
        return true;
    }

    boolean addToTooltip(List<Component> tooltip) {
        if (chamber.getLevel() == null) {
            return false;
        }

        BreezeChamberGasProcessor gasProcessor = chamber.getGasProcessorInternal();
        if (gasProcessor.isControllerActive()) {
            return false;
        }

        boolean hasInvalidInput = gasProcessor.isInputInvalid();
        boolean hasOutputFailure = gasProcessor.isOutputBlocked();
        if (!hasInvalidInput && !hasOutputFailure) {
            return false;
        }

        CCBLang.translate("gui.warning").style(ChatFormatting.GOLD).forGoggles(tooltip);
        if (hasInvalidInput) {
            Gas tankGasType = gasProcessor.getTankGasType();
            CCBLang.addToGoggles(tooltip, "gui.breeze_chamber.invalid_gas", Component.translatable(tankGasType.getTranslationKey()));
        }
        if (hasOutputFailure) {
            CCBLang.addToGoggles(tooltip, "gui.breeze_chamber.output_failed");
        }
        return true;
    }
}
