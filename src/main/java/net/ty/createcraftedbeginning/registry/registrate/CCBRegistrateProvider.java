package net.ty.createcraftedbeginning.registry.registrate;

import com.simibubi.create.foundation.item.ItemDescription.Modifier;
import com.simibubi.create.foundation.item.KineticStats;
import com.simibubi.create.foundation.item.TooltipModifier;
import net.createmod.catnip.lang.FontHelper.Palette;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.ty.createcraftedbeginning.api.CCBAPI;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class CCBRegistrateProvider {
    private static final CCBRegistrate INSTANCE = CCBRegistrate.create(CCBAPI.MOD_ID).disableDefaultCreativeTab().setTooltipModifierFactory(item -> new Modifier(item, Palette.STANDARD_CREATE).andThen(TooltipModifier.mapNull(KineticStats.create(item))));

    private CCBRegistrateProvider() {
    }

    public static CCBRegistrate get() {
        return INSTANCE;
    }
}
