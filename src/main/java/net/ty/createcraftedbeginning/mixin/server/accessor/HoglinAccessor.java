package net.ty.createcraftedbeginning.mixin.server.accessor;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.entity.monster.hoglin.Hoglin;
import net.ty.createcraftedbeginning.platform.access.HoglinAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@Mixin(Hoglin.class)
public interface HoglinAccessor extends HoglinAccess {
    @Override
    @Accessor("timeInOverworld")
    int ccb$getTimeInOverworld();

    @Override
    @Accessor("timeInOverworld")
    void ccb$setTimeInOverworld(int time);
}
