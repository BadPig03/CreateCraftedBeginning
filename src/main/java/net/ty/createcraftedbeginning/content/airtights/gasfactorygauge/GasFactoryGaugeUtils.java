package net.ty.createcraftedbeginning.content.airtights.gasfactorygauge;

import com.simibubi.create.content.logistics.BigItemStack;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.util.Mth;
import net.ty.createcraftedbeginning.content.airtights.gasfilter.GasVirtualUtils;
import net.ty.createcraftedbeginning.content.airtights.gaspackager.GasRequestUtils;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class GasFactoryGaugeUtils {
    private GasFactoryGaugeUtils() {
    }

    public static void adjustAmount(BigItemStack entry, double scrollY) {
        if (scrollY == 0) {
            return;
        }

        boolean isVirtualItem = GasVirtualUtils.isVirtualItem(entry.stack);
        boolean controlDown = Screen.hasControlDown();
        boolean shiftDown = Screen.hasShiftDown();
        int currentAmount = entry.count;
        int step = isVirtualItem ? GasRequestUtils.getStep(Screen.hasAltDown(), controlDown, shiftDown) : shiftDown ? 10 : 1;
        if (!controlDown && scrollY > 0 && currentAmount == 1 && step > 1) {
            step--;
        }

        int delta = scrollY > 0 ? step : -step;
        int maxAmount = isVirtualItem ? GasFactoryGaugeBehaviour.MAX_TARGET_AMOUNT : 64;
        entry.count = Mth.clamp(currentAmount + delta, 1, maxAmount);
    }

    public static String format(BigItemStack entry, boolean prefix) {
        String amount = GasVirtualUtils.isVirtualItem(entry.stack) ? GasRequestUtils.format(entry.count, false) : Integer.toString(entry.count);
        return (prefix ? "x" : "") + amount;
    }

    public static String formatPrecise(BigItemStack entry) {
        String amount = GasVirtualUtils.isVirtualItem(entry.stack) ? GasRequestUtils.formatPrecise(entry.count) : Integer.toString(entry.count);
        return 'x' + amount;
    }
}
