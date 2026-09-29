package net.ty.createcraftedbeginning.mixin.client.minecraft;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.HitResult;
import net.ty.createcraftedbeginning.content.airtights.airtighthandhelddrill.upgrades.LiquidReplacementUpgrade;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {
    @WrapOperation(method = "pick(Lnet/minecraft/world/entity/Entity;DDF)Lnet/minecraft/world/phys/HitResult;", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;pick(DFZ)Lnet/minecraft/world/phys/HitResult;"))
    private HitResult ccb$pickDrillLiquid(Entity entity, double distance, float partialTicks, boolean hitFluids, Operation<HitResult> original) {
        boolean targetsLiquid = entity instanceof Player player && !player.isSpectator() && LiquidReplacementUpgrade.INSTANCE.canApply(player.getMainHandItem());
        return original.call(entity, distance, partialTicks, hitFluids || targetsLiquid);
    }
}
