package net.ty.createcraftedbeginning.content.airtights.airtighttank;

import net.minecraft.ChatFormatting;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.ty.createcraftedbeginning.api.gas.gases.GasAmountUtils;
import net.ty.createcraftedbeginning.api.gas.gases.GasStack;
import net.ty.createcraftedbeginning.api.gas.gases.interfaces.IGasHandler;
import net.ty.createcraftedbeginning.content.airtights.airtightengine.airtightassemblydriver.AirtightAssemblyDriverCore;
import net.ty.createcraftedbeginning.foundation.lang.CCBLang;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
final class AirtightTankDisplay {
    private final AirtightTankBlockEntity owner;

    AirtightTankDisplay(AirtightTankBlockEntity owner) {
        this.owner = owner;
    }

    boolean addToGoggleTooltip(List<Component> tooltip) {
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
            CCBLang.translate("gui.gas_container.capacity").add(GasAmountUtils.precise(capacity).style(ChatFormatting.GOLD)).style(ChatFormatting.GRAY).forGoggles(tooltip, 1);
            return true;
        }

        CCBLang.gasName(gasStack).style(ChatFormatting.GRAY).forGoggles(tooltip, 1);
        GasAmountUtils.precise(gasStack.getAmount()).style(ChatFormatting.GOLD).text(ChatFormatting.GRAY, " / ").add(GasAmountUtils.precise(capacity).style(ChatFormatting.DARK_GRAY)).forGoggles(tooltip, 1);
        return true;
    }

    int getMaxValue() {
        AirtightTankBlockEntity controller = owner.getControllerBE();
        return controller == null ? 0 : GasAmountUtils.toWholeBucketsClamped(controller.getCapability().getTankCapacity(0));
    }

    int getCurrentValue() {
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
        return GasAmountUtils.toWholeBucketsClamped(amount);
    }

    MutableComponent format(int value) {
        return GasAmountUtils.formatWholeBuckets(value);
    }
}
