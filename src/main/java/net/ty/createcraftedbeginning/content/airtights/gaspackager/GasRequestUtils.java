package net.ty.createcraftedbeginning.content.airtights.gaspackager;

import com.simibubi.create.content.logistics.BigItemStack;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.ty.createcraftedbeginning.config.CCBConfig;
import net.ty.createcraftedbeginning.data.CCBLang;

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
        String bucket = keeper ? "b" : "B";
        if (amount >= BigItemStack.INF) {
            return keeper ? "+" : CCBLang.translateDirect("jade.gas.infinity_mark").getString();
        }
        else if (amount >= 1000000) {
            if (amount % 1000000 == 0) {
                return amount / 1000000 + "k" + bucket;
            }
            return String.format("%.1f", Math.floor(amount / 100000.0f) / 10.0f) + 'k' + bucket;
        }
        else if (amount >= 1000) {
            if (amount % 1000 == 0) {
                return amount / 1000 + bucket;
            }
            return String.format("%.1f", Math.floor(amount / 100.0f) / 10.0f) + bucket;
        }
        else if (amount >= 100) {
            return "0." + amount / 100 + bucket;
        }
        return amount + "m" + bucket;
    }

    public static String formatPrecise(int amount) {
        if (amount >= BigItemStack.INF) {
            return "∞";
        }
        return amount + "mB";
    }

    public static int getStep(boolean alt, boolean ctrl, boolean shift) {
        if (alt) {
            return getAltStep();
        }
        else if (ctrl) {
            return getCtrlStep();
        }
        else if (shift) {
            return getShiftStep();
        }
        return getScrollStep();
    }

    public static int toLogisticsAmount(long gasAmount) {
        if (gasAmount <= 0) {
            return 0;
        }
        if (gasAmount >= BigItemStack.INF) {
            return BigItemStack.INF;
        }
        return (int) gasAmount;
    }
}
