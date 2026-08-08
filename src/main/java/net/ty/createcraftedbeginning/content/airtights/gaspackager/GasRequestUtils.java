package net.ty.createcraftedbeginning.content.airtights.gaspackager;

import com.simibubi.create.content.logistics.BigItemStack;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.network.chat.Component;
import net.ty.createcraftedbeginning.api.gas.gases.GasAmountUtils;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class GasRequestUtils {
    private GasRequestUtils() {
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

    public static int toLogisticsAmount(long gasAmount) {
        return Math.clamp(gasAmount, 0, BigItemStack.INF);
    }
}
