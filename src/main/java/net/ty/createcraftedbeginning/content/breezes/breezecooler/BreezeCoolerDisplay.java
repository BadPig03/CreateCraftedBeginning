package net.ty.createcraftedbeginning.content.breezes.breezecooler;

import net.createmod.catnip.lang.LangBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.ty.createcraftedbeginning.content.breezes.breezecooler.BreezeCoolerBlock.FrostLevel;
import net.ty.createcraftedbeginning.foundation.lang.CCBLang;
import net.ty.createcraftedbeginning.platform.client.GoggleTooltip;
import net.ty.createcraftedbeginning.platform.client.GoggleTooltip.Section;
import net.ty.createcraftedbeginning.recipe.CoolingRecipeLookup.CoolingData;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
final class BreezeCoolerDisplay {
    private final BreezeCoolerBlockEntity cooler;

    BreezeCoolerDisplay(BreezeCoolerBlockEntity cooler) {
        this.cooler = cooler;
    }

    boolean addToGoggleTooltip(List<Component> tooltip) {
        Level level = cooler.getLevel();
        if (level == null || cooler.isStockKeeper()) {
            return false;
        }

        FrostLevel frostLevel = cooler.getFrostLevel();
        CCBLang.translate("gui.breeze_cooler").forGoggles(tooltip);
        CCBLang.translate("gui.breeze_cooler.current_state").style(ChatFormatting.GRAY).forGoggles(tooltip);
        CCBLang.translate(frostLevel.getTranslatable()).style(frostLevel.getChatFormatting()).forGoggles(tooltip, 1);
        int remainingCoolingTime = cooler.getCoolRemainingTime();
        if (remainingCoolingTime > 0 && GoggleTooltip.isVisible(tooltip, Section.BREEZE_TIME)) {
            CCBLang.translate("gui.breeze_cooler.remaining_time").style(ChatFormatting.GRAY).forGoggles(tooltip);
            if (cooler.isCreative()) {
                CCBLang.translate("gui.fluid_container.infinity").style(ChatFormatting.GREEN).forGoggles(tooltip, 1);
            }
            else {
                CCBLang.seconds(remainingCoolingTime, level.tickRateManager().tickrate()).style(ChatFormatting.GREEN).forGoggles(tooltip, 1);
            }
        }

        if (!GoggleTooltip.isVisible(tooltip, Section.FLUID_STORAGE)) {
            return true;
        }

        IFluidHandler fluidTank = cooler.getTankInventory();
        FluidStack storedFluid = fluidTank.getFluidInTank(0);
        tooltip.add(CommonComponents.EMPTY);
        LangBuilder millibuckets = CCBLang.translate("gui.unit.milli_buckets");
        CCBLang.translate("gui.fluid_container.capacity").style(ChatFormatting.GRAY).forGoggles(tooltip);
        if (storedFluid.isEmpty()) {
            CCBLang.number(fluidTank.getTankCapacity(0)).add(millibuckets).style(ChatFormatting.GOLD).forGoggles(tooltip, 1);
        }
        else {
            CCBLang.fluidName(storedFluid).style(ChatFormatting.WHITE).forGoggles(tooltip, 1);
            CCBLang.number(storedFluid.getAmount()).add(millibuckets).style(ChatFormatting.GOLD).text(ChatFormatting.GRAY, " / ").add(CCBLang.number(fluidTank.getTankCapacity(0)).add(millibuckets).style(ChatFormatting.DARK_GRAY)).forGoggles(tooltip, 1);
        }
        return true;
    }

    boolean addToTooltip(List<Component> tooltip) {
        if (cooler.getLevel() == null || cooler.isStockKeeper() || !isLiquidInvalid()) {
            return false;
        }

        CCBLang.translate("gui.warning").style(ChatFormatting.GOLD).forGoggles(tooltip);
        CCBLang.addToGoggles(tooltip, "gui.breeze_cooler.invalid_fluid");
        return true;
    }

    private boolean isLiquidInvalid() {
        FluidStack fluid = cooler.getTankInventory().getFluid();
        if (fluid.isEmpty() || cooler.getLevel() == null) {
            return false;
        }

        CoolingData coolingData = cooler.getFluidCoolingData(fluid);
        return coolingData.time() <= 0 || coolingData.amount() <= 0;
    }
}
