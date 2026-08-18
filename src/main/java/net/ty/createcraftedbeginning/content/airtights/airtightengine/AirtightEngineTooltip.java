package net.ty.createcraftedbeginning.content.airtights.airtightengine;

import com.simibubi.create.api.stress.BlockStressValues;
import com.simibubi.create.content.kinetics.base.IRotate.StressImpact;
import net.minecraft.ChatFormatting;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.ty.createcraftedbeginning.foundation.lang.CCBLang;
import net.ty.createcraftedbeginning.registry.CCBBlocks;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
final class AirtightEngineTooltip {
    private AirtightEngineTooltip() {
    }

    static void addGoggleInformation(List<Component> tooltip, boolean clockwise, float generatedSpeed) {
        CCBLang.translate("gui.airtight_engine").forGoggles(tooltip);
        CCBLang.translate("gui.airtight_engine.rotation_direction").style(ChatFormatting.GRAY).forGoggles(tooltip);
        String directionKey = clockwise ? "gui.airtight_engine.rotation_direction.clockwise" : "gui.airtight_engine.rotation_direction.counter_clockwise";
        CCBLang.translate(directionKey).style(ChatFormatting.GOLD).forGoggles(tooltip, 1);
        if (!StressImpact.isEnabled()) {
            return;
        }

        tooltip.add(CommonComponents.EMPTY);
        CCBLang.translate("gui.capacity_provided").style(ChatFormatting.GRAY).forGoggles(tooltip);
        double capacity = Mth.abs(generatedSpeed) * BlockStressValues.getCapacity(CCBBlocks.AIRTIGHT_ENGINE_BLOCK.get());
        CCBLang.number(capacity).translate("gui.unit.stress").style(ChatFormatting.AQUA).space().add(CCBLang.translate("gui.at_current_speed").style(ChatFormatting.DARK_GRAY)).forGoggles(tooltip, 1);
    }
}
