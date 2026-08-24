package net.ty.createcraftedbeginning.content.airtights.airtightreactorkettle;

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
import net.ty.createcraftedbeginning.api.gas.recipes.TemperatureCondition;
import net.ty.createcraftedbeginning.foundation.lang.CCBLang;
import net.ty.createcraftedbeginning.platform.CCBClientBridge;
import net.ty.createcraftedbeginning.registry.CCBBlocks;

import javax.annotation.ParametersAreNonnullByDefault;
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

    void addToGoggleTooltip(List<Component> tooltip) {
        CCBLang.translate("gui.airtight_reactor_kettle").forGoggles(tooltip);
        if (addStoredInfo(tooltip)) {
            tooltip.add(CommonComponents.EMPTY);
        }

        addTemperatureInfo(tooltip);
        addKineticInfo(tooltip);
    }

    boolean addToTooltip(List<Component> tooltip) {
        AirtightReactorKettleStructureManager structureManager = core.getStructureManager();
        if (structureManager.getOverstressed() && CCBClientBridge.isOverstressedTooltipEnabled()) {
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

    private void addKineticInfo(List<Component> tooltip) {
        if (!StressImpact.isEnabled()) {
            return;
        }

        tooltip.add(CommonComponents.EMPTY);
        CCBLang.translate("gui.stress_impact").style(ChatFormatting.GRAY).forGoggles(tooltip);
        float theoreticalSpeed = Mth.abs(core.getStructureManager().getTheoreticalSpeed());
        double stressImpact = BlockStressValues.getImpact(CCBBlocks.AIRTIGHT_REACTOR_KETTLE_STRUCTURAL_COG_BLOCK.get());
        CCBLang.number(theoreticalSpeed * stressImpact).translate("gui.unit.stress").style(ChatFormatting.AQUA).space().add(CCBLang.translate("gui.at_current_speed").style(ChatFormatting.DARK_GRAY)).forGoggles(tooltip, 1);
    }

    private boolean addStoredInfo(List<Component> tooltip) {
        int contentsStartIndex = tooltip.size();
        CCBLang.translate("gui.airtight_reactor_kettle.contents").style(ChatFormatting.GRAY).forGoggles(tooltip);

        int maxItemDisplay = CCBClientBridge.getMaxItemStackDisplay();
        int itemCount = addItemInfo(tooltip, maxItemDisplay);
        if (itemCount > maxItemDisplay) {
            CCBLang.translate("gui.airtight_reactor_kettle.more", itemCount - maxItemDisplay).style(ChatFormatting.DARK_GRAY).forGoggles(tooltip, 1);
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
        int itemCount = 0;
        IItemHandler items = kettle.getAvailableItems();
        for (int slot = 0; slot < items.getSlots(); slot++) {
            ItemStack itemStack = items.getStackInSlot(slot);
            if (itemStack.isEmpty()) {
                continue;
            }

            if (itemCount < maxItemDisplay) {
                CCBLang.text("").add(Component.translatable(itemStack.getDescriptionId()).withStyle(ChatFormatting.GRAY)).add(CCBLang.text(" x" + itemStack.getCount()).style(ChatFormatting.GREEN)).forGoggles(tooltip, 1);
            }
            itemCount++;
        }
        return itemCount;
    }

    private int addFluidInfo(List<Component> tooltip) {
        int fluidCount = 0;
        IFluidHandler fluids = kettle.getAvailableFluids();
        for (int tank = 0; tank < fluids.getTanks(); tank++) {
            FluidStack fluidStack = fluids.getFluidInTank(tank);
            LangBuilder unit = CCBLang.translate("gui.unit.milli_buckets");
            if (fluidStack.isEmpty()) {
                continue;
            }

            CCBLang.fluidName(fluidStack).add(CCBLang.text(" ")).style(ChatFormatting.GRAY).add(CCBLang.number(fluidStack.getAmount()).add(unit).style(ChatFormatting.BLUE)).forGoggles(tooltip, 1);
            fluidCount++;
        }
        return fluidCount;
    }

    private int addGasInfo(List<Component> tooltip) {
        int gasCount = 0;
        IGasHandler gases = kettle.getAvailableGases();
        for (int tank = 0; tank < gases.getTanks(); tank++) {
            GasStack gasStack = gases.getGasInTank(tank);
            if (gasStack.isEmpty()) {
                continue;
            }

            CCBLang.gasName(gasStack).add(CCBLang.text(" ")).style(ChatFormatting.GRAY).add(GasAmounts.precise(gasStack.getAmount()).style(ChatFormatting.AQUA)).forGoggles(tooltip, 1);
            gasCount++;
        }
        return gasCount;
    }

}
