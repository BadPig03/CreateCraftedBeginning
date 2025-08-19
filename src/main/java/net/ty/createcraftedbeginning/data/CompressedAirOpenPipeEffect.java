package net.ty.createcraftedbeginning.data;

import com.simibubi.create.api.effect.OpenPipeEffectHandler;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.fluids.FluidStack;

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
    }
}
