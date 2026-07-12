package net.ty.createcraftedbeginning.mixin.server.create;

import com.simibubi.create.content.kinetics.deployer.ManualApplicationRecipe;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.ty.createcraftedbeginning.content.end.endcasing.EndCasingBlock;
import net.ty.createcraftedbeginning.registry.CCBAdvancements;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@Mixin(value = ManualApplicationRecipe.class, remap = false)
public abstract class ManualApplicationRecipeMixin {
    @Inject(method = "awardAdvancements", at = @At("HEAD"))
    private static void ccb$awardAdvancements(Player player, BlockState placed, CallbackInfo ci) {
        if (!(placed.getBlock() instanceof EndCasingBlock)) {
            return;
        }

        CCBAdvancements.THE_INTEGRATED_INDUSTRY_AGE.awardTo(player);
    }
}
