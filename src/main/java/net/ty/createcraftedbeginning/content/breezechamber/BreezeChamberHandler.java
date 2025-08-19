package net.ty.createcraftedbeginning.content.breezechamber;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.Snowball;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.ProjectileImpactEvent;
import net.ty.createcraftedbeginning.content.breezechamber.BreezeChamberBlockEntity.CoolantType;

@EventBusSubscriber
public class BreezeChamberHandler {
    @SubscribeEvent
    public static void onThrowableImpact(ProjectileImpactEvent event) {
        thrownSnowballsGetEatenByChamber(event);
    }

    public static void thrownSnowballsGetEatenByChamber(ProjectileImpactEvent event) {
        Projectile projectile = event.getProjectile();
        if (!(projectile instanceof Snowball)) {
            return;
        }

        if (event.getRayTraceResult().getType() != HitResult.Type.BLOCK) {
            return;
        }

        BlockEntity blockEntity = projectile.level().getBlockEntity(BlockPos.containing(event.getRayTraceResult().getLocation()));
        if (!(blockEntity instanceof BreezeChamberBlockEntity chamber)) {
            return;
        }

        event.setCanceled(true);
        projectile.setDeltaMovement(Vec3.ZERO);
        projectile.discard();

        Level world = projectile.level();
        if (world.isClientSide) {
            return;
        }

        if (!chamber.isCreative()) {
            if (chamber.activeCoolant != CoolantType.POWERFUL) {
                chamber.activeCoolant = CoolantType.NORMAL;
                chamber.coolRemainingTime = Mth.clamp(chamber.coolRemainingTime + 10, 0, BreezeChamberBlockEntity.MAX_COOLANT_CAPACITY);
                chamber.updateBlockState();
                chamber.notifyUpdate();
            }
        }

        world.playSound(null, chamber.getBlockPos(), SoundEvents.BREEZE_SHOOT, SoundSource.BLOCKS, .125f + world.random.nextFloat() * .125f, .75f - world.random.nextFloat() * .25f);
    }
}
