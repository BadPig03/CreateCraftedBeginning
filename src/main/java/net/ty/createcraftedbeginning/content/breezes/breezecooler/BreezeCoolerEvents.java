package net.ty.createcraftedbeginning.content.breezes.breezecooler;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.Snowball;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.ProjectileImpactEvent;
import net.ty.createcraftedbeginning.api.CCBAPI;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@EventBusSubscriber(modid = CCBAPI.MOD_ID)
public class BreezeCoolerEvents {
    @SubscribeEvent
    public static void onSnowballImpact(ProjectileImpactEvent event) {
        Projectile projectile = event.getProjectile();
        if (!(projectile instanceof Snowball) || !(event.getRayTraceResult() instanceof BlockHitResult hitResult)) {
            return;
        }

        Level level = projectile.level();
        if (!(level.getBlockEntity(hitResult.getBlockPos()) instanceof BreezeCoolerBlockEntity cooler) || level.isClientSide) {
            return;
        }

        if (!cooler.getCurrentState().onSnowballImpact(cooler)) {
            return;
        }

        event.setCanceled(true);
        projectile.discard();
    }
}
