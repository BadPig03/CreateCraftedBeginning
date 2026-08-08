package net.ty.createcraftedbeginning.registry.registrate;

import com.simibubi.create.foundation.item.ItemDescription.Modifier;
import com.simibubi.create.foundation.item.KineticStats;
import com.simibubi.create.foundation.item.TooltipModifier;
import net.createmod.catnip.lang.FontHelper.Palette;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.ty.createcraftedbeginning.api.CCBAPI;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class CCBRegistrateProvider {
    @SuppressWarnings("DataFlowIssue")
    private static final CCBRegistrate INSTANCE = CCBRegistrate.create(CCBAPI.MOD_ID).defaultCreativeTab((ResourceKey<CreativeModeTab>) null).setTooltipModifierFactory(item -> new Modifier(item, Palette.STANDARD_CREATE).andThen(TooltipModifier.mapNull(KineticStats.create(item))));

    private CCBRegistrateProvider() {
    }

    public static CCBRegistrate get() {
        return INSTANCE;
    }
}
