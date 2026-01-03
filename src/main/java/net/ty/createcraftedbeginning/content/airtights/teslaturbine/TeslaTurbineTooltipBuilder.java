package net.ty.createcraftedbeginning.content.airtights.teslaturbine;

import com.simibubi.create.api.stress.BlockStressValues;
import com.simibubi.create.content.kinetics.base.IRotate.StressImpact;
import com.simibubi.create.infrastructure.config.AllConfigs;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.Mth;
import net.ty.createcraftedbeginning.api.gas.gases.GasStack;
import net.ty.createcraftedbeginning.content.airtights.teslaturbine.TeslaTurbineLevelCalculator.LevelKey;
import net.ty.createcraftedbeginning.data.CCBLang;
import net.ty.createcraftedbeginning.registry.CCBBlocks;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;

import static net.ty.createcraftedbeginning.content.airtights.teslaturbine.TeslaTurbineCore.MAX_LEVEL;

public class TeslaTurbineTooltipBuilder {
    private static final int BASE_ROTATION_SPEED = 16;
    private static final float MIN_GAS_SUPPLY_THRESHOLD = 0.005f;

    private final TeslaTurbineCore core;

    public TeslaTurbineTooltipBuilder(TeslaTurbineCore core) {
        this.core = core;
    }

    private static void addStatusLine(int currentLevel, List<Component> tooltip) {
        MutableComponent levelText;
        if (currentLevel == 0) {
            levelText = CCBLang.translateDirect("gui.goggles.tesla_turbine.idle");
        }
        else if (currentLevel == MAX_LEVEL) {
            levelText = CCBLang.translateDirect("gui.goggles.tesla_turbine.max_level");
        }
        else {
            levelText = CCBLang.translateDirect("gui.goggles.tesla_turbine.level", String.valueOf(currentLevel));
        }
        CCBLang.translate("gui.goggles.tesla_turbine.status", levelText.withStyle(ChatFormatting.GREEN)).forGoggles(tooltip);
    }

    private static void addProgressBars(@NotNull Map<LevelKey, Integer> levels, List<Component> tooltip) {
        int minValue = levels.getOrDefault(LevelKey.MIN_VALUE, 0);
        int maxValue = levels.getOrDefault(LevelKey.MAX_VALUE, MAX_LEVEL);
        CCBLang.builder().add(createProgressBar("supply", levels.getOrDefault(LevelKey.SUPPLY, 0), minValue, maxValue)).forGoggles(tooltip, 1);
        CCBLang.builder().add(createProgressBar("rotor", levels.getOrDefault(LevelKey.ROTOR, 0), minValue, maxValue)).forGoggles(tooltip, 1);
        CCBLang.builder().add(createProgressBar("type", levels.getOrDefault(LevelKey.TYPE, 0), minValue, maxValue)).forGoggles(tooltip, 1);
    }

    private static @NotNull MutableComponent createProgressBar(String label, int level, int minValue, int maxValue) {
        MutableComponent barComponent = Component.empty().append(createBars(Math.max(0, minValue - 1), ChatFormatting.DARK_GREEN)).append(createBars(minValue > 0 ? 1 : 0, ChatFormatting.GREEN)).append(createBars(Math.max(0, level - minValue), ChatFormatting.DARK_GREEN)).append(createBars(Math.max(0, maxValue - level), ChatFormatting.DARK_RED)).append(createBars(Math.max(0, Math.min(MAX_LEVEL - maxValue, (maxValue / 4 + 1) * 4 - maxValue)), ChatFormatting.DARK_GRAY));
        return CCBLang.translateDirect("gui.goggles.tesla_turbine." + label).withStyle(ChatFormatting.GRAY).append(CCBLang.translateDirect("gui.goggles.tesla_turbine.dots").withStyle(ChatFormatting.DARK_GRAY)).append(barComponent);
    }

    private static @NotNull MutableComponent createBars(int count, ChatFormatting formatting) {
        return Component.literal("|".repeat(count)).withStyle(formatting);
    }

    public void addToGoggleTooltip(List<Component> tooltip) {
        TeslaTurbineLevelCalculator levelCalculator = core.getLevelCalculator();
        levelCalculator.update();
        addStatusLine(levelCalculator.getCurrentLevel(), tooltip);
        addProgressBars(levelCalculator.getLevels(), tooltip);
        addDetailedInfo(tooltip);
        addKineticInfo(tooltip);
    }

    private void addDetailedInfo(@NotNull List<Component> tooltip) {
        TeslaTurbineStructureManager structureManager = core.getStructureManager();
        TeslaTurbineFlowMeter flowMeter = core.getFlowMeter();
        tooltip.add(CommonComponents.EMPTY);
        GasStack gasType = Mth.abs(flowMeter.getNetFlow()) < MIN_GAS_SUPPLY_THRESHOLD ? GasStack.EMPTY : flowMeter.getGasType();
        CCBLang.translate("gui.goggles.tesla_turbine.gas_type").style(ChatFormatting.GRAY).forGoggles(tooltip);
        CCBLang.gasName(gasType).style(ChatFormatting.GOLD).forGoggles(tooltip, 1);

        tooltip.add(CommonComponents.EMPTY);
        int nozzles = structureManager.getAttachedNozzle();
        if (nozzles == 0) {
            CCBLang.translate("gui.goggles.tesla_turbine.via_no_nozzle").style(ChatFormatting.GRAY).forGoggles(tooltip);
        }
        else if (nozzles == 1) {
            CCBLang.translate("gui.goggles.tesla_turbine.via_one_nozzle").style(ChatFormatting.GRAY).forGoggles(tooltip);
        }
        else {
            CCBLang.translate("gui.goggles.tesla_turbine.via_nozzles", nozzles).style(ChatFormatting.GRAY).forGoggles(tooltip);
        }
    }

    private void addKineticInfo(List<Component> tooltip) {
        if (!StressImpact.isEnabled()) {
            return;
        }

        tooltip.add(CommonComponents.EMPTY);
        TeslaTurbineLevelCalculator levelCalculator = core.getLevelCalculator();
        CCBLang.translate("gui.goggles.capacity_provided").style(ChatFormatting.GRAY).forGoggles(tooltip);
        double capacityProvided = BASE_ROTATION_SPEED * Mth.abs(levelCalculator.getSpeed()) * levelCalculator.getCurrentLevel() * BlockStressValues.getCapacity(CCBBlocks.TESLA_TURBINE_BLOCK.get()) / AllConfigs.server().kinetics.maxRotationSpeed.get();
        CCBLang.number(capacityProvided).translate("gui.goggles.unit.stress").style(ChatFormatting.AQUA).space().add(CCBLang.translate("gui.goggles.at_current_speed").style(ChatFormatting.DARK_GRAY).component()).forGoggles(tooltip, 1);
    }
}
