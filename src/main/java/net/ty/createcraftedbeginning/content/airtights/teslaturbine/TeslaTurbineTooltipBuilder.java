package net.ty.createcraftedbeginning.content.airtights.teslaturbine;

import com.simibubi.create.content.kinetics.base.IRotate.StressImpact;
import net.minecraft.ChatFormatting;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.ty.createcraftedbeginning.content.airtights.teslaturbine.TeslaTurbineLevelCalculator.LevelKey;
import net.ty.createcraftedbeginning.foundation.lang.CCBLang;
import net.ty.createcraftedbeginning.platform.client.ClientRenderBridge;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;
import java.util.Map;

import static net.ty.createcraftedbeginning.content.airtights.teslaturbine.TeslaTurbineUtils.MAX_LEVEL;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
class TeslaTurbineTooltipBuilder {
    private final TeslaTurbineCore core;

    TeslaTurbineTooltipBuilder(TeslaTurbineCore core) {
        this.core = core;
    }

    private static void addStatusLine(int currentLevel, List<Component> tooltip) {
        MutableComponent levelText;
        if (currentLevel == 0) {
            levelText = CCBLang.translateDirect("gui.tesla_turbine.idle");
        }
        else if (currentLevel == MAX_LEVEL) {
            levelText = CCBLang.translateDirect("gui.tesla_turbine.max_level");
        }
        else {
            levelText = CCBLang.translateDirect("gui.tesla_turbine.level", String.valueOf(currentLevel));
        }
        CCBLang.translate("gui.tesla_turbine.status", levelText.withStyle(ChatFormatting.GREEN)).forGoggles(tooltip);
    }

    private static void addProgressBars(Map<LevelKey, Integer> levels, List<Component> tooltip) {
        int minimumLevel = levels.getOrDefault(LevelKey.MIN_VALUE, 0);
        int maximumLevel = levels.getOrDefault(LevelKey.MAX_VALUE, MAX_LEVEL);
        List<MutableComponent> labels = List.of(createLabel("supply"), createLabel("rotor"), createLabel("type"));
        List<MutableComponent> bars = List.of(createProgressBar(levels.getOrDefault(LevelKey.SUPPLY, 0), minimumLevel, maximumLevel), createProgressBar(levels.getOrDefault(LevelKey.ROTOR, 0), minimumLevel, maximumLevel), createProgressBar(levels.getOrDefault(LevelKey.TYPE, 0), minimumLevel, maximumLevel));
        if (ClientRenderBridge.addAlignedTooltipBars(tooltip, 1, labels, bars)) {
            return;
        }

        for (int barIndex = 0; barIndex < labels.size(); barIndex++) {
            MutableComponent tooltipLine = labels.get(barIndex).copy().append(CCBLang.translateDirect("gui.tesla_turbine.dots").withStyle(ChatFormatting.DARK_GRAY)).append(bars.get(barIndex));
            CCBLang.builder().add(tooltipLine).forGoggles(tooltip, 1);
        }
    }

    private static MutableComponent createLabel(String label) {
        return CCBLang.translateDirect("gui.tesla_turbine." + label).withStyle(ChatFormatting.GRAY);
    }

    private static MutableComponent createProgressBar(int level, int minimumLevel, int maximumLevel) {
        int segmentsBeforeMinimum = Math.max(0, minimumLevel - 1);
        int minimumSegmentCount = minimumLevel > 0 ? 1 : 0;
        int completedSegments = Math.max(0, level - minimumLevel);
        int remainingSegments = Math.max(0, maximumLevel - level);
        int paddingSegments = Math.max(0, Math.min(MAX_LEVEL - maximumLevel, (maximumLevel / 4 + 1) * 4 - maximumLevel));

        return Component.empty().append(createBars(segmentsBeforeMinimum, ChatFormatting.DARK_GREEN)).append(createBars(minimumSegmentCount, ChatFormatting.GREEN)).append(createBars(completedSegments, ChatFormatting.DARK_GREEN)).append(createBars(remainingSegments, ChatFormatting.DARK_RED)).append(createBars(paddingSegments, ChatFormatting.DARK_GRAY));
    }

    private static MutableComponent createBars(int count, ChatFormatting formatting) {
        return Component.literal("|".repeat(count)).withStyle(formatting);
    }

    void addToGoggleTooltip(List<Component> tooltip) {
        TeslaTurbineLevelCalculator levelCalculator = core.getLevelCalculator();
        addStatusLine(levelCalculator.getCurrentLevel(), tooltip);
        addProgressBars(levelCalculator.getLevels(), tooltip);
        addDetailedInfo(tooltip);
        addKineticInfo(tooltip);
    }

    private void addDetailedInfo(List<Component> tooltip) {
        tooltip.add(CommonComponents.EMPTY);
        CCBLang.translate("gui.tesla_turbine.gas_type").style(ChatFormatting.GRAY).forGoggles(tooltip);
        CCBLang.gasName(core.getFlowMeter().getGasType()).style(ChatFormatting.GOLD).forGoggles(tooltip, 1);

        tooltip.add(CommonComponents.EMPTY);
        int nozzleCount = core.getStructureManager().getAttachedNozzle();
        if (nozzleCount == 0) {
            CCBLang.translate("gui.tesla_turbine.via_no_nozzle").style(ChatFormatting.GRAY).forGoggles(tooltip);
        }
        else if (nozzleCount == 1) {
            CCBLang.translate("gui.tesla_turbine.via_one_nozzle").style(ChatFormatting.GRAY).forGoggles(tooltip);
        }
        else {
            CCBLang.translate("gui.tesla_turbine.via_nozzles", nozzleCount).style(ChatFormatting.GRAY).forGoggles(tooltip);
        }
    }

    private void addKineticInfo(List<Component> tooltip) {
        if (!StressImpact.isEnabled()) {
            return;
        }

        tooltip.add(CommonComponents.EMPTY);
        CCBLang.translate("gui.capacity_provided").style(ChatFormatting.GRAY).forGoggles(tooltip);
        float stressCapacity = core.getTurbine().calculateAddedStressCapacity() * Math.abs(core.getLevelCalculator().getSpeed());
        CCBLang.number(stressCapacity).translate("gui.unit.stress").style(ChatFormatting.AQUA).space().add(CCBLang.translate("gui.at_current_speed").style(ChatFormatting.DARK_GRAY).component()).forGoggles(tooltip, 1);
    }
}
