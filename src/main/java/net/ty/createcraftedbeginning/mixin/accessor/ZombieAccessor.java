package net.ty.createcraftedbeginning.mixin.accessor;

import net.minecraft.world.entity.monster.Zombie;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Zombie.class)
public interface ZombieAccessor {
    @Invoker("convertsInWater")
    boolean ccb$convertsInWater();

    @Invoker("startUnderWaterConversion")
    void ccb$startUnderWaterConversion(int time);
}
