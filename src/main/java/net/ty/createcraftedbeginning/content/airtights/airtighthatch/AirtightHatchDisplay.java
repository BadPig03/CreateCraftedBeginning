package net.ty.createcraftedbeginning.content.airtights.airtighthatch;

import net.minecraft.ChatFormatting;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.ty.createcraftedbeginning.api.gas.gases.GasAmounts;
import net.ty.createcraftedbeginning.api.gas.gases.GasStack;
import net.ty.createcraftedbeginning.foundation.lang.CCBLang;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class AirtightHatchDisplay {
    private final AirtightHatchBlockEntity hatch;

    public AirtightHatchDisplay(AirtightHatchBlockEntity hatch) {
        this.hatch = hatch;
    }

    private static void addCreativeTooltip(List<Component> tooltip, GasStack gas) {
        if (gas.isEmpty()) {
            CCBLang.translate("gui.creative_gas_canister.empty").style(ChatFormatting.GRAY).forGoggles(tooltip, 1);
            return;
        }

        CCBLang.gasName(gas).style(ChatFormatting.GRAY).forGoggles(tooltip, 1);
        CCBLang.translate("gui.gas_container.infinity").style(ChatFormatting.GOLD).forGoggles(tooltip, 1);
    }

    private static void addStandardTooltip(List<Component> tooltip, GasStack gas, long capacity) {
        if (gas.isEmpty()) {
            CCBLang.translate("gui.gas_container.capacity").add(GasAmounts.precise(capacity).style(ChatFormatting.GOLD)).style(ChatFormatting.GRAY).forGoggles(tooltip, 1);
            return;
        }

        CCBLang.gasName(gas).style(ChatFormatting.GRAY).forGoggles(tooltip, 1);
        GasAmounts.precise(gas.getAmount()).style(ChatFormatting.GOLD).text(ChatFormatting.GRAY, " / ").add(GasAmounts.precise(capacity).style(ChatFormatting.DARK_GRAY)).forGoggles(tooltip, 1);
    }

    public boolean addToGoggleTooltip(List<Component> tooltip) {
        if (hatch.isEmpty()) {
            return false;
        }

        CCBLang.translate("gui.gas_container").forGoggles(tooltip);
        GasStack gas = hatch.getHatchGasContent();
        long capacity = hatch.getHatchCapacity();
        if (hatch.isCreative()) {
            addCreativeTooltip(tooltip, gas);
        }
        else {
            addStandardTooltip(tooltip, gas, capacity);
        }
        return true;
    }

    public int getMaxValue() {
        return hatch.isEmpty() ? 0 : GasAmounts.toWholeBucketsClamped(hatch.getHatchCapacity());
    }

    public int getCurrentValue() {
        return hatch.isEmpty() ? 0 : GasAmounts.toWholeBucketsClamped(hatch.getHatchGasContent().getAmount());
    }

    public MutableComponent format(int value) {
        return GasAmounts.formatWholeBuckets(value);
    }
}
