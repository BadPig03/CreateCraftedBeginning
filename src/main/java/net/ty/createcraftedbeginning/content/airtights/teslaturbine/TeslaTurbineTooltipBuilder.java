package net.ty.createcraftedbeginning.content.airtights.teslaturbine;

import com.simibubi.create.content.kinetics.base.IRotate.StressImpact;
import net.minecraft.ChatFormatting;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.Mth;
import net.ty.createcraftedbeginning.content.airtights.teslaturbine.TeslaTurbineLevelCalculator.LevelKey;
import net.ty.createcraftedbeginning.foundation.lang.CCBLang;
import net.ty.createcraftedbeginning.platform.client.ClientRenderBridge;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;
import java.util.Map;

import static net.ty.createcraftedbeginning.content.airtights.teslaturbine.TeslaTurbineBlock.MAX_LEVEL;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
class TeslaTurbineTooltipBuilder {
    private final TeslaTurbineCore core;

    TeslaTurbineTooltipBuilder(TeslaTurbineCore core) {
        this.core = core;
    }

    void addToGoggleTooltip(List<Component> tooltip) {
        TeslaTurbineLevelCalculator levelCalculator = core.getLevelCalculator();
        addStatusLine(levelCalculator.getCurrentLevel(), tooltip);
        addProgressBars(levelCalculator.getLevels(), tooltip);
        addDetailedInfo(tooltip);
        addKineticInfo(tooltip);
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

    private static void addProgressBars(Map<LevelKey, Float> levels, List<Component> tooltip) {
        float minimumLevel = levels.getOrDefault(LevelKey.MIN_VALUE, 0.0F);
        float maximumLevel = levels.getOrDefault(LevelKey.MAX_VALUE, (float) MAX_LEVEL);
        List<MutableComponent> labels = List.of(createLabel("supply"), createLabel("rotor"), createLabel("type"));
        List<MutableComponent> bars = List.of(createProgressBar(levels.getOrDefault(LevelKey.SUPPLY, 0.0F), minimumLevel, maximumLevel), createProgressBar(levels.getOrDefault(LevelKey.ROTOR, 0.0F), minimumLevel, maximumLevel), createProgressBar(levels.getOrDefault(LevelKey.TYPE, 0.0F), minimumLevel, maximumLevel));
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

    private static MutableComponent createProgressBar(float level, float minimumLevel, float maximumLevel) {
        int completedSegments = Mth.floor(level);
        int occupiedSegments = Mth.ceil(level);
        int minimumSegment = Mth.ceil(minimumLevel);
        int maximumSegments = Mth.ceil(maximumLevel);
        int totalSegments = Math.min(MAX_LEVEL, (Mth.floor(maximumLevel) / 4 + 1) * 4);
        MutableComponent bar = Component.empty();
        for (int segment = 1; segment <= totalSegments; segment++) {
            ChatFormatting color;
            if (segment > maximumSegments) {
                color = ChatFormatting.DARK_GRAY;
            }
            else if (segment > occupiedSegments) {
                color = ChatFormatting.DARK_RED;
            }
            else if (segment > completedSegments) {
                color = ChatFormatting.YELLOW;
            }
            else if (segment == minimumSegment) {
                color = ChatFormatting.GREEN;
            }
            else {
                color = ChatFormatting.DARK_GREEN;
            }
            bar.append(Component.literal("|").withStyle(color));
        }

        return bar;
    }

    private void addDetailedInfo(List<Component> tooltip) {
        tooltip.add(CommonComponents.EMPTY);
        CCBLang.translate("gui.tesla_turbine.gas_type").style(ChatFormatting.GRAY).forGoggles(tooltip);
        CCBLang.gasName(core.getFlowMeter().getGasType()).style(ChatFormatting.GOLD).forGoggles(tooltip, 1);

        tooltip.add(CommonComponents.EMPTY);
        int nozzleCount = core.getStructureManager().getAttachedNozzle();
        if (nozzleCount == 0) {
            CCBLang.translate("gui.tesla_turbine.via_no_nozzle").style(ChatFormatting.GRAY).forGoggles(tooltip);
            return;
        }

        if (nozzleCount == 1) {
            CCBLang.translate("gui.tesla_turbine.via_one_nozzle").style(ChatFormatting.GRAY).forGoggles(tooltip);
            return;
        }

        CCBLang.translate("gui.tesla_turbine.via_nozzles", nozzleCount).style(ChatFormatting.GRAY).forGoggles(tooltip);
    }

    private void addKineticInfo(List<Component> tooltip) {
        if (!StressImpact.isEnabled()) {
            return;
        }

        tooltip.add(CommonComponents.EMPTY);
        CCBLang.translate("gui.capacity_provided").style(ChatFormatting.GRAY).forGoggles(tooltip);
        float stressCapacity = core.getTurbine().calculateAddedStressCapacity() * Mth.abs(core.getLevelCalculator().getSpeed());
        CCBLang.number(stressCapacity).translate("gui.unit.stress").style(ChatFormatting.AQUA).space().add(CCBLang.translate("gui.at_current_speed").style(ChatFormatting.DARK_GRAY).component()).forGoggles(tooltip, 1);
    }
}
