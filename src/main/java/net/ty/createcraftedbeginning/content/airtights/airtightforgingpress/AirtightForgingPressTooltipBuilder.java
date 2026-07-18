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
import net.neoforged.neoforge.items.IItemHandler;
import net.ty.createcraftedbeginning.api.gas.gases.GasAmountUtils;
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
        String blockName = I18n.get(CCBBlocks.AIRTIGHT_FORGING_PRESS_STRUCTURAL_BLOCK.getDefaultState().getBlock().getDescriptionId());
        CCBLang.addToGoggles(tooltip, "gui.not_fast_enough", blockName);
        return true;
    }

    private void addKineticInfo(List<Component> tooltip) {
        if (!StressImpact.isEnabled()) {
            return;
        }

        tooltip.add(CommonComponents.EMPTY);
        CCBLang.translate("gui.stress_impact").style(ChatFormatting.GRAY).forGoggles(tooltip);
        double stress = Mth.abs(core.getStructureManager().getTheoreticalSpeed()) * BlockStressValues.getImpact(CCBBlocks.AIRTIGHT_FORGING_PRESS_STRUCTURAL_SHAFT_BLOCK.get());
        CCBLang.number(stress).translate("gui.unit.stress").style(ChatFormatting.AQUA).space().add(CCBLang.translate("gui.at_current_speed").style(ChatFormatting.DARK_GRAY)).forGoggles(tooltip, 1);
    }

    private List<Component> calculateStorage() {
        List<Component> tooltip = new ArrayList<>();
        addItemStorage(tooltip);
        addFluidStorage(tooltip);
        addGasStorage(tooltip);
        return tooltip;
    }

    private void addItemStorage(List<Component> tooltip) {
        int maxDisplay = CCBConfig.client().maxItemStackDisplay.get();
        int stackCount = 0;
        IItemHandler items = press.getInputOutputCapability();
        for (int slot = 0; slot < items.getSlots(); slot++) {
            ItemStack stack = items.getStackInSlot(slot);
            if (stack.isEmpty()) {
                continue;
            }

            if (stackCount < maxDisplay) {
                CCBLang.text("").add(Component.translatable(stack.getDescriptionId()).withStyle(ChatFormatting.GRAY)).add(CCBLang.text(" x" + stack.getCount()).style(ChatFormatting.GREEN)).forGoggles(tooltip, 1);
            }
            stackCount++;
        }

        if (stackCount > maxDisplay) {
            CCBLang.translate("gui.airtight_forging_press.more", stackCount - maxDisplay).style(ChatFormatting.DARK_GRAY).forGoggles(tooltip, 1);
        }
    }

    private void addFluidStorage(List<Component> tooltip) {
        IFluidHandler fluids = press.getFluidCapability();
        for (int tank = 0; tank < fluids.getTanks(); tank++) {
            FluidStack fluid = fluids.getFluidInTank(tank);
            LangBuilder unit = CCBLang.translate("gui.unit.milli_buckets");
            if (fluid.isEmpty()) {
                continue;
            }

            CCBLang.fluidName(fluid).add(CCBLang.text(" ")).style(ChatFormatting.GRAY).add(CCBLang.number(fluid.getAmount()).add(unit).style(ChatFormatting.BLUE)).forGoggles(tooltip, 1);
        }
    }

    private void addGasStorage(List<Component> tooltip) {
        IGasHandler gases = press.getGasCapability();
        for (int tank = 0; tank < gases.getTanks(); tank++) {
            GasStack gas = gases.getGasInTank(tank);
            if (gas.isEmpty()) {
                continue;
            }

            CCBLang.gasName(gas).space().style(ChatFormatting.GRAY).add(GasAmountUtils.precise(gas.getAmount()).style(ChatFormatting.AQUA)).forGoggles(tooltip, 1);
        }
    }

    private void addStoredInfo(List<Component> tooltip) {
        CCBLang.translate("gui.airtight_forging_press").forGoggles(tooltip);
        ItemStack pressHead = press.getPressHeadInventory().getStackInSlot(0);
        if (!pressHead.isEmpty()) {
            CCBLang.translate("gui.airtight_forging_press.press_head_tool").style(ChatFormatting.GRAY).forGoggles(tooltip);
            CCBLang.text("").add(Component.translatable(pressHead.getDescriptionId()).withStyle(ChatFormatting.GRAY)).forGoggles(tooltip, 1);
        }

        ItemStack processing = press.getAdditionInventory().getStackInSlot(0);
        if (!processing.isEmpty()) {
            CCBLang.translate("gui.airtight_forging_press.processing_material").style(ChatFormatting.GRAY).forGoggles(tooltip);
            CCBLang.text("").add(Component.translatable(processing.getDescriptionId()).withStyle(ChatFormatting.GRAY)).add(CCBLang.text(" x" + processing.getCount()).style(ChatFormatting.GREEN)).forGoggles(tooltip, 1);
        }

        List<Component> storage = calculateStorage();
        if (storage.isEmpty()) {
            return;
        }

        CCBLang.translate("gui.airtight_forging_press.contents").style(ChatFormatting.GRAY).forGoggles(tooltip);
        tooltip.addAll(storage);
    }
}
