package net.ty.createcraftedbeginning.mixin.server.accessor;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.entity.monster.piglin.AbstractPiglin;
import net.ty.createcraftedbeginning.platform.access.OverworldConversionAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@Mixin(AbstractPiglin.class)
public interface AbstractPiglinAccessor extends OverworldConversionAccess {
    @Override
    @Accessor("timeInOverworld")
    int ccb$getTimeInOverworld();

    @Override
    @Accessor("timeInOverworld")
    void ccb$setTimeInOverworld(int time);
}
