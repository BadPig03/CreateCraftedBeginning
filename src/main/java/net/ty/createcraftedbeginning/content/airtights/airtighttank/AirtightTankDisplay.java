package net.ty.createcraftedbeginning.content.airtights.airtighttank;

import net.minecraft.ChatFormatting;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.ty.createcraftedbeginning.api.gas.gases.GasAmounts;
import net.ty.createcraftedbeginning.api.gas.gases.GasStack;
import net.ty.createcraftedbeginning.api.gas.gases.interfaces.IGasHandler;
import net.ty.createcraftedbeginning.content.airtights.airtightengine.airtightassemblydriver.AirtightAssemblyDriverCore;
import net.ty.createcraftedbeginning.foundation.lang.CCBLang;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class AirtightTankDisplay {
    private final AirtightTankBlockEntity owner;

    public AirtightTankDisplay(AirtightTankBlockEntity owner) {
        this.owner = owner;
    }

    public boolean addToGoggleTooltip(List<Component> tooltip) {
        AirtightTankBlockEntity controller = owner.getControllerBE();
        if (controller == null) {
            return false;
        }

        AirtightAssemblyDriverCore core = controller.getCore();
        if (core.addToGoggleTooltip(tooltip)) {
            tooltip.add(Component.empty());
        }

        IGasHandler handler = controller.getTankInventory();
        CCBLang.translate("gui.gas_container").forGoggles(tooltip);
        GasStack gasStack = handler.getGasInTank(0);
        long capacity = handler.getTankCapacity(0);
        if (gasStack.isEmpty()) {
            CCBLang.translate("gui.gas_container.capacity").add(GasAmounts.precise(capacity).style(ChatFormatting.GOLD)).style(ChatFormatting.GRAY).forGoggles(tooltip, 1);
            return true;
        }

        CCBLang.gasName(gasStack).style(ChatFormatting.GRAY).forGoggles(tooltip, 1);
        GasAmounts.precise(gasStack.getAmount()).style(ChatFormatting.GOLD).text(ChatFormatting.GRAY, " / ").add(GasAmounts.precise(capacity).style(ChatFormatting.DARK_GRAY)).forGoggles(tooltip, 1);
        return true;
    }

    public int getMaxValue() {
        AirtightTankBlockEntity controller = owner.getControllerBE();
        return controller == null ? 0 : GasAmounts.toWholeBucketsClamped(controller.getCapability().getTankCapacity(0));
    }

    public int getCurrentValue() {
        AirtightTankBlockEntity controller = owner.getControllerBE();
        if (controller == null) {
            return 0;
        }

        IGasHandler handler = controller.getCapability();
        long amount = 0;
        for (int i = 0; i < handler.getTanks(); i++) {
            GasStack stack = handler.getGasInTank(i);
            if (!stack.isEmpty()) {
                amount += stack.getAmount();
            }
        }
        return GasAmounts.toWholeBucketsClamped(amount);
    }

    public MutableComponent format(int value) {
        return GasAmounts.formatWholeBuckets(value);
    }
}
