package net.ty.createcraftedbeginning.gas.visual;

import net.createmod.catnip.lang.LangBuilder;
import net.createmod.catnip.lang.LangNumberFormat;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.network.chat.MutableComponent;
import net.ty.createcraftedbeginning.api.gas.GasUnits;
import net.ty.createcraftedbeginning.foundation.lang.CCBLang;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Locale;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class GasUnitFormat {
    private GasUnitFormat() {
    }

    public static LangBuilder amount(long gasUnits) {
        return CCBLang.text(LangNumberFormat.format(gasUnits)).space().add(CCBLang.translate("gui.unit.gas_units"));
    }

    public static String format(long gasUnits) {
        return amount(gasUnits).component().getString();
    }

    public static String formatCompact(long gasUnits) {
        if (gasUnits < GasUnits.GU_PER_KGU) {
            return gasUnits + " GU";
        }

        if (gasUnits < GasUnits.GU_PER_MGU) {
            return formatTenths(gasUnits, GasUnits.GU_PER_KGU) + " kGU";
        }

        if (gasUnits < GasUnits.GU_PER_GGU) {
            return formatTenths(gasUnits, GasUnits.GU_PER_MGU) + " MGU";
        }

        return formatTenths(gasUnits, GasUnits.GU_PER_GGU) + " GGU";
    }

    public static String formatCompactTight(long gasUnits) {
        if (gasUnits >= GasUnits.GU_PER_GGU / 10) {
            return formatTenths(gasUnits, GasUnits.GU_PER_GGU) + "GGU";
        }

        if (gasUnits >= GasUnits.GU_PER_MGU / 10) {
            return formatTenths(gasUnits, GasUnits.GU_PER_MGU) + "MGU";
        }

        if (gasUnits >= GasUnits.GU_PER_KGU / 10) {
            return formatTenths(gasUnits, GasUnits.GU_PER_KGU) + "kGU";
        }

        return gasUnits + "GU";
    }

    public static String formatPrecise(long gasUnits) {
        if (gasUnits >= GasUnits.GU_PER_GGU && gasUnits % (GasUnits.GU_PER_GGU / 10) == 0) {
            return formatTenths(gasUnits, GasUnits.GU_PER_GGU) + " GGU";
        }

        if (gasUnits >= GasUnits.GU_PER_MGU && gasUnits % (GasUnits.GU_PER_MGU / 10) == 0) {
            return formatTenths(gasUnits, GasUnits.GU_PER_MGU) + " MGU";
        }

        if (gasUnits >= GasUnits.GU_PER_KGU && gasUnits % (GasUnits.GU_PER_KGU / 10) == 0) {
            return formatTenths(gasUnits, GasUnits.GU_PER_KGU) + " kGU";
        }

        return format(gasUnits);
    }

    public static MutableComponent formatKilo(long kiloGasUnits) {
        return CCBLang.text(LangNumberFormat.format(kiloGasUnits)).space().add(CCBLang.translate("gui.unit.kilo_gas_units")).component();
    }

    public static String formatRate(long gasUnitsPerTick) {
        if (gasUnitsPerTick < GasUnits.GU_PER_KGU) {
            return gasUnitsPerTick + " GU/t";
        }

        if (gasUnitsPerTick < GasUnits.GU_PER_MGU) {
            return formatHundredths(gasUnitsPerTick, GasUnits.GU_PER_KGU, "kGU/t");
        }

        if (gasUnitsPerTick < GasUnits.GU_PER_GGU) {
            return formatHundredths(gasUnitsPerTick, GasUnits.GU_PER_MGU, "MGU/t");
        }

        return formatHundredths(gasUnitsPerTick, GasUnits.GU_PER_GGU, "GGU/t");
    }

    private static String formatTenths(long gasAmount, long unit) {
        long tenths = gasAmount / (unit / 10);
        long whole = tenths / 10;
        long fraction = tenths % 10;
        if (fraction != 0) {
            return String.valueOf(whole) + '.' + fraction;
        }

        return Long.toString(whole);
    }

    private static String formatHundredths(long gasAmount, long unit, String suffix) {
        return String.format(Locale.ROOT, "%.2f %s", (double) gasAmount / unit, suffix);
    }
}
