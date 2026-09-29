package net.ty.createcraftedbeginning.content.airtights.airtightreactorkettle;

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
import net.ty.createcraftedbeginning.recipe.temperature.TemperatureCondition;
import net.ty.createcraftedbeginning.registry.CCBBlocks;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
class AirtightReactorKettleTooltipBuilder {
    private final AirtightReactorKettleCore core;
    private final AirtightReactorKettleBlockEntity kettle;

    AirtightReactorKettleTooltipBuilder(AirtightReactorKettleCore core, AirtightReactorKettleBlockEntity kettle) {
        this.core = core;
        this.kettle = kettle;
    }

    void addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        CCBLang.translate("gui.airtight_reactor_kettle").forGoggles(tooltip);
        boolean hasStoredInfo = addStoredInfo(tooltip);
        boolean hasOverpressureInfo = OverpressureTooltips.addStatus(tooltip, kettle.getOverpressureBehaviour(), kettle.getAvailableGases());
        if (hasStoredInfo || hasOverpressureInfo) {
            tooltip.add(CommonComponents.EMPTY);
        }

        addTemperatureInfo(tooltip);
        addKineticInfo(tooltip, isPlayerSneaking);
    }

    boolean addToTooltip(List<Component> tooltip) {
        AirtightReactorKettleStructureManager structureManager = core.getStructureManager();
        if (structureManager.getOverstressed() && ClientContextBridge.isOverstressedTooltipEnabled()) {
            CCBLang.translate("gui.overstressed").style(ChatFormatting.GOLD).forGoggles(tooltip);
            CCBLang.addToGoggles(tooltip, "gui.network_overstressed");
            return true;
        }

        float currentSpeed = structureManager.getSpeed();
        if (currentSpeed == 0 || Mth.abs(currentSpeed) >= SpeedLevel.FAST.getSpeedValue()) {
            return false;
        }

        CCBLang.translate("gui.speed_requirement").style(ChatFormatting.GOLD).forGoggles(tooltip);
        String structuralBlockName = Component.translatable(CCBBlocks.AIRTIGHT_REACTOR_KETTLE_STRUCTURAL_BLOCK.getDefaultState().getBlock().getDescriptionId()).getString();
        CCBLang.addToGoggles(tooltip, "gui.not_fast_enough", structuralBlockName);
        return true;
    }

    private void addTemperatureInfo(List<Component> tooltip) {
        AirtightReactorKettleStructureManager structureManager = core.getStructureManager();
        TemperatureCondition condition = TemperatureCondition.getConditionByTemperature(structureManager.getTemperature());
        CCBLang.translate("gui.airtight_reactor_kettle.temperature_state").style(ChatFormatting.GRAY).forGoggles(tooltip);
        CCBLang.translate(condition.getTranslationKey()).color(condition.getColor()).forGoggles(tooltip, 1);
    }

    private void addKineticInfo(List<Component> tooltip, boolean isPlayerSneaking) {
        AirtightReactorKettleStructuralCogBlockEntity cog = core.getStructureManager().getKineticTooltipSource();
        if (cog == null) {
            return;
        }

        List<Component> kineticTooltip = new ArrayList<>();
        if (!cog.addToGoggleTooltip(kineticTooltip, isPlayerSneaking)) {
            return;
        }

        tooltip.add(CommonComponents.EMPTY);
        tooltip.addAll(kineticTooltip);
    }

    private boolean addStoredInfo(List<Component> tooltip) {
        int contentsStartIndex = tooltip.size();
        CCBLang.translate("gui.airtight_reactor_kettle.contents").style(ChatFormatting.GRAY).forGoggles(tooltip);

        int maxItemDisplay = ClientContextBridge.getMaxItemStackDisplay();
        int itemCount = addItemInfo(tooltip, maxItemDisplay);
        if (itemCount > maxItemDisplay) {
            CCBLang.translate("gui.airtight_reactor_kettle.more", itemCount - maxItemDisplay).style(ChatFormatting.DARK_GRAY).forGoggles(tooltip, 2);
        }

        int storedEntryCount = itemCount + addFluidInfo(tooltip) + addGasInfo(tooltip);
        if (storedEntryCount > 0) {
            return true;
        }

        while (tooltip.size() > contentsStartIndex) {
            tooltip.removeLast();
        }
        return false;
    }

    private int addItemInfo(List<Component> tooltip, int maxItemDisplay) {
        if (!GoggleTooltip.isVisible(tooltip, Section.ITEM_STORAGE)) {
            return 0;
        }

        int itemCount = 0;
        IItemHandler items = kettle.getAvailableItems();
        for (int slot = 0; slot < items.getSlots(); slot++) {
            ItemStack itemStack = items.getStackInSlot(slot);
            if (itemStack.isEmpty()) {
                continue;
            }

            if (itemCount == 0) {
                CCBLang.translate("gui.airtight_reactor_kettle.items").style(ChatFormatting.GRAY).forGoggles(tooltip, 1);
            }
            if (itemCount < maxItemDisplay) {
                CCBLang.itemName(itemStack).style(ChatFormatting.GRAY).add(CCBLang.text(" x" + itemStack.getCount()).style(ChatFormatting.GREEN)).forGoggles(tooltip, 2);
            }
            itemCount++;
        }
        return itemCount;
    }

    private int addFluidInfo(List<Component> tooltip) {
        if (!GoggleTooltip.isVisible(tooltip, Section.FLUID_STORAGE)) {
            return 0;
        }

        int fluidCount = 0;
        IFluidHandler fluids = kettle.getAvailableFluids();
        for (int tank = 0; tank < fluids.getTanks(); tank++) {
            FluidStack fluidStack = fluids.getFluidInTank(tank);
            LangBuilder unit = CCBLang.translate("gui.unit.milli_buckets");
            if (fluidStack.isEmpty()) {
                continue;
            }

            if (fluidCount == 0) {
                CCBLang.translate("gui.airtight_reactor_kettle.fluids").style(ChatFormatting.GRAY).forGoggles(tooltip, 1);
            }
            CCBLang.fluidName(fluidStack).add(CCBLang.text(" ")).style(ChatFormatting.GRAY).add(CCBLang.number(fluidStack.getAmount()).space().add(unit).style(ChatFormatting.BLUE)).forGoggles(tooltip, 2);
            fluidCount++;
        }
        return fluidCount;
    }

    private int addGasInfo(List<Component> tooltip) {
        if (!GoggleTooltip.isVisible(tooltip, Section.GAS_STORAGE)) {
            return 0;
        }

        int gasCount = 0;
        GasStorageHandler gases = kettle.getAvailableGases();
        for (int tank = 0; tank < gases.getTanks(); tank++) {
            GasStack gasStack = gases.getGasInTank(tank);
            if (gasStack.isEmpty()) {
                continue;
            }

            if (gasCount == 0) {
                CCBLang.translate("gui.airtight_reactor_kettle.gases").style(ChatFormatting.GRAY).forGoggles(tooltip, 1);
            }
            CCBLang.gasName(gasStack).add(CCBLang.text(" ")).style(ChatFormatting.GRAY).add(GasUnitFormat.amount(gasStack.getAmount()).style(ChatFormatting.AQUA)).forGoggles(tooltip, 2);
            GasUnitsTooltips.addPressureReading(tooltip, gases, tank, 2, kettle);
            gasCount++;
        }
        return gasCount;
    }

}
