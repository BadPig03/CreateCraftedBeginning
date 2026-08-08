package net.ty.createcraftedbeginning.mixin.server.accessor;

import net.minecraft.world.entity.monster.Zombie;
import net.ty.createcraftedbeginning.platform.access.ZombieAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Zombie.class)
public interface ZombieAccessor extends ZombieAccess {
    @Override
    @Invoker("convertsInWater")
    boolean ccb$convertsInWater();

    @Override
    @Invoker("startUnderWaterConversion")
    void ccb$startUnderWaterConversion(int time);
}
