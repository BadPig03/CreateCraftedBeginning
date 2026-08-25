package net.ty.createcraftedbeginning.content.airtights.airtightengine.airtightassemblydriver;

import com.simibubi.create.api.stress.BlockStressValues;
import com.simibubi.create.content.kinetics.base.IRotate.StressImpact;
import net.minecraft.ChatFormatting;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.ty.createcraftedbeginning.api.gas.gases.GasStack;
import net.ty.createcraftedbeginning.content.airtights.airtightengine.AirtightEngineBlockEntity;
import net.ty.createcraftedbeginning.content.airtights.airtightengine.airtightassemblydriver.AirtightAssemblyDriverLevelCalculator.LevelKey;
import net.ty.createcraftedbeginning.foundation.lang.CCBLang;
import net.ty.createcraftedbeginning.platform.client.ClientRenderBridge;
import net.ty.createcraftedbeginning.registry.CCBBlocks;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;
import java.util.Map;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
class AirtightAssemblyDriverTooltipBuilder {
    private final AirtightAssemblyDriverCore driverCore;

    AirtightAssemblyDriverTooltipBuilder(AirtightAssemblyDriverCore driverCore) {
        this.driverCore = driverCore;
    }

    private static void addStatusLine(int currentLevel, List<Component> tooltip) {
        MutableComponent levelText = createLevelText(currentLevel);
        CCBLang.translate("gui.airtight_assembly_driver.status", levelText.withStyle(ChatFormatting.GREEN)).forGoggles(tooltip);
    }

    private static MutableComponent createLevelText(int currentLevel) {
        if (currentLevel == 0) {
            return CCBLang.translateDirect("gui.airtight_assembly_driver.idle");
        }

        if (currentLevel == AirtightAssemblyDriverCore.MAX_LEVEL) {
            return CCBLang.translateDirect("gui.airtight_assembly_driver.max_level");
        }
        return CCBLang.translateDirect("gui.airtight_assembly_driver.level", String.valueOf(currentLevel));
    }

    private static void addProgressBars(Map<LevelKey, Integer> levels, List<Component> tooltip) {
        int minValue = levels.getOrDefault(LevelKey.MIN_VALUE, 0);
        int maxValue = levels.getOrDefault(LevelKey.MAX_VALUE, AirtightAssemblyDriverCore.MAX_LEVEL);
        List<MutableComponent> labels = List.of(createLabel("supply"), createLabel("wind_charging"), createLabel("residue"));
        List<MutableComponent> bars = List.of(createProgressBar(levels.getOrDefault(LevelKey.SUPPLY, 0), minValue, maxValue), createProgressBar(levels.getOrDefault(LevelKey.WIND_CHARGING, 0), minValue, maxValue), createProgressBar(levels.getOrDefault(LevelKey.RESIDUE, 0), minValue, maxValue));
        if (ClientRenderBridge.addAlignedTooltipBars(tooltip, 1, labels, bars)) {
            return;
        }

        for (int barIndex = 0; barIndex < labels.size(); barIndex++) {
            MutableComponent tooltipLine = labels.get(barIndex).copy().append(CCBLang.translateDirect("gui.airtight_assembly_driver.dots").withStyle(ChatFormatting.DARK_GRAY)).append(bars.get(barIndex));
            CCBLang.builder().add(tooltipLine).forGoggles(tooltip, 1);
        }
    }

    private static MutableComponent createLabel(String labelKey) {
        return CCBLang.translateDirect("gui.airtight_assembly_driver." + labelKey).withStyle(ChatFormatting.GRAY);
    }

