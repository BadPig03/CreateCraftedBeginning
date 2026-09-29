package net.ty.createcraftedbeginning.content.airtights.airtightforgingpress;

import com.simibubi.create.content.kinetics.base.IRotate.SpeedLevel;
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
import net.ty.createcraftedbeginning.api.gas.GasStack;
import net.ty.createcraftedbeginning.api.gas.handler.GasStorageHandler;
import net.ty.createcraftedbeginning.foundation.lang.CCBLang;
import net.ty.createcraftedbeginning.gas.visual.GasUnitFormat;
import net.ty.createcraftedbeginning.gas.visual.GasUnitsTooltips;
import net.ty.createcraftedbeginning.gas.visual.OverpressureTooltips;
import net.ty.createcraftedbeginning.platform.client.ClientContextBridge;
import net.ty.createcraftedbeginning.platform.client.GoggleTooltip;
import net.ty.createcraftedbeginning.platform.client.GoggleTooltip.Section;
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

    void addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        addStoredInfo(tooltip);
        OverpressureTooltips.addStatus(tooltip, press.getOverpressureBehaviour(), press.getGasCapability());
        addKineticInfo(tooltip, isPlayerSneaking);
    }

    boolean addToTooltip(List<Component> tooltip) {
        AirtightForgingPressStructureManager structureManager = core.getStructureManager();
        if (structureManager.getOverstressed() && ClientContextBridge.isOverstressedTooltipEnabled()) {
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

    private void addKineticInfo(List<Component> tooltip, boolean isPlayerSneaking) {
        AirtightForgingPressStructuralShaftBlockEntity shaft = core.getStructureManager().getKineticTooltipSource();
        if (shaft == null) {
            return;
        }

        List<Component> kineticTooltip = new ArrayList<>();
        if (!shaft.addToGoggleTooltip(kineticTooltip, isPlayerSneaking)) {
            return;
        }

        tooltip.add(CommonComponents.EMPTY);
        tooltip.addAll(kineticTooltip);
    }

    private int addItemStorage(List<Component> tooltip, int maxDisplayedStacks) {
        if (!GoggleTooltip.isVisible(tooltip, Section.ITEM_STORAGE)) {
            return 0;
        }

        int stackCount = 0;
        IItemHandler itemHandler = press.getInputOutputCapability();
        for (int slot = 0; slot < itemHandler.getSlots(); slot++) {
            ItemStack storedStack = itemHandler.getStackInSlot(slot);
            if (storedStack.isEmpty()) {
                continue;
            }

            if (stackCount == 0) {
                CCBLang.translate("gui.airtight_forging_press.items").style(ChatFormatting.GRAY).forGoggles(tooltip, 1);
            }
            if (stackCount < maxDisplayedStacks) {
                CCBLang.itemName(storedStack).style(ChatFormatting.GRAY).add(CCBLang.text(" x" + storedStack.getCount()).style(ChatFormatting.GREEN)).forGoggles(tooltip, 2);
            }
            stackCount++;
        }
        return stackCount;
    }

    private int addFluidStorage(List<Component> tooltip) {
        if (!GoggleTooltip.isVisible(tooltip, Section.FLUID_STORAGE)) {
            return 0;
        }

        int fluidCount = 0;
        IFluidHandler fluidHandler = press.getFluidCapability();
        for (int tank = 0; tank < fluidHandler.getTanks(); tank++) {
            FluidStack storedFluid = fluidHandler.getFluidInTank(tank);
            LangBuilder volumeUnit = CCBLang.translate("gui.unit.milli_buckets");
            if (storedFluid.isEmpty()) {
                continue;
            }

            if (fluidCount == 0) {
                CCBLang.translate("gui.airtight_forging_press.fluids").style(ChatFormatting.GRAY).forGoggles(tooltip, 1);
            }
            CCBLang.fluidName(storedFluid).add(CCBLang.text(" ")).style(ChatFormatting.GRAY).add(CCBLang.number(storedFluid.getAmount()).space().add(volumeUnit).style(ChatFormatting.BLUE)).forGoggles(tooltip, 2);
            fluidCount++;
        }
        return fluidCount;
    }

    private int addGasStorage(List<Component> tooltip) {
        if (!GoggleTooltip.isVisible(tooltip, Section.GAS_STORAGE)) {
            return 0;
        }

        int gasCount = 0;
        GasStorageHandler gasHandler = press.getGasCapability();
        for (int tank = 0; tank < gasHandler.getTanks(); tank++) {
            GasStack storedGas = gasHandler.getGasInTank(tank);
            if (storedGas.isEmpty()) {
                continue;
            }

            if (gasCount == 0) {
                CCBLang.translate("gui.airtight_forging_press.gases").style(ChatFormatting.GRAY).forGoggles(tooltip, 1);
            }
            CCBLang.gasName(storedGas).space().style(ChatFormatting.GRAY).add(GasUnitFormat.amount(storedGas.getAmount()).style(ChatFormatting.AQUA)).forGoggles(tooltip, 2);
            GasUnitsTooltips.addPressureReading(tooltip, gasHandler, tank, 2, press);
            gasCount++;
        }
        return gasCount;
    }

    private void addStoredInfo(List<Component> tooltip) {
        CCBLang.translate("gui.airtight_forging_press").forGoggles(tooltip);
        ItemStack pressHeadStack = press.getPressHeadInventory().getStackInSlot(0);
        if (!pressHeadStack.isEmpty() && GoggleTooltip.isVisible(tooltip, Section.PRESS_HEAD)) {
            CCBLang.translate("gui.airtight_forging_press.press_head_tool").style(ChatFormatting.GRAY).forGoggles(tooltip);
            CCBLang.text("").add(Component.translatable(pressHeadStack.getDescriptionId()).withStyle(ChatFormatting.GRAY)).forGoggles(tooltip, 1);
        }

        ItemStack processingStack = press.getAdditionInventory().getStackInSlot(0);
        if (!processingStack.isEmpty() && GoggleTooltip.isVisible(tooltip, Section.PROCESSING_MATERIAL)) {
            CCBLang.translate("gui.airtight_forging_press.processing_material").style(ChatFormatting.GRAY).forGoggles(tooltip);
            CCBLang.text("").add(Component.translatable(processingStack.getDescriptionId()).withStyle(ChatFormatting.GRAY)).add(CCBLang.text(" x" + processingStack.getCount()).style(ChatFormatting.GREEN)).forGoggles(tooltip, 1);
        }

        int contentsStartIndex = tooltip.size();
        CCBLang.translate("gui.airtight_forging_press.contents").style(ChatFormatting.GRAY).forGoggles(tooltip);
        int maxDisplayedStacks = ClientContextBridge.getMaxItemStackDisplay();
        int itemCount = addItemStorage(tooltip, maxDisplayedStacks);
        if (itemCount > maxDisplayedStacks) {
            CCBLang.translate("gui.airtight_forging_press.more", itemCount - maxDisplayedStacks).style(ChatFormatting.DARK_GRAY).forGoggles(tooltip, 2);
        }

        int storedEntryCount = itemCount + addFluidStorage(tooltip) + addGasStorage(tooltip);
        if (storedEntryCount > 0) {
            return;
        }

        while (tooltip.size() > contentsStartIndex) {
            tooltip.removeLast();
        }
    }
}
