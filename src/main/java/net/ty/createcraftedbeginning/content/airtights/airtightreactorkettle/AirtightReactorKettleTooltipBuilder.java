package net.ty.createcraftedbeginning.content.airtights.airtightreactorkettle;

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
import net.ty.createcraftedbeginning.api.gas.gases.GasAmountUtils;
import net.ty.createcraftedbeginning.api.gas.gases.GasStack;
import net.ty.createcraftedbeginning.api.gas.gases.interfaces.IGasHandler;
import net.ty.createcraftedbeginning.config.CCBConfig;
import net.ty.createcraftedbeginning.data.CCBLang;
import net.ty.createcraftedbeginning.registry.CCBBlocks;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class AirtightReactorKettleTooltipBuilder {
    private final AirtightReactorKettleCore core;
    private final AirtightReactorKettleBlockEntity kettle;

    public AirtightReactorKettleTooltipBuilder(AirtightReactorKettleCore core, AirtightReactorKettleBlockEntity kettle) {
        this.core = core;
        this.kettle = kettle;
    }

    public void addToGoggleTooltip(List<Component> tooltip) {
        if (addStoredInfo(tooltip)) {
            tooltip.add(CommonComponents.EMPTY);
        }

        addTemperatureInfo(tooltip);
        addKineticInfo(tooltip);
    }

    public boolean addToTooltip(List<Component> tooltip) {
        AirtightReactorKettleStructureManager structureManager = core.getStructureManager();
        if (structureManager.getOverstressed() && AllConfigs.client().enableOverstressedTooltip.get()) {
            CCBLang.translate("gui.overstressed").style(ChatFormatting.GOLD).forGoggles(tooltip);
            CCBLang.addToGoggles(tooltip, "gui.network_overstressed");
            return true;
        }

        float speed = structureManager.getSpeed();
        if (speed == 0 || Mth.abs(speed) >= SpeedLevel.FAST.getSpeedValue()) {
            return false;
        }

        CCBLang.translate("gui.speed_requirement").style(ChatFormatting.GOLD).forGoggles(tooltip);
        String blockName = I18n.get(CCBBlocks.AIRTIGHT_REACTOR_KETTLE_STRUCTURAL_BLOCK.getDefaultState().getBlock().getDescriptionId());
        CCBLang.addToGoggles(tooltip, "gui.not_fast_enough", blockName);
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
        float speed = Mth.abs(core.getStructureManager().getTheoreticalSpeed());
        double stressImpact = BlockStressValues.getImpact(CCBBlocks.AIRTIGHT_REACTOR_KETTLE_STRUCTURAL_COG_BLOCK.get());
        CCBLang.number(speed * stressImpact).translate("gui.unit.stress").style(ChatFormatting.AQUA).space().add(CCBLang.translate("gui.at_current_speed").style(ChatFormatting.DARK_GRAY)).forGoggles(tooltip, 1);
    }

    private boolean addStoredInfo(List<Component> tooltip) {
        int startIndex = tooltip.size();
        CCBLang.translate("gui.airtight_reactor_kettle").forGoggles(tooltip);
        CCBLang.translate("gui.airtight_reactor_kettle.contents").style(ChatFormatting.GRAY).forGoggles(tooltip);

        int maxDisplay = CCBConfig.client().maxItemStackDisplay.get();
        int itemCount = addItemInfo(tooltip, maxDisplay);
        if (itemCount > maxDisplay) {
            CCBLang.translate("gui.airtight_reactor_kettle.more", itemCount - maxDisplay).style(ChatFormatting.DARK_GRAY).forGoggles(tooltip, 1);
        }

        int storedCount = itemCount + addFluidInfo(tooltip) + addGasInfo(tooltip);
        if (storedCount > 0) {
            return true;
        }

        while (tooltip.size() > startIndex) {
            tooltip.removeLast();
        }
        return false;
    }

    private int addItemInfo(List<Component> tooltip, int maxDisplay) {
        int itemCount = 0;
        IItemHandlerModifiable items = kettle.getItemCapability();
        for (int slot = 0; slot < items.getSlots(); slot++) {
            ItemStack stack = items.getStackInSlot(slot);
            if (stack.isEmpty()) {
                continue;
            }

            if (itemCount < maxDisplay) {
                CCBLang.text("").add(Component.translatable(stack.getDescriptionId()).withStyle(ChatFormatting.GRAY)).add(CCBLang.text(" x" + stack.getCount()).style(ChatFormatting.GREEN)).forGoggles(tooltip, 1);
            }
            itemCount++;
        }
        return itemCount;
    }

    private int addFluidInfo(List<Component> tooltip) {
        int fluidCount = 0;
        IFluidHandler fluids = kettle.getFluidCapability();
        for (int tank = 0; tank < fluids.getTanks(); tank++) {
            FluidStack stack = fluids.getFluidInTank(tank);
            LangBuilder unit = CCBLang.translate("gui.unit.milli_buckets");
            if (stack.isEmpty()) {
                continue;
            }

            CCBLang.fluidName(stack).add(CCBLang.text(" ")).style(ChatFormatting.GRAY).add(CCBLang.number(stack.getAmount()).add(unit).style(ChatFormatting.BLUE)).forGoggles(tooltip, 1);
            fluidCount++;
        }
        return fluidCount;
    }

    private int addGasInfo(List<Component> tooltip) {
        int gasCount = 0;
        IGasHandler gases = kettle.getGasCapability();
        for (int tank = 0; tank < gases.getTanks(); tank++) {
            GasStack stack = gases.getGasInTank(tank);
            if (stack.isEmpty()) {
                continue;
            }

            CCBLang.gasName(stack).add(CCBLang.text(" ")).style(ChatFormatting.GRAY).add(GasAmountUtils.precise(stack.getAmount()).style(ChatFormatting.AQUA)).forGoggles(tooltip, 1);
            gasCount++;
        }
        return gasCount;
    }

}
