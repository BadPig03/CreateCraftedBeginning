package net.ty.createcraftedbeginning.content.airtights.airtightreactorkettle;

import com.simibubi.create.api.stress.BlockStressValues;
import com.simibubi.create.content.kinetics.base.IRotate.SpeedLevel;
import com.simibubi.create.content.kinetics.base.IRotate.StressImpact;
import com.simibubi.create.infrastructure.config.AllConfigs;
import net.createmod.catnip.lang.LangBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.ty.createcraftedbeginning.api.gas.gases.GasStack;
import net.ty.createcraftedbeginning.api.gas.gases.IGasHandler;
import net.ty.createcraftedbeginning.api.gas.reactorkettle.TemperatureCondition;
import net.ty.createcraftedbeginning.config.CCBConfig;
import net.ty.createcraftedbeginning.data.CCBLang;
import net.ty.createcraftedbeginning.registry.CCBBlocks;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class AirtightReactorKettleTooltipBuilder {
    private final AirtightReactorKettleCore core;
    private final AirtightReactorKettleBlockEntity kettle;

    public AirtightReactorKettleTooltipBuilder(AirtightReactorKettleCore core, AirtightReactorKettleBlockEntity kettle) {
        this.core = core;
        this.kettle = kettle;
    }

    private static int getMaxItemDisplayCount() {
        return CCBConfig.client().maxItemStackDisplay.get();
    }

    public void addToGoggleTooltip(@NotNull List<Component> tooltip) {
        if (addStoredInfo(tooltip)) {
            tooltip.add(CommonComponents.EMPTY);
        }

        addTemperatureInfo(tooltip);
        addKineticInfo(tooltip);
    }

    public boolean addToTooltip(@NotNull List<Component> tooltip) {
        AirtightReactorKettleStructureManager structureManager = core.getStructureManager();
        if (structureManager.getOverstressed() && AllConfigs.client().enableOverstressedTooltip.get()) {
            CCBLang.translate("gui.goggles.overstressed").style(ChatFormatting.GOLD).forGoggles(tooltip);
            CCBLang.addToGoggles(tooltip, "gui.goggles.network_overstressed");
            return true;
        }

        boolean added = false;
        float speed = structureManager.getSpeed();
        if (speed != 0 && Mth.abs(speed) < SpeedLevel.FAST.getSpeedValue()) {
            CCBLang.translate("gui.goggles.speed_requirement").style(ChatFormatting.GOLD).forGoggles(tooltip);
            CCBLang.addToGoggles(tooltip, "gui.goggles.not_fast_enough", I18n.get(CCBBlocks.AIRTIGHT_REACTOR_KETTLE_STRUCTURAL_BLOCK.getDefaultState().getBlock().getDescriptionId()));
            added = true;
        }
        return added;
    }

    private void addTemperatureInfo(List<Component> tooltip) {
        AirtightReactorKettleStructureManager structureManager = core.getStructureManager();
        TemperatureCondition condition = TemperatureCondition.getConditionByTemperature(structureManager.getTemperature());
        CCBLang.translate("gui.goggles.airtight_reactor_kettle.temperature_state").style(ChatFormatting.GRAY).forGoggles(tooltip);
        CCBLang.translate(condition.getTranslationKey()).color(condition.getColor()).forGoggles(tooltip, 1);
    }

    private void addKineticInfo(List<Component> tooltip) {
        if (!StressImpact.isEnabled()) {
            return;
        }

        tooltip.add(CommonComponents.EMPTY);
        CCBLang.translate("gui.goggles.stress_impact").style(ChatFormatting.GRAY).forGoggles(tooltip);
        CCBLang.number(Mth.abs(core.getStructureManager().getTheoreticalSpeed()) * BlockStressValues.getImpact(CCBBlocks.AIRTIGHT_REACTOR_KETTLE_STRUCTURAL_COG_BLOCK.get())).translate("gui.goggles.unit.stress").style(ChatFormatting.AQUA).space().add(CCBLang.translate("gui.goggles.at_current_speed").style(ChatFormatting.DARK_GRAY)).forGoggles(tooltip, 1);
    }

    private boolean addStoredInfo(List<Component> tooltip) {
        CCBLang.translate("gui.goggles.airtight_reactor_kettle").forGoggles(tooltip);
        CCBLang.translate("gui.goggles.airtight_reactor_kettle.contents").style(ChatFormatting.GRAY).forGoggles(tooltip);
        int maxDisplay = getMaxItemDisplayCount();
        int listCount = 0;
        IItemHandlerModifiable itemCapability = kettle.getItemCapability();
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
            CCBLang.translate("gui.goggles.airtight_reactor_kettle.more", listCount - maxDisplay).style(ChatFormatting.DARK_GRAY).forGoggles(tooltip, 1);
        }

        IFluidHandler fluidCapability = kettle.getFluidCapability();
        for (int i = 0; i < fluidCapability.getTanks(); i++) {
            FluidStack fluidStack = fluidCapability.getFluidInTank(i);
            LangBuilder mb = CCBLang.translate("gui.goggles.unit.milli_buckets");
            if (fluidStack.isEmpty()) {
                continue;
            }

            CCBLang.fluidName(fluidStack).add(CCBLang.text(" ")).style(ChatFormatting.GRAY).add(CCBLang.number(fluidStack.getAmount()).add(mb).style(ChatFormatting.BLUE)).forGoggles(tooltip, 1);
            listCount++;
        }

        IGasHandler gasCapability = kettle.getGasCapability();
        for (int i = 0; i < gasCapability.getTanks(); i++) {
            GasStack gasStack = gasCapability.getGasInTank(i);
            LangBuilder mb = CCBLang.translate("gui.goggles.unit.milli_buckets");
            if (gasStack.isEmpty()) {
                continue;
            }

            CCBLang.gasName(gasStack).add(CCBLang.text(" ")).style(ChatFormatting.GRAY).add(CCBLang.number(gasStack.getAmount()).add(mb).style(ChatFormatting.AQUA)).forGoggles(tooltip, 1);
            listCount++;
        }

        if (listCount == 0) {
            tooltip.remove(1);
            return false;
        }

        return true;
    }
}
