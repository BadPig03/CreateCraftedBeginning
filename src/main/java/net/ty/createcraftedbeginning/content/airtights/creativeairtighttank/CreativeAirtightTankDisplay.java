package net.ty.createcraftedbeginning.content.airtights.creativeairtighttank;

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
final class CreativeAirtightTankDisplay {
    private final CreativeAirtightTankBlockEntity owner;

    CreativeAirtightTankDisplay(CreativeAirtightTankBlockEntity owner) {
        this.owner = owner;
    }

    boolean addToGoggleTooltip(List<Component> tooltip) {
        if (owner.getLevel() == null) {
            return false;
        }

        CreativeAirtightTankBlockEntity controller = owner.getControllerBE();
        if (controller == null) {
            return false;
        }

        CCBLang.translate("gui.gas_container").forGoggles(tooltip);
        GasStack gasStack = controller.getCapability().getGasInTank(0);
        if (gasStack.isEmpty()) {
            CCBLang.translate("gui.gas_container.empty").style(ChatFormatting.GRAY).forGoggles(tooltip, 1);
            return true;
        }

        CCBLang.gasName(gasStack).style(ChatFormatting.GRAY).forGoggles(tooltip, 1);
        CCBLang.translate("gui.gas_container.infinity").style(ChatFormatting.GOLD).forGoggles(tooltip, 1);
        return true;
    }

    int getMaxValue() {
        if (owner.getControllerBE() == null) {
            return 0;
        }
        return GasAmounts.toWholeBucketsClamped(CreativeAirtightTankBlockEntity.getCapacityPerTank());
    }

    int getCurrentValue() {
        CreativeAirtightTankBlockEntity controller = owner.getControllerBE();
        if (controller == null) {
            return 0;
        }

        GasStack gasStack = controller.getCapability().getGasInTank(0);
        if (gasStack.isEmpty()) {
            return 0;
        }
        return GasAmounts.toWholeBucketsClamped(CreativeAirtightTankBlockEntity.getCapacityPerTank());
    }

    MutableComponent format(int value) {
        return GasAmounts.formatWholeBuckets(value);
    }
}
