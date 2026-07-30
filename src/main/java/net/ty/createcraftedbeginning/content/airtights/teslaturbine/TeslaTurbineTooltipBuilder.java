package net.ty.createcraftedbeginning.content.airtights.teslaturbine;

import com.simibubi.create.content.kinetics.base.IRotate.StressImpact;
import net.minecraft.ChatFormatting;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.ty.createcraftedbeginning.api.gas.gases.GasStack;
import net.ty.createcraftedbeginning.platform.CCBClientBridge;
import net.ty.createcraftedbeginning.content.airtights.teslaturbine.TeslaTurbineLevelCalculator.LevelKey;
import net.ty.createcraftedbeginning.data.CCBLang;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;
import java.util.Map;

import static net.ty.createcraftedbeginning.content.airtights.teslaturbine.TeslaTurbineUtils.MAX_LEVEL;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class TeslaTurbineTooltipBuilder {
    private final TeslaTurbineCore core;

    public TeslaTurbineTooltipBuilder(TeslaTurbineCore core) {
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
        int minValue = levels.getOrDefault(LevelKey.MIN_VALUE, 0);
        int maxValue = levels.getOrDefault(LevelKey.MAX_VALUE, MAX_LEVEL);
        List<MutableComponent> labels = List.of(createLabel("supply"), createLabel("rotor"), createLabel("type"));
        List<MutableComponent> bars = List.of(createProgressBar(levels.getOrDefault(LevelKey.SUPPLY, 0), minValue, maxValue), createProgressBar(levels.getOrDefault(LevelKey.ROTOR, 0), minValue, maxValue), createProgressBar(levels.getOrDefault(LevelKey.TYPE, 0), minValue, maxValue));
        if (CCBClientBridge.addAlignedTooltipBars(tooltip, 1, labels, bars)) {
            return;
        }

        for (int i = 0; i < labels.size(); i++) {
            MutableComponent line = labels.get(i).copy().append(CCBLang.translateDirect("gui.tesla_turbine.dots").withStyle(ChatFormatting.DARK_GRAY)).append(bars.get(i));
            CCBLang.builder().add(line).forGoggles(tooltip, 1);
        }
    }

    private static MutableComponent createLabel(String label) {
        return CCBLang.translateDirect("gui.tesla_turbine." + label).withStyle(ChatFormatting.GRAY);
    }

    private static MutableComponent createProgressBar(int level, int minValue, int maxValue) {
        int beforeMin = Math.max(0, minValue - 1);
        int atMin = minValue > 0 ? 1 : 0;
        int completed = Math.max(0, level - minValue);
        int remaining = Math.max(0, maxValue - level);
        int padding = Math.max(0, Math.min(MAX_LEVEL - maxValue, (maxValue / 4 + 1) * 4 - maxValue));

        return Component.empty().append(createBars(beforeMin, ChatFormatting.DARK_GREEN)).append(createBars(atMin, ChatFormatting.GREEN)).append(createBars(completed, ChatFormatting.DARK_GREEN)).append(createBars(remaining, ChatFormatting.DARK_RED)).append(createBars(padding, ChatFormatting.DARK_GRAY));
    }

    private static MutableComponent createBars(int count, ChatFormatting formatting) {
        return Component.literal("|".repeat(count)).withStyle(formatting);
    }

    public void addToGoggleTooltip(List<Component> tooltip) {
        TeslaTurbineLevelCalculator levelCalculator = core.getLevelCalculator();
        addStatusLine(levelCalculator.getCurrentLevel(), tooltip);
        addProgressBars(levelCalculator.getLevels(), tooltip);
        addDetailedInfo(tooltip);
        addKineticInfo(tooltip);
    }

    private void addDetailedInfo(List<Component> tooltip) {
        TeslaTurbineStructureManager structureManager = core.getStructureManager();
        TeslaTurbineFlowMeter flowMeter = core.getFlowMeter();
        tooltip.add(CommonComponents.EMPTY);
        GasStack gas = flowMeter.getGasType();
        CCBLang.translate("gui.tesla_turbine.gas_type").style(ChatFormatting.GRAY).forGoggles(tooltip);
        CCBLang.gasName(gas).style(ChatFormatting.GOLD).forGoggles(tooltip, 1);

        tooltip.add(CommonComponents.EMPTY);
        int nozzles = structureManager.getAttachedNozzle();
        if (nozzles == 0) {
            CCBLang.translate("gui.tesla_turbine.via_no_nozzle").style(ChatFormatting.GRAY).forGoggles(tooltip);
        }
        else if (nozzles == 1) {
            CCBLang.translate("gui.tesla_turbine.via_one_nozzle").style(ChatFormatting.GRAY).forGoggles(tooltip);
        }
        else {
            CCBLang.translate("gui.tesla_turbine.via_nozzles", nozzles).style(ChatFormatting.GRAY).forGoggles(tooltip);
        }
    }

    private void addKineticInfo(List<Component> tooltip) {
        if (!StressImpact.isEnabled()) {
            return;
        }

        tooltip.add(CommonComponents.EMPTY);
        TeslaTurbineLevelCalculator levelCalculator = core.getLevelCalculator();
        CCBLang.translate("gui.capacity_provided").style(ChatFormatting.GRAY).forGoggles(tooltip);
        float capacity = core.getTurbine().calculateAddedStressCapacity() * Math.abs(levelCalculator.getSpeed());
        CCBLang.number(capacity).translate("gui.unit.stress").style(ChatFormatting.AQUA).space().add(CCBLang.translate("gui.at_current_speed").style(ChatFormatting.DARK_GRAY).component()).forGoggles(tooltip, 1);
    }
}
