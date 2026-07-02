package net.ty.createcraftedbeginning.content.airtights.airtightforgingpress;

import com.simibubi.create.api.stress.BlockStressValues;
import com.simibubi.create.content.kinetics.base.IRotate.SpeedLevel;
import com.simibubi.create.content.kinetics.base.IRotate.StressImpact;
import com.simibubi.create.infrastructure.config.AllConfigs;
import net.createmod.catnip.lang.LangBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.ty.createcraftedbeginning.api.gas.gases.GasStack;
import net.ty.createcraftedbeginning.api.gas.gases.interfaces.IGasHandler;
import net.ty.createcraftedbeginning.config.CCBConfig;
import net.ty.createcraftedbeginning.data.CCBLang;
import net.ty.createcraftedbeginning.registry.CCBBlocks;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class AirtightForgingPressTooltipBuilder {
    private final AirtightForgingPressCore core;
    private final AirtightForgingPressBlockEntity press;

    public AirtightForgingPressTooltipBuilder(AirtightForgingPressCore core, AirtightForgingPressBlockEntity press) {
        this.core = core;
        this.press = press;
    }

    public void addToGoggleTooltip(List<Component> tooltip) {
        addStoredInfo(tooltip);
        addKineticInfo(tooltip);
    }

    public boolean addToTooltip(List<Component> tooltip) {
        AirtightForgingPressStructureManager structureManager = core.getStructureManager();
        if (structureManager.getOverstressed() && AllConfigs.client().enableOverstressedTooltip.get()) {
            CCBLang.translate("gui.goggles.overstressed").style(ChatFormatting.GOLD).forGoggles(tooltip);
            CCBLang.addToGoggles(tooltip, "gui.goggles.network_overstressed");
            return true;
        }

        boolean added = false;
        float speed = structureManager.getSpeed();
        if (speed != 0 && Mth.abs(speed) < SpeedLevel.FAST.getSpeedValue()) {
            CCBLang.translate("gui.goggles.speed_requirement").style(ChatFormatting.GOLD).forGoggles(tooltip);
            CCBLang.addToGoggles(tooltip, "gui.goggles.not_fast_enough", I18n.get(CCBBlocks.AIRTIGHT_FORGING_PRESS_STRUCTURAL_BLOCK.getDefaultState().getBlock().getDescriptionId()));
            added = true;
        }
        return added;
    }

    private void addKineticInfo(List<Component> tooltip) {
        if (!StressImpact.isEnabled()) {
            return;
        }

        tooltip.add(CommonComponents.EMPTY);
        CCBLang.translate("gui.goggles.stress_impact").style(ChatFormatting.GRAY).forGoggles(tooltip);
        CCBLang.number(Mth.abs(core.getStructureManager().getTheoreticalSpeed()) * BlockStressValues.getImpact(CCBBlocks.AIRTIGHT_FORGING_PRESS_STRUCTURAL_SHAFT_BLOCK.get())).translate("gui.goggles.unit.stress").style(ChatFormatting.AQUA).space().add(CCBLang.translate("gui.goggles.at_current_speed").style(ChatFormatting.DARK_GRAY)).forGoggles(tooltip, 1);
    }

    private List<Component> calculateStorage() {
        List<Component> tooltip = new ArrayList<>();
        int maxDisplay = CCBConfig.client().maxItemStackDisplay.get();
        int listCount = 0;
        IItemHandlerModifiable itemCapability = press.getInputOutputCapability();
        for (int i = 0; i < itemCapability.getSlots(); i++) {
            ItemStack itemStack = itemCapability.getStackInSlot(i);
            if (itemStack.isEmpty()) {
                continue;
            }

            if (listCount < maxDisplay) {
                CCBLang.text("").add(Component.translatable(itemStack.getDescriptionId()).withStyle(ChatFormatting.GRAY)).add(CCBLang.text(" x" + itemStack.getCount()).style(ChatFormatting.GREEN)).forGoggles(tooltip, 1);
            }
            listCount++;
        }
        if (listCount > maxDisplay) {
            CCBLang.translate("gui.goggles.airtight_forging_press.more", listCount - maxDisplay).style(ChatFormatting.DARK_GRAY).forGoggles(tooltip, 1);
        }

        IFluidHandler fluidCapability = press.getFluidCapability();
        for (int i = 0; i < fluidCapability.getTanks(); i++) {
            FluidStack fluidStack = fluidCapability.getFluidInTank(i);
            LangBuilder mb = CCBLang.translate("gui.goggles.unit.milli_buckets");
            if (fluidStack.isEmpty()) {
                continue;
            }

            CCBLang.fluidName(fluidStack).add(CCBLang.text(" ")).style(ChatFormatting.GRAY).add(CCBLang.number(fluidStack.getAmount()).add(mb).style(ChatFormatting.BLUE)).forGoggles(tooltip, 1);
        }

        IGasHandler gasCapability = press.getGasCapability();
        for (int i = 0; i < gasCapability.getTanks(); i++) {
            GasStack gasStack = gasCapability.getGasInTank(i);
            LangBuilder mb = CCBLang.translate("gui.goggles.unit.milli_buckets");
            if (gasStack.isEmpty()) {
                continue;
            }

            CCBLang.gasName(gasStack).add(CCBLang.text(" ")).style(ChatFormatting.GRAY).add(CCBLang.number(gasStack.getAmount()).add(mb).style(ChatFormatting.AQUA)).forGoggles(tooltip, 1);
        }
        return tooltip;
    }

    private void addStoredInfo(List<Component> tooltip) {
        CCBLang.translate("gui.goggles.airtight_forging_press").forGoggles(tooltip);
        ItemStack pressHeadStack = press.getProcessingInventories().getFirst().getStackInSlot(0);
        if (!pressHeadStack.isEmpty()) {
            CCBLang.translate("gui.goggles.airtight_forging_press.press_head_tool").style(ChatFormatting.GRAY).forGoggles(tooltip);
            CCBLang.text("").add(Component.translatable(pressHeadStack.getDescriptionId()).withStyle(ChatFormatting.GRAY)).forGoggles(tooltip, 1);
        }

        ItemStack processingStack = press.getProcessingInventories().getSecond().getStackInSlot(0);
        if (!processingStack.isEmpty()) {
            CCBLang.translate("gui.goggles.airtight_forging_press.processing_material").style(ChatFormatting.GRAY).forGoggles(tooltip);
            CCBLang.text("").add(Component.translatable(processingStack.getDescriptionId()).withStyle(ChatFormatting.GRAY)).add(CCBLang.text(" x" + processingStack.getCount()).style(ChatFormatting.GREEN)).forGoggles(tooltip, 1);
        }

        List<Component> storage = calculateStorage();
        if (storage.isEmpty()) {
            return;
        }

        CCBLang.translate("gui.goggles.airtight_forging_press.contents").style(ChatFormatting.GRAY).forGoggles(tooltip);
        tooltip.addAll(storage);
    }
}
