package net.ty.createcraftedbeginning.mixin.accessor;

import net.minecraft.world.entity.monster.piglin.AbstractPiglin;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(AbstractPiglin.class)
public interface AbstractPiglinAccessor {
    @Accessor("timeInOverworld")
    int getTimeInOverworld();

    @Accessor("timeInOverworld")
    void setTimeInOverworld(int time);
}
