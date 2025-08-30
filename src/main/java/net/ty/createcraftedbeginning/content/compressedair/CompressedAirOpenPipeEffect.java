package net.ty.createcraftedbeginning.content.compressedair;

import com.simibubi.create.api.effect.OpenPipeEffectHandler;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.fluids.FluidStack;
import net.ty.createcraftedbeginning.registry.CCBParticleTypes;

import java.util.List;

public class CompressedAirOpenPipeEffect implements OpenPipeEffectHandler {
    @Override
    public void apply(Level level, AABB area, FluidStack fluid) {
        List<Entity> entities = level.getEntities((Entity) null, area, entity -> entity instanceof ItemEntity || entity instanceof LivingEntity);
        for (Entity entity : entities) {
            if (entity.canFreeze()) {
                entity.setTicksFrozen(Math.min(entity.getTicksRequiredToFreeze(), entity.getTicksFrozen()) + entity.getTicksRequiredToFreeze() / 20);
            }
            entity.extinguishFire();
        }

        spawnCloudParticles(level, area);
    }

    private void spawnCloudParticles(Level level, AABB area) {
        Vec3 center = area.getCenter();
        ((ServerLevel) level).getPlayers(player -> true).forEach(player -> player.connection.send(new ClientboundLevelParticlesPacket((SimpleParticleType) CCBParticleTypes.COMPRESSED_AIR_LEAK.get(), false, center.x, center.y, center.z, (float) area.getXsize() / 2f, (float) area.getYsize() / 2f, (float) area.getZsize() / 2f, 0, 1)));
    }
}
