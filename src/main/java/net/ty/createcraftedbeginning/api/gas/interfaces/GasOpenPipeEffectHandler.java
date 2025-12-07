package net.ty.createcraftedbeginning.api.gas.interfaces;

import com.simibubi.create.api.registry.SimpleRegistry;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.ty.createcraftedbeginning.api.gas.gases.Gas;

@FunctionalInterface
public interface GasOpenPipeEffectHandler {
    SimpleRegistry<Gas, GasOpenPipeEffectHandler> REGISTRY = SimpleRegistry.create();

    void apply(Level level, AABB area, Gas gas);
}
