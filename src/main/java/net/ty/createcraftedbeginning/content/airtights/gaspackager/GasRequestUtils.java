package net.ty.createcraftedbeginning.content.airtights.gaspackager;

import com.simibubi.create.content.logistics.BigItemStack;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.network.chat.Component;
import net.ty.createcraftedbeginning.api.gas.gases.GasAmounts;
import net.ty.createcraftedbeginning.foundation.CCBMathUtils;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class GasRequestUtils {
    private static final String INFINITY_MARK_TRANSLATION_KEY = "createcraftedbeginning.generic.infinity_mark";

    private GasRequestUtils() {
    }

    public static String format(int amount, boolean keeper) {
        if (amount >= BigItemStack.INF) {
            return keeper ? "+" : Component.translatable(INFINITY_MARK_TRANSLATION_KEY).getString();
        }
        return keeper ? GasAmounts.formatStockKeeper(amount) : GasAmounts.formatCompact(amount);
    }

    public static String formatPrecise(int amount) {
        if (amount >= BigItemStack.INF) {
            return Component.translatable(INFINITY_MARK_TRANSLATION_KEY).getString();
        }
        return GasAmounts.formatPrecise(amount);
    }

    public static int toLogisticsAmount(long gasAmount) {
        return CCBMathUtils.clampNonNegative(gasAmount, BigItemStack.INF);
    }
}
