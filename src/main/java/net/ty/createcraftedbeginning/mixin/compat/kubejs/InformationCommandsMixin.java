package net.ty.createcraftedbeginning.mixin.compat.kubejs;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.ty.createcraftedbeginning.compat.kubejs.KubeJSHandGasOutput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@Pseudo
@Mixin(targets = "dev.latvian.mods.kubejs.command.InformationCommands", remap = false)
public abstract class InformationCommandsMixin {
    @Inject(method = "hand", at = @At("RETURN"))
    private static void ccb$hand(ServerPlayer player, InteractionHand hand, CallbackInfoReturnable<Integer> callback) {
        KubeJSHandGasOutput.append(player, player.getItemInHand(hand));
    }
}
