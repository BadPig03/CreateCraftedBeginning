package net.ty.createcraftedbeginning.content.airtights.gaspackager;

import com.simibubi.create.content.logistics.BigItemStack;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.network.chat.Component;
import net.ty.createcraftedbeginning.api.gas.GasUnits;
import net.ty.createcraftedbeginning.gas.visual.GasUnitFormat;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class GasRequestFormat {
    private GasRequestFormat() {
    }

    public static String format(int amount, boolean compact) {
        if (amount >= BigItemStack.INF) {
            if (compact) {
                return "+";
            }

            return Component.translatable("createcraftedbeginning.generic.infinity_mark").getString();
        }

        if (compact) {
            return GasUnitFormat.formatCompactTight(amount);
        }

        return GasUnitFormat.formatCompact(amount);
    }

    public static String formatPrecise(int amount) {
        if (amount >= BigItemStack.INF) {
            return Component.translatable("createcraftedbeginning.generic.infinity_mark").getString();
        }

        return GasUnitFormat.format(amount);
    }

    public static String formatDecoration(int amount, boolean compact) {
        if (amount >= BigItemStack.INF) {
            if (compact) {
                return "+";
            }

            return Component.translatable("createcraftedbeginning.generic.infinity_mark").getString();
        }

        if (amount < GasUnits.GU_PER_KGU) {
            return Integer.toString(amount);
        }

        if (amount < GasUnits.GU_PER_MGU) {
            return formatDecorationUnit(amount, GasUnits.GU_PER_KGU, "k");
        }

        return formatDecorationUnit(amount, GasUnits.GU_PER_MGU, "m");
    }

    private static String formatDecorationUnit(long amount, long unit, String suffix) {
        long whole = amount / unit;
        if (whole >= 10) {
            return whole + suffix;
        }

        long tenths = amount / (unit / 10);
        long fraction = tenths % 10;
        if (fraction == 0) {
            return whole + suffix;
        }

        return String.valueOf(whole) + '.' + fraction + suffix;
    }
}
