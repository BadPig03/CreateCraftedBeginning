package net.ty.createcraftedbeginning.api.gas.gases;

import com.simibubi.create.api.registry.SimpleRegistry;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

@FunctionalInterface
public interface GasOpenPipeEffectHandler {
    SimpleRegistry<Gas, GasOpenPipeEffectHandler> REGISTRY = SimpleRegistry.create();

    void apply(Level level, AABB area, Gas gas);
}
