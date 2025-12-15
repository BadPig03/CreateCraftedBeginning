package net.ty.createcraftedbeginning.content.breezes.breezecooler;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.Snowball;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.HitResult.Type;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.ProjectileImpactEvent;
import net.ty.createcraftedbeginning.CreateCraftedBeginning;
import org.jetbrains.annotations.NotNull;

@EventBusSubscriber(modid = CreateCraftedBeginning.MOD_ID)
public class BreezeCoolerEvents {
    @SubscribeEvent
    public static void onSnowballImpact(@NotNull ProjectileImpactEvent event) {
        Projectile projectile = event.getProjectile();
        if (!(projectile instanceof Snowball) || event.getRayTraceResult().getType() != Type.BLOCK) {
            return;
        }

        Level level = projectile.level();
        BlockEntity blockEntity = level.getBlockEntity(BlockPos.containing(event.getRayTraceResult().getLocation()));
        if (!(blockEntity instanceof BreezeCoolerBlockEntity cooler)) {
            return;
        }

        event.setCanceled(true);
        projectile.setDeltaMovement(Vec3.ZERO);
        projectile.discard();
        boolean result = cooler.getCurrentState().onSnowballImpact(cooler);
        if (!result) {
            return;
        }

        level.playSound(null, cooler.getBlockPos(), SoundEvents.BREEZE_SHOOT, SoundSource.BLOCKS, 0.125f + level.random.nextFloat() * 0.125f, 0.75f - level.random.nextFloat() * 0.25f);
    }
}
