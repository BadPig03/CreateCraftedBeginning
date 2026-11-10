package net.ty.createcraftedbeginning.mixin.common.neoforge;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.CommonHooks;
import net.neoforged.neoforge.common.util.BlockSnapshot;
import net.ty.createcraftedbeginning.content.airtights.airtightfractionationtower.AirtightFractionationTowerInstrumentPanelItem;
import org.spongepowered.asm.mixin.Mixin;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@Mixin(value = CommonHooks.class, remap = false)
public abstract class CommonHooksMixin {
    @WrapMethod(method = "onPlaceItemIntoWorld")
    private static InteractionResult ccb$onPlaceItemIntoWorld(UseOnContext context, Operation<InteractionResult> original) {
        if (!(context.getItemInHand().getItem() instanceof AirtightFractionationTowerInstrumentPanelItem)) {
            return original.call(context);
        }

        Level level = context.getLevel();
        boolean capturing = level.captureBlockSnapshots;
        List<BlockSnapshot> outerSnapshots = List.copyOf(level.capturedBlockSnapshots);
        try {
            return original.call(context);
        }
        catch (RuntimeException | Error failure) {
            level.captureBlockSnapshots = capturing;
            level.capturedBlockSnapshots.clear();
            level.capturedBlockSnapshots.addAll(outerSnapshots);
            throw failure;
        }
    }
}
