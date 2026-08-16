package net.ty.createcraftedbeginning.content.airtights.creativeairtighttank;

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
public final class CreativeAirtightTankDisplay {
    private final CreativeAirtightTankBlockEntity owner;

    public CreativeAirtightTankDisplay(CreativeAirtightTankBlockEntity owner) {
        this.owner = owner;
    }

    public boolean addToGoggleTooltip(List<Component> tooltip) {
        if (owner.getLevel() == null) {
            return false;
        }

        CreativeAirtightTankBlockEntity controller = owner.getControllerBE();
        if (controller == null) {
            return false;
        }

        IGasHandler handler = controller.getCapability();
        CCBLang.translate("gui.gas_container").forGoggles(tooltip);
        GasStack gas = handler.getGasInTank(0);
        if (gas.isEmpty()) {
            CCBLang.translate("gui.gas_container.empty").style(ChatFormatting.GRAY).forGoggles(tooltip, 1);
            return true;
        }

        CCBLang.gasName(gas).style(ChatFormatting.GRAY).forGoggles(tooltip, 1);
        CCBLang.translate("gui.gas_container.infinity").style(ChatFormatting.GOLD).forGoggles(tooltip, 1);
        return true;
    }

    public int getMaxValue() {
        return owner.getControllerBE() == null ? 0 : GasAmounts.toWholeBucketsClamped(CreativeAirtightTankBlockEntity.getCapacityPerTank());
    }

    public int getCurrentValue() {
        CreativeAirtightTankBlockEntity controller = owner.getControllerBE();
        if (controller == null) {
            return 0;
        }

        GasStack gas = controller.getCapability().getGasInTank(0);
        return gas.isEmpty() ? 0 : GasAmounts.toWholeBucketsClamped(CreativeAirtightTankBlockEntity.getCapacityPerTank());
    }

    public MutableComponent format(int value) {
        return GasAmounts.formatWholeBuckets(value);
    }
}
