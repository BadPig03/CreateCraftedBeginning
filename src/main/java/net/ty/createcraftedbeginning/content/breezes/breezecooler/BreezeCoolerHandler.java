package net.ty.createcraftedbeginning.content.breezes.breezecooler;

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
import net.ty.createcraftedbeginning.content.breezes.breezecooler.BreezeCoolerBlockEntity.CoolantType;
import org.jetbrains.annotations.NotNull;

@EventBusSubscriber
public class BreezeCoolerHandler {
    @SubscribeEvent
    public static void onThrowableImpact(ProjectileImpactEvent event) {
        thrownSnowballsGetEatenByCooler(event);
    }

    public static void thrownSnowballsGetEatenByCooler(@NotNull ProjectileImpactEvent event) {
        Projectile projectile = event.getProjectile();
        if (!(projectile instanceof Snowball)) {
            return;
        }

        if (event.getRayTraceResult().getType() != HitResult.Type.BLOCK) {
            return;
        }

        BlockEntity blockEntity = projectile.level().getBlockEntity(BlockPos.containing(event.getRayTraceResult().getLocation()));
        if (!(blockEntity instanceof BreezeCoolerBlockEntity cooler)) {
            return;
        }

        event.setCanceled(true);
        projectile.setDeltaMovement(Vec3.ZERO);
        projectile.discard();

        Level level = projectile.level();
        if (level.isClientSide) {
            return;
        }

        if (cooler.activeCoolant == CoolantType.NONE) {
            cooler.activeCoolant = CoolantType.NORMAL;
            cooler.coolRemainingTime = Mth.clamp(cooler.coolRemainingTime + 10, 0, BreezeCoolerBlockEntity.MAX_COOLANT_CAPACITY);
            cooler.updateBlockState();
            cooler.notifyUpdate();
        }

        level.playSound(null, cooler.getBlockPos(), SoundEvents.BREEZE_SHOOT, SoundSource.BLOCKS, 0.125f + level.random.nextFloat() * .125f, .75f - level.random.nextFloat() * .25f);
    }
}
