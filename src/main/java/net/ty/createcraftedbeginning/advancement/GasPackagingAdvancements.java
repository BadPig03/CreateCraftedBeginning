package net.ty.createcraftedbeginning.advancement;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.ty.createcraftedbeginning.registry.CCBAdvancements;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class GasPackagingAdvancements {
    private static final ThreadLocal<CCBAdvancementBehaviour> ACTIVE_GAUGE = new ThreadLocal<>();

    private GasPackagingAdvancements() {
    }

    public static void withGauge(@Nullable CCBAdvancementBehaviour gauge, Runnable action) {
        CCBAdvancementBehaviour previous = ACTIVE_GAUGE.get();
        ACTIVE_GAUGE.set(gauge);
        try {
            action.run();
        }
        finally {
            if (previous == null) {
                ACTIVE_GAUGE.remove();
            }
            else {
                ACTIVE_GAUGE.set(previous);
            }
        }
    }

    public static void onBalloonCreated() {
        CCBAdvancementBehaviour gauge = ACTIVE_GAUGE.get();
        if (gauge == null) {
            return;
        }

        gauge.awardPlayer(CCBAdvancements.SMART_GAS_COLLECTION);
    }
}