    private static MutableComponent createProgressBar(int currentLevel, int minLevel, int maxLevel) {
        int lowerPadding = Math.max(0, minLevel - 1);
        int minimumMarker = minLevel > 0 ? 1 : 0;
        int filledBars = Math.max(0, currentLevel - minLevel);
        int emptyBars = Math.max(0, maxLevel - currentLevel);
        int upperPadding = Math.max(0, Math.min(AirtightAssemblyDriverCore.MAX_LEVEL - maxLevel, (maxLevel / 4 + 1) * 4 - maxLevel));
        return Component.empty().append(createBars(lowerPadding, ChatFormatting.DARK_GREEN)).append(createBars(minimumMarker, ChatFormatting.GREEN)).append(createBars(filledBars, ChatFormatting.DARK_GREEN)).append(createBars(emptyBars, ChatFormatting.DARK_RED)).append(createBars(upperPadding, ChatFormatting.DARK_GRAY));
    }

    private static MutableComponent createBars(int count, ChatFormatting formatting) {
        return Component.literal("|".repeat(count)).withStyle(formatting);
    }

    private static void addGasInfo(AirtightAssemblyDriverFlowMeter flowMeter, List<Component> tooltip) {
        tooltip.add(CommonComponents.EMPTY);
        GasStack displayedGas = flowMeter.hasDisplayableGasSupply() ? flowMeter.getGasType() : GasStack.EMPTY;
        CCBLang.translate("gui.airtight_assembly_driver.gas_type").style(ChatFormatting.GRAY).forGoggles(tooltip);
        CCBLang.gasName(displayedGas).style(ChatFormatting.GOLD).forGoggles(tooltip, 1);
    }

    private static void addOutletInfo(int outletCount, List<Component> tooltip) {
        tooltip.add(CommonComponents.EMPTY);
        if (outletCount == 0) {
            CCBLang.translate("gui.airtight_assembly_driver.via_no_outlet").style(ChatFormatting.GRAY).forGoggles(tooltip);
            return;
        }

        if (outletCount == 1) {
            CCBLang.translate("gui.airtight_assembly_driver.via_one_outlet").style(ChatFormatting.GRAY).forGoggles(tooltip);
            return;
        }

        CCBLang.translate("gui.airtight_assembly_driver.via_outlets", outletCount).style(ChatFormatting.GRAY).forGoggles(tooltip);
    }

    private static void addStressInfo(int engineCount, int currentLevel, List<Component> tooltip) {
        tooltip.add(CommonComponents.EMPTY);
        CCBLang.translate("gui.capacity_provided").style(ChatFormatting.GRAY).forGoggles(tooltip);
        double stressCapacity = AirtightEngineBlockEntity.BASE_ROTATION_SPEED * currentLevel * BlockStressValues.getCapacity(CCBBlocks.AIRTIGHT_ENGINE_BLOCK.get());
        MutableComponent stressText = CCBLang.number(stressCapacity).translate("gui.unit.stress").style(ChatFormatting.AQUA).space().component();
        MutableComponent engineText = engineCount == 1 ? CCBLang.translate("gui.airtight_assembly_driver.via_one_engine").style(ChatFormatting.DARK_GRAY).component() : CCBLang.translate("gui.airtight_assembly_driver.via_engines", engineCount).style(ChatFormatting.DARK_GRAY).component();
        stressText.append(engineText);
        CCBLang.builder().add(stressText).forGoggles(tooltip, 1);
    }

    void addToGoggleTooltip(List<Component> tooltip) {
        AirtightAssemblyDriverLevelCalculator levelCalculator = driverCore.getLevelCalculator();
        addStatusLine(levelCalculator.getCurrentLevel(), tooltip);
        addProgressBars(levelCalculator.getLevels(), tooltip);
        addDetailedInfo(tooltip);
    }

    private void addDetailedInfo(List<Component> tooltip) {
        AirtightAssemblyDriverStructureManager structureManager = driverCore.getStructureManager();

        addGasInfo(driverCore.getFlowMeter(), tooltip);
        addOutletInfo(structureManager.getAttachedOutlets(), tooltip);
        if (!StressImpact.isEnabled()) {
            return;
        }

        addStressInfo(structureManager.getAttachedEngines(), driverCore.getLevelCalculator().getCurrentLevel(), tooltip);
    }
}
