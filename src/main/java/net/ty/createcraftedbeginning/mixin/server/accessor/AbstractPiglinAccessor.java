package net.ty.createcraftedbeginning.mixin.server.accessor;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.entity.monster.piglin.AbstractPiglin;
import net.ty.createcraftedbeginning.platform.access.AbstractPiglinAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@Mixin(AbstractPiglin.class)
public interface AbstractPiglinAccessor extends AbstractPiglinAccess {
    @Override
    @Accessor("timeInOverworld")
    int getTimeInOverworld();

    @Override
    @Accessor("timeInOverworld")
    void setTimeInOverworld(int time);
}
