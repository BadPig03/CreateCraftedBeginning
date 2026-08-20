package net.ty.createcraftedbeginning.content.airtights.airtightforgingpress;

import com.simibubi.create.api.stress.BlockStressValues;
import com.simibubi.create.content.kinetics.base.IRotate.SpeedLevel;
import com.simibubi.create.content.kinetics.base.IRotate.StressImpact;
import net.createmod.catnip.lang.LangBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.IItemHandler;
import net.ty.createcraftedbeginning.api.gas.gases.GasAmounts;
import net.ty.createcraftedbeginning.api.gas.gases.GasStack;
import net.ty.createcraftedbeginning.api.gas.gases.interfaces.IGasHandler;
import net.ty.createcraftedbeginning.foundation.lang.CCBLang;
import net.ty.createcraftedbeginning.platform.CCBClientBridge;
import net.ty.createcraftedbeginning.registry.CCBBlocks;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
class AirtightForgingPressTooltipBuilder {
    private final AirtightForgingPressCore core;
    private final AirtightForgingPressBlockEntity press;

    AirtightForgingPressTooltipBuilder(AirtightForgingPressCore core, AirtightForgingPressBlockEntity press) {
        this.core = core;
        this.press = press;
    }

    void addToGoggleTooltip(List<Component> tooltip) {
        addStoredInfo(tooltip);
        addKineticInfo(tooltip);
    }

    boolean addToTooltip(List<Component> tooltip) {
        AirtightForgingPressStructureManager structureManager = core.getStructureManager();
        if (structureManager.getOverstressed() && CCBClientBridge.isOverstressedTooltipEnabled()) {
            CCBLang.translate("gui.overstressed").style(ChatFormatting.GOLD).forGoggles(tooltip);
            CCBLang.addToGoggles(tooltip, "gui.network_overstressed");
            return true;
        }

        float speed = structureManager.getSpeed();
        boolean isTooSlow = speed != 0 && Mth.abs(speed) < SpeedLevel.FAST.getSpeedValue();
        if (!isTooSlow) {
            return false;
        }

        CCBLang.translate("gui.speed_requirement").style(ChatFormatting.GOLD).forGoggles(tooltip);
        String structuralBlockName = Component.translatable(CCBBlocks.AIRTIGHT_FORGING_PRESS_STRUCTURAL_BLOCK.getDefaultState().getBlock().getDescriptionId()).getString();
        CCBLang.addToGoggles(tooltip, "gui.not_fast_enough", structuralBlockName);
        return true;
    }

    private void addKineticInfo(List<Component> tooltip) {
        if (!StressImpact.isEnabled()) {
            return;
        }

        tooltip.add(CommonComponents.EMPTY);
        CCBLang.translate("gui.stress_impact").style(ChatFormatting.GRAY).forGoggles(tooltip);
        double stressImpact = Mth.abs(core.getStructureManager().getTheoreticalSpeed()) * BlockStressValues.getImpact(CCBBlocks.AIRTIGHT_FORGING_PRESS_STRUCTURAL_SHAFT_BLOCK.get());
        CCBLang.number(stressImpact).translate("gui.unit.stress").style(ChatFormatting.AQUA).space().add(CCBLang.translate("gui.at_current_speed").style(ChatFormatting.DARK_GRAY)).forGoggles(tooltip, 1);
    }

    private List<Component> calculateStorage() {
        List<Component> storageTooltip = new ArrayList<>();
        addItemStorage(storageTooltip);
        addFluidStorage(storageTooltip);
        addGasStorage(storageTooltip);
        return storageTooltip;
    }

    private void addItemStorage(List<Component> tooltip) {
        int maxDisplayedStacks = CCBClientBridge.getMaxItemStackDisplay();
        int stackCount = 0;
        IItemHandler itemHandler = press.getInputOutputCapability();
        for (int slot = 0; slot < itemHandler.getSlots(); slot++) {
            ItemStack storedStack = itemHandler.getStackInSlot(slot);
            if (storedStack.isEmpty()) {
                continue;
            }

            if (stackCount < maxDisplayedStacks) {
                CCBLang.text("").add(Component.translatable(storedStack.getDescriptionId()).withStyle(ChatFormatting.GRAY)).add(CCBLang.text(" x" + storedStack.getCount()).style(ChatFormatting.GREEN)).forGoggles(tooltip, 1);
            }
            stackCount++;
        }

        if (stackCount <= maxDisplayedStacks) {
            return;
        }

        CCBLang.translate("gui.airtight_forging_press.more", stackCount - maxDisplayedStacks).style(ChatFormatting.DARK_GRAY).forGoggles(tooltip, 1);
    }

    private void addFluidStorage(List<Component> tooltip) {
        IFluidHandler fluidHandler = press.getFluidCapability();
        for (int tank = 0; tank < fluidHandler.getTanks(); tank++) {
            FluidStack storedFluid = fluidHandler.getFluidInTank(tank);
            LangBuilder volumeUnit = CCBLang.translate("gui.unit.milli_buckets");
            if (storedFluid.isEmpty()) {
                continue;
            }

            CCBLang.fluidName(storedFluid).add(CCBLang.text(" ")).style(ChatFormatting.GRAY).add(CCBLang.number(storedFluid.getAmount()).add(volumeUnit).style(ChatFormatting.BLUE)).forGoggles(tooltip, 1);
        }
    }

    private void addGasStorage(List<Component> tooltip) {
        IGasHandler gasHandler = press.getGasCapability();
        for (int tank = 0; tank < gasHandler.getTanks(); tank++) {
            GasStack storedGas = gasHandler.getGasInTank(tank);
            if (storedGas.isEmpty()) {
                continue;
            }

            CCBLang.gasName(storedGas).space().style(ChatFormatting.GRAY).add(GasAmounts.precise(storedGas.getAmount()).style(ChatFormatting.AQUA)).forGoggles(tooltip, 1);
        }
    }

    private void addStoredInfo(List<Component> tooltip) {
        CCBLang.translate("gui.airtight_forging_press").forGoggles(tooltip);
        ItemStack pressHeadStack = press.getPressHeadInventory().getStackInSlot(0);
        if (!pressHeadStack.isEmpty()) {
            CCBLang.translate("gui.airtight_forging_press.press_head_tool").style(ChatFormatting.GRAY).forGoggles(tooltip);
            CCBLang.text("").add(Component.translatable(pressHeadStack.getDescriptionId()).withStyle(ChatFormatting.GRAY)).forGoggles(tooltip, 1);
        }

        ItemStack processingStack = press.getAdditionInventory().getStackInSlot(0);
        if (!processingStack.isEmpty()) {
            CCBLang.translate("gui.airtight_forging_press.processing_material").style(ChatFormatting.GRAY).forGoggles(tooltip);
            CCBLang.text("").add(Component.translatable(processingStack.getDescriptionId()).withStyle(ChatFormatting.GRAY)).add(CCBLang.text(" x" + processingStack.getCount()).style(ChatFormatting.GREEN)).forGoggles(tooltip, 1);
        }

        List<Component> storageTooltip = calculateStorage();
        if (storageTooltip.isEmpty()) {
            return;
        }

        CCBLang.translate("gui.airtight_forging_press.contents").style(ChatFormatting.GRAY).forGoggles(tooltip);
        tooltip.addAll(storageTooltip);
    }
}
