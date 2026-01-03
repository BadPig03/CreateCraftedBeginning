package net.ty.createcraftedbeginning.content.airtights.airtightassemblydriver;

import com.simibubi.create.api.stress.BlockStressValues;
import com.simibubi.create.content.kinetics.base.IRotate.StressImpact;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.ty.createcraftedbeginning.api.gas.gases.GasStack;
import net.ty.createcraftedbeginning.content.airtights.airtightassemblydriver.AirtightAssemblyDriverLevelCalculator.LevelKey;
import net.ty.createcraftedbeginning.data.CCBLang;
import net.ty.createcraftedbeginning.registry.CCBBlocks;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;

import static net.ty.createcraftedbeginning.content.airtights.airtightassemblydriver.AirtightAssemblyDriverCore.MAX_LEVEL;
import static net.ty.createcraftedbeginning.content.airtights.airtightengine.AirtightEngineBlockEntity.BASE_ROTATION_SPEED;

public class AirtightAssemblyDriverTooltipBuilder {
    private static final float MIN_GAS_SUPPLY_THRESHOLD = 0.005f;

    private final AirtightAssemblyDriverCore driverCore;

    public AirtightAssemblyDriverTooltipBuilder(AirtightAssemblyDriverCore driverCore) {
        this.driverCore = driverCore;
    }

    private static void addStatusLine(int currentLevel, List<Component> tooltip) {
        MutableComponent levelText;
        if (currentLevel == 0) {
            levelText = CCBLang.translateDirect("gui.goggles.airtight_assembly_driver.idle");
        }
        else if (currentLevel == MAX_LEVEL) {
            levelText = CCBLang.translateDirect("gui.goggles.airtight_assembly_driver.max_level");
        }
        else {
            levelText = CCBLang.translateDirect("gui.goggles.airtight_assembly_driver.level", String.valueOf(currentLevel));
        }
        CCBLang.translate("gui.goggles.airtight_assembly_driver.status", levelText.withStyle(ChatFormatting.GREEN)).forGoggles(tooltip);
    }

    private static void addProgressBars(@NotNull Map<LevelKey, Integer> levels, List<Component> tooltip) {
        int minValue = levels.getOrDefault(LevelKey.MIN_VALUE, 0);
        int maxValue = levels.getOrDefault(LevelKey.MAX_VALUE, MAX_LEVEL);
        CCBLang.builder().add(createProgressBar("supply", levels.getOrDefault(LevelKey.SUPPLY, 0), minValue, maxValue)).forGoggles(tooltip, 1);
        CCBLang.builder().add(createProgressBar("wind_charging", levels.getOrDefault(LevelKey.WIND_CHARGING, 0), minValue, maxValue)).forGoggles(tooltip, 1);
        CCBLang.builder().add(createProgressBar("residue", levels.getOrDefault(LevelKey.RESIDUE, 0), minValue, maxValue)).forGoggles(tooltip, 1);
    }

    private static @NotNull MutableComponent createProgressBar(String label, int level, int minValue, int maxValue) {
        MutableComponent barComponent = Component.empty().append(createBars(Math.max(0, minValue - 1), ChatFormatting.DARK_GREEN)).append(createBars(minValue > 0 ? 1 : 0, ChatFormatting.GREEN)).append(createBars(Math.max(0, level - minValue), ChatFormatting.DARK_GREEN)).append(createBars(Math.max(0, maxValue - level), ChatFormatting.DARK_RED)).append(createBars(Math.max(0, Math.min(MAX_LEVEL - maxValue, (maxValue / 4 + 1) * 4 - maxValue)), ChatFormatting.DARK_GRAY));
        return CCBLang.translateDirect("gui.goggles.airtight_assembly_driver." + label).withStyle(ChatFormatting.GRAY).append(CCBLang.translateDirect("gui.goggles.airtight_assembly_driver.dots").withStyle(ChatFormatting.DARK_GRAY)).append(barComponent);
    }

    private static @NotNull MutableComponent createBars(int count, ChatFormatting formatting) {
        return Component.literal("|".repeat(count)).withStyle(formatting);
    }

    public void addToGoggleTooltip(List<Component> tooltip) {
        AirtightAssemblyDriverLevelCalculator levelCalculator = driverCore.getLevelCalculator();
        levelCalculator.update();
        addStatusLine(levelCalculator.getCurrentLevel(), tooltip);
        addProgressBars(levelCalculator.getLevels(), tooltip);
        addDetailedInfo(tooltip);
    }

    private void addDetailedInfo(@NotNull List<Component> tooltip) {
        AirtightAssemblyDriverFlowMeter flowMeter = driverCore.getFlowMeter();
        AirtightAssemblyDriverStructureManager structureManager = driverCore.getStructureManager();
        AirtightAssemblyDriverLevelCalculator levelCalculator = driverCore.getLevelCalculator();

        tooltip.add(CommonComponents.EMPTY);
        GasStack gasStack = flowMeter.getGasSupply() < MIN_GAS_SUPPLY_THRESHOLD ? GasStack.EMPTY : flowMeter.getGasType();
        CCBLang.translate("gui.goggles.airtight_assembly_driver.gas_type").style(ChatFormatting.GRAY).forGoggles(tooltip);
        CCBLang.gasName(gasStack).style(ChatFormatting.GOLD).forGoggles(tooltip, 1);

        tooltip.add(CommonComponents.EMPTY);
        int outlets = structureManager.getAttachedOutlets();
        if (outlets == 0) {
            CCBLang.translate("gui.goggles.airtight_assembly_driver.via_no_outlet").style(ChatFormatting.GRAY).forGoggles(tooltip);
        }
        else if (outlets == 1) {
            CCBLang.translate("gui.goggles.airtight_assembly_driver.via_one_outlet").style(ChatFormatting.GRAY).forGoggles(tooltip);
        }
        else {
            CCBLang.translate("gui.goggles.airtight_assembly_driver.via_outlets", outlets).style(ChatFormatting.GRAY).forGoggles(tooltip);
        }
        if (!StressImpact.isEnabled()) {
            return;
        }

        tooltip.add(CommonComponents.EMPTY);
        CCBLang.translate("gui.goggles.capacity_provided").style(ChatFormatting.GRAY).forGoggles(tooltip);
        int engines = structureManager.getAttachedEngines();
        double capacityProvided = BASE_ROTATION_SPEED * levelCalculator.getCurrentLevel() * BlockStressValues.getCapacity(CCBBlocks.AIRTIGHT_ENGINE_BLOCK.get());
        MutableComponent stressText = CCBLang.number(capacityProvided).translate("gui.goggles.unit.stress").style(ChatFormatting.AQUA).space().component();
        stressText.append(engines == 1 ? CCBLang.translate("gui.goggles.airtight_assembly_driver.via_one_engine").style(ChatFormatting.DARK_GRAY).component() : CCBLang.translate("gui.goggles.airtight_assembly_driver.via_engines", engines).style(ChatFormatting.DARK_GRAY).component());
        CCBLang.builder().add(stressText).forGoggles(tooltip, 1);
    }
}