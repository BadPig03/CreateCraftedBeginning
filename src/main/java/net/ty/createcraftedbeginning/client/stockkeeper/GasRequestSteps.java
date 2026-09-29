package net.ty.createcraftedbeginning.client.stockkeeper;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.ty.createcraftedbeginning.config.CCBConfig;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@OnlyIn(Dist.CLIENT)
public final class GasRequestSteps {
    private GasRequestSteps() {
    }

    public static int getScrollStep() {
        return CCBConfig.client().gasRequests.scrollStep.get();
    }

    public static int getAltStep() {
        return CCBConfig.client().gasRequests.altScrollStep.get();
    }

    public static int getCtrlStep() {
        return CCBConfig.client().gasRequests.ctrlScrollStep.get();
    }

    public static int getShiftStep() {
        return CCBConfig.client().gasRequests.shiftScrollStep.get();
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

}
