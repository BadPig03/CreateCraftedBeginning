package net.ty.createcraftedbeginning.mixin.accessor;

import net.minecraft.world.entity.monster.hoglin.Hoglin;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Hoglin.class)
public interface HoglinAccessor {
    @Accessor("timeInOverworld")
    int getTimeInOverworld();

    @Accessor("timeInOverworld")
    void setTimeInOverworld(int time);
}
