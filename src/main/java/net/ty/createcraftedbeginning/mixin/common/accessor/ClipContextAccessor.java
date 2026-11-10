package net.ty.createcraftedbeginning.mixin.common.accessor;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.shapes.CollisionContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import javax.annotation.ParametersAreNonnullByDefault;

@FunctionalInterface
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@Mixin(ClipContext.class)
public interface ClipContextAccessor {
    @Accessor("collisionContext")
    CollisionContext ccb$getCollisionContext();
}
