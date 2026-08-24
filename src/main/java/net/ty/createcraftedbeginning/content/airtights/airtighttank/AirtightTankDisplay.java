package net.ty.createcraftedbeginning.content.airtights.airtighttank;

import net.minecraft.ChatFormatting;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.ty.createcraftedbeginning.api.gas.gases.GasAmounts;
import net.ty.createcraftedbeginning.api.gas.gases.GasStack;
import net.ty.createcraftedbeginning.api.gas.gases.interfaces.IGasHandler;
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

        if (controller.getCore().addToGoggleTooltip(tooltip)) {
            tooltip.add(Component.empty());
        }

        IGasHandler gasHandler = controller.getTankInventory();
        CCBLang.translate("gui.gas_container").forGoggles(tooltip);
        GasStack gasStack = gasHandler.getGasInTank(0);
        long gasCapacity = gasHandler.getTankCapacity(0);
        if (gasStack.isEmpty()) {
            CCBLang.translate("gui.gas_container.capacity").add(GasAmounts.precise(gasCapacity).style(ChatFormatting.GOLD)).style(ChatFormatting.GRAY).forGoggles(tooltip, 1);
            return true;
        }

        CCBLang.gasName(gasStack).style(ChatFormatting.GRAY).forGoggles(tooltip, 1);
        GasAmounts.precise(gasStack.getAmount()).style(ChatFormatting.GOLD).text(ChatFormatting.GRAY, " / ").add(GasAmounts.precise(gasCapacity).style(ChatFormatting.DARK_GRAY)).forGoggles(tooltip, 1);
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

        IGasHandler gasHandler = controller.getCapability();
        long totalAmount = 0;
        for (int tank = 0; tank < gasHandler.getTanks(); tank++) {
            GasStack gasStack = gasHandler.getGasInTank(tank);
            if (!gasStack.isEmpty()) {
                totalAmount += gasStack.getAmount();
            }
        }
        return GasAmounts.toWholeBucketsClamped(totalAmount);
    }

    public MutableComponent format(int value) {
        return GasAmounts.formatWholeBuckets(value);
    }
}
