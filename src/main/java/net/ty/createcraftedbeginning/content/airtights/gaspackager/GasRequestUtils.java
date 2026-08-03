package net.ty.createcraftedbeginning.content.airtights.gaspackager;

import com.simibubi.create.content.logistics.BigItemStack;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.network.chat.Component;
import net.ty.createcraftedbeginning.api.gas.gases.GasAmountUtils;
import net.ty.createcraftedbeginning.config.CCBConfig;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class GasRequestUtils {
    private GasRequestUtils() {
    }

    public static int getScrollStep() {
        return CCBConfig.client().gasRequestScrollStep.get();
    }

    public static int getAltStep() {
        return CCBConfig.client().gasRequestAltScrollStep.get();
    }

    public static int getCtrlStep() {
        return CCBConfig.client().gasRequestCtrlScrollStep.get();
    }

    public static int getShiftStep() {
        return CCBConfig.client().gasRequestShiftScrollStep.get();
    }

    public static String format(int amount, boolean keeper) {
        if (amount >= BigItemStack.INF) {
            return keeper ? "+" : Component.translatable("jade.gas.infinity_mark").getString();
        }
        return keeper ? GasAmountUtils.formatStockKeeper(amount) : GasAmountUtils.formatCompact(amount);
    }

    public static String formatPrecise(int amount) {
        if (amount >= BigItemStack.INF) {
            return Component.translatable("jade.gas.infinity_mark").getString();
        }
        return GasAmountUtils.formatPrecise(amount);
    }

    public static int getStep(boolean alt, boolean ctrl, boolean shift) {
        if (alt) {
            return getAltStep();
        }

        if (ctrl) {
            return getCtrlStep();
        }

        if (shift) {
            return getShiftStep();
        }
        return getScrollStep();
    }

    public static int toLogisticsAmount(long gasAmount) {
        return Math.clamp(gasAmount, 0, BigItemStack.INF);
    }
}
