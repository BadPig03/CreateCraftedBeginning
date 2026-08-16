package net.ty.createcraftedbeginning.content.airtights.residueoutlet;

import net.createmod.catnip.lang.LangBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;
import net.ty.createcraftedbeginning.foundation.lang.CCBLang;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class ResidueOutletTooltip {
    private final ResidueOutletBlockEntity outlet;
    private final ResidueOutletInventory inventory;

    public ResidueOutletTooltip(ResidueOutletBlockEntity outlet, ResidueOutletInventory inventory) {
        this.outlet = outlet;
        this.inventory = inventory;
    }

    private static void addItemTooltip(List<Component> tooltip, ItemStack item) {
        CCBLang.text("").add(Component.translatable(item.getDescriptionId()).withStyle(ChatFormatting.GRAY)).add(CCBLang.text(" x" + item.getCount()).style(ChatFormatting.GREEN)).forGoggles(tooltip, 1);
    }

    private static void addFluidTooltip(List<Component> tooltip, FluidStack fluid) {
        LangBuilder unit = CCBLang.translate("gui.unit.milli_buckets");
        CCBLang.fluidName(fluid).add(CCBLang.text(" ")).style(ChatFormatting.GRAY).add(CCBLang.number(fluid.getAmount()).add(unit).style(ChatFormatting.BLUE)).forGoggles(tooltip, 1);
    }

    public boolean addToGoggleTooltip(List<Component> tooltip) {
        ItemStack item = inventory.getStackInSlot(0);
        FluidStack fluid = outlet.getStoredFluid();
        if (item.isEmpty() && fluid.isEmpty()) {
            return false;
        }

        CCBLang.translate("gui.residue_outlet.header").forGoggles(tooltip);
        if (!item.isEmpty()) {
            addItemTooltip(tooltip, item);
        }
        if (!fluid.isEmpty()) {
            addFluidTooltip(tooltip, fluid);
        }
        return true;
    }
}
